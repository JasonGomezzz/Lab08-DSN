package com.techstore.inventario.usuarios;

import java.util.Set;
import com.techstore.inventario.autorizacion.Accion;
import com.techstore.inventario.tiendas.TiendaDto;

/** Perfil de la sesión. Los permisos solo sirven para mostrar u ocultar botones: el servidor valida igual. */
public record PerfilDto(Long id, String email, String nombreCompleto, Rol rol, TiendaDto tienda,
                        ProveedorIdentidad proveedor, boolean mfaHabilitado, Set<Accion> permisos) {

    public static PerfilDto de(Usuario usuario, Set<Accion> permisos) {
        return new PerfilDto(usuario.getId(), usuario.getEmail(), usuario.getNombreCompleto(), usuario.getRol(),
            TiendaDto.de(usuario.getTienda()), usuario.getProveedor(), usuario.isMfaHabilitado(), permisos);
    }
}
