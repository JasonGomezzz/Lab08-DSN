package com.techstore.inventario;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.inventario.autenticacion.Totp;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Cliente de pruebas del flujo de autenticación: encapsula las llamadas HTTP repetidas. */
public class ApiAuth {
    private final MockMvc api;
    private final ObjectMapper json;
    private final RelojDePrueba reloj;

    public ApiAuth(MockMvc api, ObjectMapper json, RelojDePrueba reloj) {
        this.api = api;
        this.json = json;
        this.reloj = reloj;
    }

    public record Sesion(String token, String secreto) {
    }

    public ResultActions login(String email, String password) throws Exception {
        return api.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email", email, "password", password))));
    }

    public String mfaToken(String email, String password) throws Exception {
        return leer(login(email, password).andReturn().getResponse().getContentAsString()).get("mfaToken").asText();
    }

    public ResultActions enrolar(String mfaToken) throws Exception {
        return api.perform(post("/api/auth/mfa/enrolar").header("Authorization", "Bearer " + mfaToken));
    }

    public ResultActions verificar(String mfaToken, String codigo) throws Exception {
        return api.perform(post("/api/auth/mfa/verificar").header("Authorization", "Bearer " + mfaToken)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("codigo", codigo))));
    }

    public ResultActions me(String token) throws Exception {
        return api.perform(get("/api/auth/me").header("Authorization", "Bearer " + token));
    }

    public String codigoActual(String secreto) {
        return Totp.codigo(secreto, Totp.pasoDe(reloj.instant()));
    }

    /** Recorre el flujo completo por primera vez (login, enrolamiento y verificación). */
    public Sesion iniciarSesionPorPrimeraVez(String email, String password) throws Exception {
        String mfaToken = mfaToken(email, password);
        String secreto = leer(enrolar(mfaToken).andReturn().getResponse().getContentAsString()).get("secreto").asText();
        String token = leer(verificar(mfaToken, codigoActual(secreto)).andReturn().getResponse().getContentAsString())
            .get("token").asText();
        return new Sesion(token, secreto);
    }

    /** Inicia sesión con un usuario que ya activó MFA; avanza el reloj para obtener un código nuevo. */
    public String iniciarSesion(String email, String password, String secreto) throws Exception {
        reloj.avanzar(java.time.Duration.ofSeconds(Totp.PERIODO_SEGUNDOS));
        String mfaToken = mfaToken(email, password);
        return leer(verificar(mfaToken, codigoActual(secreto)).andReturn().getResponse().getContentAsString())
            .get("token").asText();
    }

    public JsonNode leer(String contenido) throws Exception {
        return json.readTree(contenido);
    }
}
