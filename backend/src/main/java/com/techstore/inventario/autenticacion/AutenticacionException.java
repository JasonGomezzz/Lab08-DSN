package com.techstore.inventario.autenticacion;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** Fallo de autenticación con código estable para que la interfaz decida qué mostrar. */
public class AutenticacionException extends RuntimeException {
    private final HttpStatus estado;
    private final String codigo;
    private final Map<String, Object> extras;

    public AutenticacionException(HttpStatus estado, String codigo, String mensaje) {
        this(estado, codigo, mensaje, Map.of());
    }

    public AutenticacionException(HttpStatus estado, String codigo, String mensaje, Map<String, Object> extras) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
        this.extras = extras;
    }

    public HttpStatus getEstado() {
        return estado;
    }

    public String getCodigo() {
        return codigo;
    }

    public Map<String, Object> getExtras() {
        return extras;
    }
}
