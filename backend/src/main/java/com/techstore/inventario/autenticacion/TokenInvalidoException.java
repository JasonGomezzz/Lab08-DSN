package com.techstore.inventario.autenticacion;

public class TokenInvalidoException extends RuntimeException {
    public TokenInvalidoException(String codigo) {
        super(codigo);
    }
}
