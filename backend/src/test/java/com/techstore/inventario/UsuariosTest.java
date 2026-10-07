package com.techstore.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.inventario.autenticacion.ServicioJwt;
import com.techstore.inventario.tiendas.Tienda;
import com.techstore.inventario.usuarios.Rol;
import com.techstore.inventario.usuarios.Usuario;
import com.techstore.inventario.usuarios.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@AutoConfigureMockMvc
class UsuariosTest extends PruebaIntegracion {
    @Autowired MockMvc api;
    @Autowired ObjectMapper json;
    @Autowired ServicioJwt jwt;
    @Autowired FabricaUsuarios fabrica;
    @Autowired UsuarioRepository usuarios;
    @Autowired RelojDePrueba reloj;

    Tienda lima;
    Usuario admin;
    Usuario auditor;
    Usuario gerente;
    Usuario empleado;
    ApiAuth auth;

    @BeforeEach
    void preparar() {
        lima = fabrica.tienda("LIM-01");
        admin = fabrica.crear(Rol.ADMINISTRADOR, null);
        auditor = fabrica.crear(Rol.AUDITOR, null);
        gerente = fabrica.crear(Rol.GERENTE_TIENDA, lima);
        empleado = fabrica.crear(Rol.EMPLEADO_VENTAS, lima);
        auth = new ApiAuth(api, json, reloj);
    }

    @AfterEach
    void restablecerReloj() {
        reloj.restablecer();
    }

    private ResultActions como(Usuario usuario, MockHttpServletRequestBuilder peticion) throws Exception {
        return api.perform(peticion.header("Authorization", "Bearer " + jwt.emitirAcceso(usuario.getId()).token()));
    }

    private MockHttpServletRequestBuilder conCuerpo(MockHttpServletRequestBuilder peticion, Object cuerpo)
            throws Exception {
        return peticion.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo));
    }

    @Test
    void elAdministradorYElAuditorListanUsuariosPeroNoElGerenteNiElEmpleado() throws Exception {
        como(admin, get("/api/usuarios")).andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id==" + empleado.getId() + ")]").isNotEmpty());
        como(auditor, get("/api/usuarios")).andExpect(status().isOk());
        como(gerente, get("/api/usuarios")).andExpect(status().isForbidden());
        como(empleado, get("/api/usuarios")).andExpect(status().isForbidden());
    }

    @Test
    void elListadoNuncaExponeHashesNiSecretos() throws Exception {
        como(admin, get("/api/usuarios")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("password"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("$2"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secreto"))));
    }

    @Test
    void elAdministradorCambiaElRolYElEfectoEsInmediatoParaElTokenYaEmitido() throws Exception {
        String tokenDelEmpleado = jwt.emitirAcceso(empleado.getId()).token();
        api.perform(get("/api/reportes/inventario").header("Authorization", "Bearer " + tokenDelEmpleado))
            .andExpect(status().isForbidden());

        como(admin, conCuerpo(patch("/api/usuarios/" + empleado.getId() + "/rol"), Map.of("rol", "GERENTE_TIENDA")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("GERENTE_TIENDA"));

        api.perform(get("/api/reportes/inventario").header("Authorization", "Bearer " + tokenDelEmpleado))
            .andExpect(status().isOk());
    }

    @Test
    void soloElAdministradorPuedeCambiarRoles() throws Exception {
        for (Usuario actor : new Usuario[] {auditor, gerente, empleado}) {
            como(actor, conCuerpo(patch("/api/usuarios/" + empleado.getId() + "/rol"), Map.of("rol", "ADMINISTRADOR")))
                .andExpect(status().isForbidden());
        }
        assertThat(usuarios.findById(empleado.getId()).orElseThrow().getRol()).isEqualTo(Rol.EMPLEADO_VENTAS);
    }

    @Test
    void unAdministradorNoPuedeCambiarseSuPropioRol() throws Exception {
        como(admin, conCuerpo(patch("/api/usuarios/" + admin.getId() + "/rol"), Map.of("rol", "AUDITOR")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("NO_CAMBIAR_PROPIO_ROL"));
    }

    @Test
    void rechazaRolesInexistentesYUsuariosInexistentes() throws Exception {
        como(admin, conCuerpo(patch("/api/usuarios/" + empleado.getId() + "/rol"), Map.of("rol", "SUPERUSUARIO")))
            .andExpect(status().isBadRequest());
        como(admin, conCuerpo(patch("/api/usuarios/" + empleado.getId() + "/rol"), Map.of()))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.rol").isNotEmpty());
        como(admin, conCuerpo(patch("/api/usuarios/999999999/rol"), Map.of("rol", "AUDITOR")))
            .andExpect(status().isNotFound());
    }

    @Test
    void elAdministradorAsignaLaTiendaDeUnUsuario() throws Exception {
        Usuario sinTienda = fabrica.crear(Rol.EMPLEADO_VENTAS, null);

        como(admin, conCuerpo(patch("/api/usuarios/" + sinTienda.getId() + "/tienda"), Map.of("tiendaId", lima.getId())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.tienda.codigo").value("LIM-01"));
        como(admin, conCuerpo(patch("/api/usuarios/" + sinTienda.getId() + "/tienda"), Map.of("tiendaId", 999_999_999L)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void elAdministradorDesbloqueaUnaCuentaBloqueada() throws Exception {
        for (int intento = 1; intento <= 5; intento++) {
            auth.login(empleado.getEmail(), "Incorrecta#" + intento);
        }
        auth.login(empleado.getEmail(), FabricaUsuarios.PASSWORD).andExpect(status().isLocked());
        como(admin, get("/api/usuarios")).andExpect(jsonPath("$[?(@.id==" + empleado.getId() + ")].bloqueado").value(true));

        como(admin, post("/api/usuarios/" + empleado.getId() + "/desbloquear"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.bloqueado").value(false));

        auth.login(empleado.getEmail(), FabricaUsuarios.PASSWORD).andExpect(status().isOk());
    }

    @Test
    void reiniciarElMfaObligaAEnrolarLaAppDeNuevoEInvalidaElSecretoAnterior() throws Exception {
        ApiAuth.Sesion sesion = auth.iniciarSesionPorPrimeraVez(empleado.getEmail(), FabricaUsuarios.PASSWORD);
        reloj.avanzar(Duration.ofSeconds(30));
        String desafioPendiente = auth.mfaToken(empleado.getEmail(), FabricaUsuarios.PASSWORD);

        como(admin, post("/api/usuarios/" + empleado.getId() + "/reiniciar-mfa"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.mfaHabilitado").value(false));

        auth.verificar(desafioPendiente, auth.codigoActual(sesion.secreto())).andExpect(status().isUnauthorized());
        auth.login(empleado.getEmail(), FabricaUsuarios.PASSWORD).andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("MFA_ENROLAMIENTO"));
        String nuevoToken = auth.mfaToken(empleado.getEmail(), FabricaUsuarios.PASSWORD);
        String nuevoSecreto = auth.leer(auth.enrolar(nuevoToken).andReturn().getResponse().getContentAsString())
            .get("secreto").asText();
        assertThat(nuevoSecreto).isNotEqualTo(sesion.secreto());
        auth.verificar(nuevoToken, auth.codigoActual(nuevoSecreto)).andExpect(status().isOk());
    }

    @Test
    void soloElAdministradorPuedeDesbloquearOReiniciarElMfa() throws Exception {
        como(gerente, post("/api/usuarios/" + empleado.getId() + "/desbloquear")).andExpect(status().isForbidden());
        como(auditor, post("/api/usuarios/" + empleado.getId() + "/reiniciar-mfa")).andExpect(status().isForbidden());
    }

    @Test
    void quienEntroPorLaViaSocialEligeSuTiendaUnaSolaVez() throws Exception {
        Usuario social = fabrica.crear(Rol.EMPLEADO_VENTAS, null);

        como(social, conCuerpo(put("/api/auth/me/tienda"), Map.of("tiendaId", lima.getId())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.tienda.codigo").value("LIM-01"))
            .andExpect(jsonPath("$.permisos").isNotEmpty());
        como(social, conCuerpo(put("/api/auth/me/tienda"), Map.of("tiendaId", fabrica.tienda("AQP-01").getId())))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("TIENDA_YA_ASIGNADA"));
        assertThat(usuarios.findWithTiendaById(social.getId()).orElseThrow().getTienda().getCodigo()).isEqualTo("LIM-01");
    }

    @Test
    void laTiendaElegidaDebeExistir() throws Exception {
        Usuario social = fabrica.crear(Rol.EMPLEADO_VENTAS, null);

        como(social, conCuerpo(put("/api/auth/me/tienda"), Map.of("tiendaId", 999_999_999L)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.tiendaId").isNotEmpty());
        como(social, conCuerpo(put("/api/auth/me/tienda"), Map.of())).andExpect(status().isBadRequest());
    }
}
