package com.techstore.inventario.autenticacion;

import com.techstore.inventario.usuarios.UsuarioDto;

public record SesionIniciada(String token, String tipo, long expiraEnSegundos, UsuarioDto usuario) {
}
