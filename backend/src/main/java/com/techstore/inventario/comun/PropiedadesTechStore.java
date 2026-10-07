package com.techstore.inventario.comun;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración propia de la aplicación, enlazada al prefijo {@code techstore} de application.yml. */
@ConfigurationProperties(prefix = "techstore")
public record PropiedadesTechStore(Jwt jwt, Seguridad seguridad, Mfa mfa, Urls urls, Oauth2 oauth2) {

    public record Jwt(String secreto, int expiracionMinutos, int expiracionMfaMinutos) {
    }

    public record Seguridad(int intentosMaximosLogin, int bloqueoMinutos, int intentosMaximosMfa, int bcryptCosto) {
    }

    public record Mfa(String claveCifrado, String emisor) {
    }

    /** {@code base} es la URL pública del backend (redirect de OAuth2); {@code interfaz} es donde aterriza el usuario. */
    public record Urls(String base, String interfaz) {
    }

    public record Oauth2(Proveedor google, Proveedor github, String githubCorreosUri) {
    }

    public record Proveedor(String clientId, String clientSecret) {
        public boolean configurado() {
            return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
        }
    }
}
