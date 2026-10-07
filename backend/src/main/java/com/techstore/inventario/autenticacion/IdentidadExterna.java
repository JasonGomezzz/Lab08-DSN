package com.techstore.inventario.autenticacion;

import java.util.Locale;
import java.util.Map;
import com.techstore.inventario.usuarios.ProveedorIdentidad;

/** Lo mínimo que se necesita de Google o GitHub para crear o reconocer una cuenta. */
public record IdentidadExterna(ProveedorIdentidad proveedor, String idExterno, String email, String nombre,
                               boolean emailVerificado) {

    /**
     * Normaliza los atributos que devuelve cada proveedor. Para GitHub, {@code email} y
     * {@code email_verified} los inyecta {@link ServicioUsuarioOAuth2} a partir de /user/emails.
     */
    public static IdentidadExterna de(String registro, Map<String, Object> atributos) {
        return switch (registro) {
            case "google" -> nueva(ProveedorIdentidad.GOOGLE, texto(atributos, "sub"), atributos, "name",
                "true".equalsIgnoreCase(texto(atributos, "email_verified")));
            case "github" -> nueva(ProveedorIdentidad.GITHUB, texto(atributos, "id"), atributos, "login",
                Boolean.TRUE.equals(atributos.get("email_verified")));
            default -> throw new IllegalArgumentException("Proveedor no soportado: " + registro);
        };
    }

    private static IdentidadExterna nueva(ProveedorIdentidad proveedor, String id, Map<String, Object> atributos,
                                          String atributoNombre, boolean verificado) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El proveedor no entregó un identificador de usuario");
        }
        String email = texto(atributos, "email");
        String nombre = texto(atributos, "name");
        if (nombre == null || nombre.isBlank()) {
            nombre = texto(atributos, atributoNombre);
        }
        if ((nombre == null || nombre.isBlank()) && email != null) {
            nombre = email.substring(0, Math.max(email.indexOf('@'), 1));
        }
        if (nombre == null || nombre.isBlank()) {
            nombre = "Usuario " + proveedor.name().toLowerCase(Locale.ROOT);
        }
        nombre = nombre.strip();
        return new IdentidadExterna(proveedor, id, email == null || email.isBlank() ? null
            : email.strip().toLowerCase(Locale.ROOT), nombre.length() > 120 ? nombre.substring(0, 120) : nombre,
            verificado && email != null && !email.isBlank());
    }

    private static String texto(Map<String, Object> atributos, String clave) {
        Object valor = atributos.get(clave);
        return valor == null ? null : String.valueOf(valor);
    }
}
