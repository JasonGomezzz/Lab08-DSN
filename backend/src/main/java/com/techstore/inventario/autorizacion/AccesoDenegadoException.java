package com.techstore.inventario.autorizacion;

public class AccesoDenegadoException extends RuntimeException {
    private final String codigo;

    public AccesoDenegadoException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
