package com.techstore.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.inventario.usuarios.ProveedorIdentidad;
import com.techstore.inventario.usuarios.Rol;
import com.techstore.inventario.usuarios.Usuario;
import com.techstore.inventario.usuarios.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class LoginTest extends PruebaIntegracion {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired FabricaUsuarios fabrica;
    @Autowired RelojDePrueba reloj;

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

    @Test
    void conCredencialesCorrectasEntregaSoloUnTokenMfaYPideEnrolarElSegundoFactor() throws Exception {
        Usuario usuario = usuario();

        auth.login(usuario.getEmail(), FabricaUsuarios.PASSWORD)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("MFA_ENROLAMIENTO"))
            .andExpect(jsonPath("$.mfaToken").isNotEmpty())
            .andExpect(jsonPath("$.expiraEnSegundos").value(org.hamcrest.Matchers.lessThanOrEqualTo(300)))
            .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void elCorreoNoDistingueMayusculas() throws Exception {
        Usuario usuario = usuario();

        auth.login(usuario.getEmail().toUpperCase(), FabricaUsuarios.PASSWORD).andExpect(status().isOk());
    }

    @Test
    void contrasenaIncorrectaYCorreoInexistenteDanLaMismaRespuesta() throws Exception {
        Usuario usuario = usuario();
        String incorrecta = auth.login(usuario.getEmail(), "Otra#Clave1").andExpect(status().isUnauthorized())
            .andReturn().getResponse().getContentAsString();
        String inexistente = auth.login("nadie-" + UUID.randomUUID() + "@techstore.test", "Otra#Clave1")
            .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();

        assertThat(auth.leer(incorrecta).get("codigo").asText()).isEqualTo("CREDENCIALES_INVALIDAS");
        assertThat(auth.leer(inexistente).get("codigo").asText()).isEqualTo("CREDENCIALES_INVALIDAS");
        assertThat(auth.leer(incorrecta).get("detail")).isEqualTo(auth.leer(inexistente).get("detail"));
    }

    @Test
    void elQuintoIntentoFallidoBloqueaLaCuentaYElSextoEsRechazadoAunqueSeaCorrecto() throws Exception {
        Usuario usuario = usuario();
        for (int intento = 1; intento <= 4; intento++) {
            auth.login(usuario.getEmail(), "Incorrecta#" + intento).andExpect(status().isUnauthorized());
        }

        auth.login(usuario.getEmail(), "Incorrecta#5")
            .andExpect(status().isLocked())
            .andExpect(jsonPath("$.codigo").value("CUENTA_BLOQUEADA"))
            .andExpect(jsonPath("$.minutosRestantes").value(15));

        auth.login(usuario.getEmail(), FabricaUsuarios.PASSWORD)
            .andExpect(status().isLocked())
            .andExpect(jsonPath("$.mfaToken").doesNotExist());
    }

    @Test
    void elBloqueoSeLevantaSolo15MinutosDespues() throws Exception {
        Usuario usuario = usuario();
        for (int intento = 1; intento <= 5; intento++) {
            auth.login(usuario.getEmail(), "Incorrecta#" + intento);
        }

        reloj.avanzar(Duration.ofMinutes(14));
        auth.login(usuario.getEmail(), FabricaUsuarios.PASSWORD).andExpect(status().isLocked());

        reloj.avanzar(Duration.ofMinutes(2));
        auth.login(usuario.getEmail(), FabricaUsuarios.PASSWORD).andExpect(status().isOk());
    }

    @Test
    void trasDesbloquearseElConteoDeFallosEmpiezaDeNuevo() throws Exception {
        Usuario usuario = usuario();
        for (int intento = 1; intento <= 5; intento++) {
            auth.login(usuario.getEmail(), "Incorrecta#" + intento);
        }
        reloj.avanzar(Duration.ofMinutes(16));

        for (int intento = 1; intento <= 4; intento++) {
            auth.login(usuario.getEmail(), "Incorrecta#" + intento).andExpect(status().isUnauthorized());
        }
        auth.login(usuario.getEmail(), "Incorrecta#5").andExpect(status().isLocked());
    }

    @Test
    void unAciertoCompletoReiniciaElContadorDeFallos() throws Exception {
        Usuario usuario = usuario();
        for (int intento = 1; intento <= 4; intento++) {
            auth.login(usuario.getEmail(), "Incorrecta#" + intento);
        }
        auth.iniciarSesionPorPrimeraVez(usuario.getEmail(), FabricaUsuarios.PASSWORD);

        assertThat(usuarios.findByEmail(usuario.getEmail()).orElseThrow().getIntentosFallidos()).isZero();
        for (int intento = 1; intento <= 4; intento++) {
            auth.login(usuario.getEmail(), "Incorrecta#" + intento).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void elBloqueoDeUnaCuentaNoAfectaAOtra() throws Exception {
        Usuario bloqueada = usuario();
        Usuario libre = usuario();
        for (int intento = 1; intento <= 5; intento++) {
            auth.login(bloqueada.getEmail(), "Incorrecta#" + intento);
        }

        auth.login(libre.getEmail(), FabricaUsuarios.PASSWORD).andExpect(status().isOk());
    }

    @Test
    void unaCuentaSocialNoSePuedeUsarConContrasenaNiSeBloqueaPorIntentosAjenos() throws Exception {
        Usuario social = fabrica.crear(Rol.EMPLEADO_VENTAS, fabrica.tienda("LIM-01"));
        social.setProveedor(ProveedorIdentidad.GOOGLE);
        social.setProveedorId("google-" + UUID.randomUUID());
        social.setPasswordHash(null);
        usuarios.save(social);

        for (int intento = 1; intento <= 8; intento++) {
            auth.login(social.getEmail(), "Intento#" + intento)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
        }
        assertThat(usuarios.findByEmail(social.getEmail()).orElseThrow().getBloqueadoHasta()).isNull();
    }

    @Test
    void rechazaPeticionesSinCorreoOContrasena() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.email").isNotEmpty())
            .andExpect(jsonPath("$.errores.password").isNotEmpty());
    }

    @Test
    void elTokenMfaParcialNoSirveParaConsultarElPerfil() throws Exception {
        Usuario usuario = usuario();
        String mfaToken = auth.mfaToken(usuario.getEmail(), FabricaUsuarios.PASSWORD);

        auth.me(mfaToken).andExpect(status().isForbidden());
    }

    @Test
    void sinCredencialesDeProveedoresNoSeOfreceLoginSocial() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/proveedores"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/oauth2/authorization/google"))
            .andExpect(status().isNotFound());
    }

    @Test
    void laRespuestaDeLoginNuncaIncluyeHashNiSecretos() throws Exception {
        Usuario usuario = usuario();

        auth.login(usuario.getEmail(), FabricaUsuarios.PASSWORD)
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("hash"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("$2"))));
    }
}
