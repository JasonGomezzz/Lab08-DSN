package com.techstore.inventario.usuarios;

import java.time.Instant;
import com.techstore.inventario.tiendas.TiendaDto;

/** Vista de administración: agrega el estado de bloqueo, sin hashes ni secretos. */
public record UsuarioAdminDto(Long id, String email, String nombreCompleto, Rol rol, TiendaDto tienda,
                              ProveedorIdentidad proveedor, boolean mfaHabilitado, boolean bloqueado,
                              Instant bloqueadoHasta) {

    public static UsuarioAdminDto de(Usuario usuario, Instant ahora) {
        boolean bloqueado = usuario.estaBloqueado(ahora);
        return new UsuarioAdminDto(usuario.getId(), usuario.getEmail(), usuario.getNombreCompleto(),
            usuario.getRol(), TiendaDto.de(usuario.getTienda()), usuario.getProveedor(),
            usuario.isMfaHabilitado(), bloqueado, bloqueado ? usuario.getBloqueadoHasta() : null);
    }
}
