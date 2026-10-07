package com.techstore.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.inventario.autenticacion.Totp;
import com.techstore.inventario.usuarios.Rol;
import com.techstore.inventario.usuarios.Usuario;
import com.techstore.inventario.usuarios.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class MfaTest extends PruebaIntegracion {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired FabricaUsuarios fabrica;
    @Autowired RelojDePrueba reloj;
    @Autowired JdbcTemplate jdbc;

    ApiAuth auth;

    @BeforeEach
    void preparar() {
        auth = new ApiAuth(mockMvc, json, reloj);
    }

    @AfterEach
    void restablecerReloj() {
        reloj.restablecer();
    }

    private Usuario usuario() {
        return fabrica.crear(Rol.EMPLEADO_VENTAS, fabrica.tienda("LIM-01"));
    }

    private String codigoIncorrecto(String secreto) {
        String correcto = auth.codigoActual(secreto);
        return correcto.equals("000000") ? "111111" : "000000";
    }

    @Test
    void elPrimerInicioEnrolaLaAppAutenticadoraYEntregaElTokenCompleto() throws Exception {
        Usuario usuario = usuario();
        String mfaToken = auth.mfaToken(usuario.getEmail(), FabricaUsuarios.PASSWORD);

        String secreto = auth.leer(auth.enrolar(mfaToken)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.otpauthUri").value(org.hamcrest.Matchers.startsWith("otpauth://totp/TechStore:")))
            .andReturn().getResponse().getContentAsString()).get("secreto").asText();

        String respuesta = auth.verificar(mfaToken, auth.codigoActual(secreto))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tipo").value("Bearer"))
            .andExpect(jsonPath("$.usuario.email").value(usuario.getEmail()))
            .andExpect(jsonPath("$.usuario.mfaHabilitado").value(true))
            .andReturn().getResponse().getContentAsString();

        auth.me(auth.leer(respuesta).get("token").asText())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(usuario.getEmail()));
    }

    @Test
    void elSecretoQuedaCifradoEnLaBaseDeDatos() throws Exception {
        Usuario usuario = usuario();
        ApiAuth.Sesion sesion = auth.iniciarSesionPorPrimeraVez(usuario.getEmail(), FabricaUsuarios.PASSWORD);

        String almacenado = jdbc.queryForObject("SELECT mfa_secreto FROM usuario WHERE id = ?", String.class,
            usuario.getId());

        assertThat(almacenado).startsWith("v1:").doesNotContain(sesion.secreto());
    }

    @Test
    void conMfaYaActivoElLoginPideElCodigoYNoPermiteReenrolar() throws Exception {
        Usuario usuario = usuario();
        ApiAuth.Sesion sesion = auth.iniciarSesionPorPrimeraVez(usuario.getEmail(), FabricaUsuarios.PASSWORD);
        reloj.avanzar(Duration.ofSeconds(Totp.PERIODO_SEGUNDOS));

        auth.login(usuario.getEmail(), FabricaUsuarios.PASSWORD)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("MFA_REQUERIDO"));
        String mfaToken = auth.mfaToken(usuario.getEmail(), FabricaUsuarios.PASSWORD);

        auth.enrolar(mfaToken).andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("MFA_YA_CONFIGURADO"));
        auth.verificar(mfaToken, auth.codigoActual(sesion.secreto())).andExpect(status().isOk());
    }

    @Test
    void unCodigoYaUsadoNoSirveDosVeces() throws Exception {
        Usuario usuario = usuario();
        ApiAuth.Sesion sesion = auth.iniciarSesionPorPrimeraVez(usuario.getEmail(), FabricaUsuarios.PASSWORD);

        String mfaToken = auth.mfaToken(usuario.getEmail(), FabricaUsuarios.PASSWORD);
        auth.verificar(mfaToken, auth.codigoActual(sesion.secreto()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("MFA_CODIGO_INVALIDO"));

        reloj.avanzar(Duration.ofSeconds(Totp.PERIODO_SEGUNDOS));
        auth.verificar(mfaToken, auth.codigoActual(sesion.secreto())).andExpect(status().isOk());
    }

    @Test
    void elTercerCodigoIncorrectoAgotaElDesafioAunqueElCuartoSeaCorrecto() throws Exception {
        Usuario usuario = usuario();
        String mfaToken = auth.mfaToken(usuario.getEmail(), FabricaUsuarios.PASSWORD);
        String secreto = auth.leer(auth.enrolar(mfaToken).andReturn().getResponse().getContentAsString())
            .get("secreto").asText();
        String malo = codigoIncorrecto(secreto);

        auth.verificar(mfaToken, malo).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("MFA_CODIGO_INVALIDO"))
            .andExpect(jsonPath("$.intentosRestantes").value(2));
        auth.verificar(mfaToken, malo).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.intentosRestantes").value(1));
        auth.verificar(mfaToken, malo).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("MFA_INTENTOS_AGOTADOS"));

        auth.verificar(mfaToken, auth.codigoActual(secreto)).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("MFA_DESAFIO_INVALIDO"));
        assertThat(usuarios.findByEmail(usuario.getEmail()).orElseThrow().isMfaHabilitado()).isFalse();
    }

    @Test
    void trasAgotarElDesafioUnNuevoLoginDaTresIntentosNuevos() throws Exception {
        Usuario usuario = usuario();
        String primero = auth.mfaToken(usuario.getEmail(), FabricaUsuarios.PASSWORD);
        String secreto = auth.leer(auth.enrolar(primero).andReturn().getResponse().getContentAsString())
            .get("secreto").asText();
        for (int i = 0; i < 3; i++) {
            auth.verificar(primero, codigoIncorrecto(secreto));
        }

        String segundo = auth.mfaToken(usuario.getEmail(), FabricaUsuarios.PASSWORD);

        auth.verificar(segundo, auth.codigoActual(secreto)).andExpect(status().isOk());
    }

    @Test
    void elDesafioExpiraALosCincoMinutos() throws Exception {
        Usuario usuario = usuario();
        String mfaToken = auth.mfaToken(usuario.getEmail(), FabricaUsuarios.PASSWORD);
        String secreto = auth.leer(auth.enrolar(mfaToken).andReturn().getResponse().getContentAsString())
            .get("secreto").asText();

        reloj.avanzar(Duration.ofMinutes(5).plusSeconds(1));

        auth.verificar(mfaToken, auth.codigoActual(secreto)).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("TOKEN_EXPIRADO"));
    }

    @Test
    void elTokenMfaSoloSirveUnaVez() throws Exception {
        Usuario usuario = usuario();
        String mfaToken = auth.mfaToken(usuario.getEmail(), FabricaUsuarios.PASSWORD);
        String secreto = auth.leer(auth.enrolar(mfaToken).andReturn().getResponse().getContentAsString())
            .get("secreto").asText();
        auth.verificar(mfaToken, auth.codigoActual(secreto)).andExpect(status().isOk());

        reloj.avanzar(Duration.ofSeconds(Totp.PERIODO_SEGUNDOS));

        auth.verificar(mfaToken, auth.codigoActual(secreto)).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("MFA_DESAFIO_INVALIDO"));
    }

    @Test
    void unNuevoLoginInvalidaElDesafioAnterior() throws Exception {
        Usuario usuario = usuario();
        String viejo = auth.mfaToken(usuario.getEmail(), FabricaUsuarios.PASSWORD);
        String secreto = auth.leer(auth.enrolar(viejo).andReturn().getResponse().getContentAsString())
            .get("secreto").asText();

        auth.mfaToken(usuario.getEmail(), FabricaUsuarios.PASSWORD);

        auth.verificar(viejo, auth.codigoActual(secreto)).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("MFA_DESAFIO_INVALIDO"));
    }

    @Test
    void noSePuedeVerificarSinHaberRegistradoLaAppAutenticadora() throws Exception {
        Usuario usuario = usuario();
        String mfaToken = auth.mfaToken(usuario.getEmail(), FabricaUsuarios.PASSWORD);

        auth.verificar(mfaToken, "123456").andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("MFA_NO_ENROLADO"));
    }

    @Test
    void uncodigoConFormatoInvalidoCuentaComoIntentoFallido() throws Exception {
        Usuario usuario = usuario();
        String mfaToken = auth.mfaToken(usuario.getEmail(), FabricaUsuarios.PASSWORD);
        auth.enrolar(mfaToken);

        auth.verificar(mfaToken, "abc").andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.intentosRestantes").value(2));
    }

    @Test
    void cincoDesafiosAgotadosBloqueanLaCuentaAunqueLaContrasenaSeaCorrecta() throws Exception {
        Usuario usuario = usuario();
        ApiAuth.Sesion sesion = auth.iniciarSesionPorPrimeraVez(usuario.getEmail(), FabricaUsuarios.PASSWORD);
        String malo = codigoIncorrecto(sesion.secreto());

        for (int ronda = 1; ronda <= 4; ronda++) {
            String mfaToken = auth.mfaToken(usuario.getEmail(), FabricaUsuarios.PASSWORD);
            for (int i = 0; i < 3; i++) {
                auth.verificar(mfaToken, malo);
            }
        }
        String quinto = auth.mfaToken(usuario.getEmail(), FabricaUsuarios.PASSWORD);
        auth.verificar(quinto, malo);
        auth.verificar(quinto, malo);
        auth.verificar(quinto, malo).andExpect(status().isLocked())
            .andExpect(jsonPath("$.codigo").value("CUENTA_BLOQUEADA"));

        auth.login(usuario.getEmail(), FabricaUsuarios.PASSWORD).andExpect(status().isLocked());
    }

    @Test
    void cerrarSesionRevocaElTokenDeAcceso() throws Exception {
        Usuario usuario = usuario();
        ApiAuth.Sesion sesion = auth.iniciarSesionPorPrimeraVez(usuario.getEmail(), FabricaUsuarios.PASSWORD);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/logout")
                .header("Authorization", "Bearer " + sesion.token()))
            .andExpect(status().isNoContent());

        auth.me(sesion.token()).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("TOKEN_REVOCADO"));
    }

    @Test
    void elTokenDeAccesoCompletoExpiraAlaHora() throws Exception {
        Usuario usuario = usuario();
        ApiAuth.Sesion sesion = auth.iniciarSesionPorPrimeraVez(usuario.getEmail(), FabricaUsuarios.PASSWORD);

        auth.me(sesion.token()).andExpect(status().isOk());
        reloj.avanzar(Duration.ofMinutes(61));

        auth.me(sesion.token()).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("TOKEN_EXPIRADO"));
    }

    @Test
    void unDesafioDeOtroUsuarioNoSePuedeUsar() throws Exception {
        Usuario ana = usuario();
        Usuario beto = usuario();
        String tokenDeAna = auth.mfaToken(ana.getEmail(), FabricaUsuarios.PASSWORD);
        String secretoDeBeto = auth.leer(auth.enrolar(auth.mfaToken(beto.getEmail(), FabricaUsuarios.PASSWORD))
            .andReturn().getResponse().getContentAsString()).get("secreto").asText();

        auth.verificar(tokenDeAna, auth.codigoActual(secretoDeBeto)).andExpect(status().isConflict());
        assertThat(usuarios.findByEmail(ana.getEmail()).orElseThrow().isMfaHabilitado()).isFalse();
    }
}
