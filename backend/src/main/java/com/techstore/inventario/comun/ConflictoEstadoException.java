package com.techstore.inventario.comun;

public class ConflictoEstadoException extends RuntimeException {
    private final String codigo;

    public ConflictoEstadoException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
