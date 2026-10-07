package com.techstore.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.techstore.inventario.autenticacion.ServicioUsuarioOAuth2;
import com.techstore.inventario.comun.PropiedadesTechStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ServicioUsuarioOAuth2Test {
    private static final String URL = "https://api.github.test/user/emails";

    private MockRestServiceServer servidor;
    private ServicioUsuarioOAuth2 servicio;

    @BeforeEach
    void preparar() {
        RestClient.Builder constructor = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(constructor).build();
        servicio = new ServicioUsuarioOAuth2(constructor, new PropiedadesTechStore(null, null, null, null,
            new PropiedadesTechStore.Oauth2(null, null, URL)));
    }

    @Test
    void eligeElCorreoPrincipalYVerificadoEnviandoElTokenDelUsuario() {
        servidor.expect(requestTo(URL)).andExpect(header("Authorization", "Bearer tok-1")).andRespond(withSuccess("""
            [{"email":"otro@correo.com","primary":false,"verified":true},
             {"email":"ana@correo.com","primary":true,"verified":true}]""", MediaType.APPLICATION_JSON));

        assertThat(servicio.correoVerificado("tok-1")).contains("ana@correo.com");
        servidor.verify();
    }

    @Test
    void ignoraElCorreoPrincipalSiNoEstaVerificado() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess("""
            [{"email":"ana@correo.com","primary":true,"verified":false}]""", MediaType.APPLICATION_JSON));

        assertThat(servicio.correoVerificado("tok-1")).isEmpty();
    }

    @Test
    void ignoraUnCorreoVerificadoQueNoEsElPrincipal() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess("""
            [{"email":"secundario@correo.com","primary":false,"verified":true}]""", MediaType.APPLICATION_JSON));

        assertThat(servicio.correoVerificado("tok-1")).isEmpty();
    }

    @Test
    void siGithubFallaNoHayCorreoVerificado() {
        servidor.expect(requestTo(URL)).andRespond(withServerError());

        assertThat(servicio.correoVerificado("tok-1")).isEmpty();
    }
}
