package com.techstore.inventario.comun;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración propia de la aplicación, enlazada al prefijo {@code techstore} de application.yml. */
@ConfigurationProperties(prefix = "techstore")
public record PropiedadesTechStore(Jwt jwt, Seguridad seguridad, Mfa mfa) {

    public record Jwt(String secreto, int expiracionMinutos, int expiracionMfaMinutos) {
    }

    public record Seguridad(int intentosMaximosLogin, int bloqueoMinutos, int intentosMaximosMfa, int bcryptCosto) {
    }

    public record Mfa(String claveCifrado, String emisor) {
    }
}
