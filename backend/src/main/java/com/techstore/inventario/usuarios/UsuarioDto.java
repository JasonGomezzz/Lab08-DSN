package com.techstore.inventario.usuarios;

import com.techstore.inventario.tiendas.TiendaDto;

/** Vista pública de un usuario: nunca incluye el hash de la contraseña ni el secreto MFA. */
public record UsuarioDto(Long id, String email, String nombreCompleto, Rol rol, TiendaDto tienda,
                         ProveedorIdentidad proveedor, boolean mfaHabilitado) {

    public static UsuarioDto de(Usuario usuario) {
        return new UsuarioDto(usuario.getId(), usuario.getEmail(), usuario.getNombreCompleto(), usuario.getRol(),
            TiendaDto.de(usuario.getTienda()), usuario.getProveedor(), usuario.isMfaHabilitado());
    }
}
