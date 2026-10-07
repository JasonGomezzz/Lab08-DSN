package com.techstore.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Set;
import com.techstore.inventario.autorizacion.AccesoDenegadoException;
import com.techstore.inventario.autorizacion.Accion;
import com.techstore.inventario.autorizacion.PoliticaAcceso;
import com.techstore.inventario.tiendas.Tienda;
import com.techstore.inventario.usuarios.Rol;
import com.techstore.inventario.usuarios.Usuario;
import org.junit.jupiter.api.Test;

class PoliticaAccesoTest {
    private final PoliticaAcceso politica = new PoliticaAcceso();

    private Tienda tienda(long id) {
        Tienda tienda = new Tienda();
        tienda.setId(id);
        return tienda;
    }

    private Usuario usuario(Rol rol, Tienda tienda) {
        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setTienda(tienda);
        return usuario;
    }

    @Test
    void laMatrizDePermisosCoincideConLosPerfilesDelCaso() {
        Map<Rol, Set<Accion>> esperado = Map.of(
            Rol.ADMINISTRADOR, Set.of(Accion.values()),
            Rol.GERENTE_TIENDA, Set.of(Accion.VER_PRODUCTOS, Accion.CREAR_PRODUCTO, Accion.EDITAR_PRODUCTO,
                Accion.ACTUALIZAR_STOCK, Accion.ELIMINAR_PRODUCTO, Accion.VER_REPORTE),
            Rol.EMPLEADO_VENTAS, Set.of(Accion.VER_PRODUCTOS, Accion.ACTUALIZAR_STOCK),
            Rol.AUDITOR, Set.of(Accion.VER_PRODUCTOS, Accion.VER_REPORTE, Accion.VER_USUARIOS));

        for (Rol rol : Rol.values()) {
            for (Accion accion : Accion.values()) {
                assertThat(politica.permite(rol, accion)).as(rol + " / " + accion)
                    .isEqualTo(esperado.get(rol).contains(accion));
            }
        }
    }

    @Test
    void elEmpleadoDeVentasNoPuedeModificarPreciosNiEliminar() {
        assertThat(politica.permite(Rol.EMPLEADO_VENTAS, Accion.EDITAR_PRODUCTO)).isFalse();
        assertThat(politica.permite(Rol.EMPLEADO_VENTAS, Accion.ELIMINAR_PRODUCTO)).isFalse();
        assertThat(politica.permite(Rol.EMPLEADO_VENTAS, Accion.ACTUALIZAR_STOCK)).isTrue();
    }

    @Test
    void elAuditorEsSoloLecturaYNingunaAccionDeEscrituraEstaPermitida() {
        for (Accion accion : Set.of(Accion.CREAR_PRODUCTO, Accion.EDITAR_PRODUCTO, Accion.ACTUALIZAR_STOCK,
            Accion.ELIMINAR_PRODUCTO, Accion.GESTIONAR_USUARIOS)) {
            assertThat(politica.permite(Rol.AUDITOR, accion)).as(accion.name()).isFalse();
        }
    }

    @Test
    void soloElAdministradorGestionaUsuarios() {
        for (Rol rol : Rol.values()) {
            assertThat(politica.permite(rol, Accion.GESTIONAR_USUARIOS)).isEqualTo(rol == Rol.ADMINISTRADOR);
        }
    }

    @Test
    void administradorYAuditorTienenAlcanceGlobalYLosDemasSoloSuTienda() {
        assertThat(politica.alcanceGlobal(Rol.ADMINISTRADOR)).isTrue();
        assertThat(politica.alcanceGlobal(Rol.AUDITOR)).isTrue();
        assertThat(politica.alcanceGlobal(Rol.GERENTE_TIENDA)).isFalse();
        assertThat(politica.alcanceGlobal(Rol.EMPLEADO_VENTAS)).isFalse();
    }

    @Test
    void elGerenteSoloOperaSobreRecursosDeSuTienda() {
        Usuario gerente = usuario(Rol.GERENTE_TIENDA, tienda(1));

        politica.exigir(gerente, Accion.ELIMINAR_PRODUCTO, 1L);
        assertThatThrownBy(() -> politica.exigir(gerente, Accion.ELIMINAR_PRODUCTO, 2L))
            .isInstanceOf(AccesoDenegadoException.class).extracting("codigo").isEqualTo("TIENDA_AJENA");
    }

    @Test
    void elAdministradorOperaSobreCualquierTienda() {
        Usuario admin = usuario(Rol.ADMINISTRADOR, null);

        politica.exigir(admin, Accion.ELIMINAR_PRODUCTO, 1L);
        politica.exigir(admin, Accion.ELIMINAR_PRODUCTO, 99L);
    }

    @Test
    void elRolSinPermisoSeRechazaAunqueSeaDeLaMismaTienda() {
        Usuario empleado = usuario(Rol.EMPLEADO_VENTAS, tienda(1));

        assertThatThrownBy(() -> politica.exigir(empleado, Accion.EDITAR_PRODUCTO, 1L))
            .isInstanceOf(AccesoDenegadoException.class).extracting("codigo").isEqualTo("ROL_SIN_PERMISO");
    }

    @Test
    void unPerfilDeTiendaSinTiendaAsignadaNoPuedeOperar() {
        Usuario sinTienda = usuario(Rol.EMPLEADO_VENTAS, null);

        assertThatThrownBy(() -> politica.exigir(sinTienda, Accion.ACTUALIZAR_STOCK, 1L))
            .isInstanceOf(AccesoDenegadoException.class).extracting("codigo").isEqualTo("SIN_TIENDA");
    }

    @Test
    void laListaDePermisosDeUnRolEsUnaCopiaQueNoAlteraLaMatriz() {
        Set<Accion> permisos = politica.permisosDe(Rol.AUDITOR);
        permisos.add(Accion.GESTIONAR_USUARIOS);

        assertThat(politica.permite(Rol.AUDITOR, Accion.GESTIONAR_USUARIOS)).isFalse();
    }
}
