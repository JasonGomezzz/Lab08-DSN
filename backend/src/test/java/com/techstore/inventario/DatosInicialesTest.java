package com.techstore.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import com.techstore.inventario.comun.DatosIniciales;
import com.techstore.inventario.comun.PropiedadesTechStore;
import com.techstore.inventario.productos.ProductoRepository;
import com.techstore.inventario.tiendas.TiendaRepository;
import com.techstore.inventario.usuarios.Rol;
import com.techstore.inventario.usuarios.Usuario;
import com.techstore.inventario.usuarios.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
    "techstore.admin.email=Admin.Semilla@TechStore.test",
    "techstore.admin.password=Semilla#2026",
    "techstore.demo.activo=true",
    "techstore.demo.password=Demostracion#2026"})
class DatosInicialesTest extends PruebaIntegracion {
    @Autowired DatosIniciales semilla;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProductoRepository productos;
    @Autowired TiendaRepository tiendas;
    @Autowired PasswordEncoder codificador;
    @Autowired Clock reloj;

    @Test
    void creaElAdministradorInicialConLaContrasenaCifradaYSinTienda() {
        Usuario admin = usuarios.findByEmail("admin.semilla@techstore.test").orElseThrow();

        assertThat(admin.getRol()).isEqualTo(Rol.ADMINISTRADOR);
        assertThat(admin.getTienda()).isNull();
        assertThat(admin.getPasswordHash()).startsWith("$2").doesNotContain("Semilla");
        assertThat(codificador.matches("Semilla#2026", admin.getPasswordHash())).isTrue();
        assertThat(admin.isMfaHabilitado()).isFalse();
    }

    @Test
    void cargaUnUsuarioPorPerfilYProductosDeDemostracion() {
        assertThat(usuarios.findByEmail("gerente.lima@techstore.demo").orElseThrow().getRol())
            .isEqualTo(Rol.GERENTE_TIENDA);
        assertThat(usuarios.findByEmail("ventas.arequipa@techstore.demo").orElseThrow().getTienda().getCodigo())
            .isEqualTo("AQP-01");
        assertThat(usuarios.findByEmail("auditor@techstore.demo").orElseThrow().getRol()).isEqualTo(Rol.AUDITOR);
        Long lima = tiendas.findByCodigo("LIM-01").orElseThrow().getId();
        assertThat(productos.findByTiendaIdOrderByNombreAsc(lima)).hasSize(6);
    }

    @Test
    void sembrarDosVecesNoDuplicaNada() {
        long usuariosAntes = usuarios.count();
        long productosAntes = productos.count();

        semilla.run(null);

        assertThat(usuarios.count()).isEqualTo(usuariosAntes);
        assertThat(productos.count()).isEqualTo(productosAntes);
    }

    @Test
    void sinCredencialesDeAdministradorNoSeCreaNinguno() {
        UsuarioRepository repositorio = mock(UsuarioRepository.class);
        DatosIniciales sinAdmin = sembrador(repositorio, new PropiedadesTechStore.Admin("", ""),
            new PropiedadesTechStore.Demo(false, ""));

        sinAdmin.run(null);

        verifyNoInteractions(repositorio);
    }

    @Test
    void unaContrasenaDeAdministradorDebilImpideArrancar() {
        UsuarioRepository repositorio = mock(UsuarioRepository.class);
        when(repositorio.existsByEmail("admin@techstore.test")).thenReturn(false);
        DatosIniciales debil = sembrador(repositorio, new PropiedadesTechStore.Admin("admin@techstore.test", "admin"),
            new PropiedadesTechStore.Demo(false, ""));

        assertThatThrownBy(() -> debil.run(null)).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ADMIN_PASSWORD");
    }

    @Test
    void losDatosDeDemostracionExigenUnaContrasena() {
        DatosIniciales sinClave = sembrador(mock(UsuarioRepository.class), new PropiedadesTechStore.Admin("", ""),
            new PropiedadesTechStore.Demo(true, ""));

        assertThatThrownBy(() -> sinClave.run(null)).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("DEMO_PASSWORD");
    }

    private DatosIniciales sembrador(UsuarioRepository repositorio, PropiedadesTechStore.Admin admin,
                                     PropiedadesTechStore.Demo demo) {
        return new DatosIniciales(repositorio, tiendas, productos, codificador,
            new PropiedadesTechStore(null, null, null, null, null, admin, demo), reloj);
    }
}
