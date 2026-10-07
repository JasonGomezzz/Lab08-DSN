package com.techstore.inventario;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.UUID;
import com.techstore.inventario.autenticacion.ServicioJwt;
import com.techstore.inventario.usuarios.Rol;
import com.techstore.inventario.usuarios.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class SeguridadTest extends PruebaIntegracion {
    @Autowired MockMvc api;
    @Autowired ServicioJwt jwt;
    @Autowired FabricaUsuarios fabrica;
    @Autowired RelojDePrueba reloj;

    @AfterEach
    void restablecerReloj() {
        reloj.restablecer();
    }

    private Usuario usuario() {
        return fabrica.crear(Rol.EMPLEADO_VENTAS, fabrica.tienda("LIM-01"));
    }

    @Test
    void sinTokenLasRutasProtegidasRespondenUnauthorizedConProblemJson() throws Exception {
        api.perform(get("/api/ruta-protegida"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("TOKEN_AUSENTE"));
    }

    @Test
    void conTokenInvalidoElCodigoIndicaElMotivo() throws Exception {
        api.perform(get("/api/ruta-protegida").header("Authorization", "Bearer basura"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
        api.perform(get("/api/ruta-protegida").header("Authorization", "Basic abc"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
    }

    @Test
    void conTokenExpiradoElCodigoEsTokenExpirado() throws Exception {
        String token = jwt.emitirAcceso(usuario().getId()).token();
        reloj.avanzar(Duration.ofMinutes(61));

        api.perform(get("/api/ruta-protegida").header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("TOKEN_EXPIRADO"));
    }

    @Test
    void tokenDeUnUsuarioInexistenteSeRechaza() throws Exception {
        String token = jwt.emitirAcceso(999_999_999L).token();

        api.perform(get("/api/ruta-protegida").header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
    }

    @Test
    void conTokenDeAccesoValidoLaPeticionPasaLaSeguridad() throws Exception {
        String token = jwt.emitirAcceso(usuario().getId()).token();

        api.perform(get("/api/ruta-que-no-existe").header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }

    @Test
    void elTokenMfaParcialNoPermiteUsarLaApi() throws Exception {
        String token = jwt.emitirMfa(usuario().getId(), UUID.randomUUID().toString()).token();

        api.perform(get("/api/ruta-protegida").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("TOKEN_NO_PERMITIDO"));
    }

    @Test
    void elTokenDeAccesoNoPermiteLasRutasDelSegundoFactor() throws Exception {
        String token = jwt.emitirAcceso(usuario().getId()).token();

        api.perform(post("/api/auth/mfa/verificar").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("TOKEN_NO_PERMITIDO"));
    }

    @Test
    void unTokenInvalidoNoBloqueaLasRutasPublicas() throws Exception {
        api.perform(get("/api/actuator/health").header("Authorization", "Bearer basura"))
            .andExpect(status().isOk());
    }
}
