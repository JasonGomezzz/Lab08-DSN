package com.techstore.inventario.autorizacion;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import com.techstore.inventario.usuarios.Rol;
import com.techstore.inventario.usuarios.Usuario;
import org.springframework.stereotype.Component;

/**
 * Autorización en dos capas, siempre en el servidor:
 * 1) por rol (RBAC): qué acciones puede ejecutar cada perfil;
 * 2) por atributo (ABAC): los perfiles de tienda solo operan sobre recursos de su propia tienda.
 */
@Component
public class PoliticaAcceso {
    private static final Map<Rol, Set<Accion>> PERMISOS = Map.of(
        Rol.ADMINISTRADOR, EnumSet.allOf(Accion.class),
        Rol.GERENTE_TIENDA, EnumSet.of(Accion.VER_PRODUCTOS, Accion.CREAR_PRODUCTO, Accion.EDITAR_PRODUCTO,
            Accion.ACTUALIZAR_STOCK, Accion.ELIMINAR_PRODUCTO, Accion.VER_REPORTE),
        Rol.EMPLEADO_VENTAS, EnumSet.of(Accion.VER_PRODUCTOS, Accion.ACTUALIZAR_STOCK),
        Rol.AUDITOR, EnumSet.of(Accion.VER_PRODUCTOS, Accion.VER_REPORTE, Accion.VER_USUARIOS));

    public boolean permite(Rol rol, Accion accion) {
        return PERMISOS.get(rol).contains(accion);
    }

    public Set<Accion> permisosDe(Rol rol) {
        return EnumSet.copyOf(PERMISOS.get(rol));
    }

    /** El administrador y el auditor ven todas las tiendas; los demás, solo la suya. */
    public boolean alcanceGlobal(Rol rol) {
        return rol == Rol.ADMINISTRADOR || rol == Rol.AUDITOR;
    }

    public void exigir(Usuario usuario, Accion accion) {
        if (!permite(usuario.getRol(), accion)) {
            throw new AccesoDenegadoException("ROL_SIN_PERMISO", "Tu perfil no permite esta operación");
        }
    }

    public void exigir(Usuario usuario, Accion accion, Long tiendaDelRecurso) {
        exigir(usuario, accion);
        if (!alcanceGlobal(usuario.getRol()) && !tiendaDe(usuario).equals(tiendaDelRecurso)) {
            throw new AccesoDenegadoException("TIENDA_AJENA", "Solo puedes operar sobre los datos de tu tienda");
        }
    }

    /** Tienda del usuario; sin tienda asignada un perfil de tienda no puede operar. */
    public Long tiendaDe(Usuario usuario) {
        if (usuario.getTienda() == null) {
            throw new AccesoDenegadoException("SIN_TIENDA", "Todavía no tienes una tienda asignada");
        }
        return usuario.getTienda().getId();
    }
}
