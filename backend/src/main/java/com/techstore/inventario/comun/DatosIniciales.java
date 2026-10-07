package com.techstore.inventario.comun;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import com.techstore.inventario.autenticacion.PoliticaPassword;
import com.techstore.inventario.productos.Producto;
import com.techstore.inventario.productos.ProductoRepository;
import com.techstore.inventario.tiendas.Tienda;
import com.techstore.inventario.tiendas.TiendaRepository;
import com.techstore.inventario.usuarios.ProveedorIdentidad;
import com.techstore.inventario.usuarios.Rol;
import com.techstore.inventario.usuarios.Usuario;
import com.techstore.inventario.usuarios.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Siembra el administrador inicial (el registro público nunca puede crearlo) y, si se pide, usuarios
 * y productos de demostración. Las credenciales llegan solo por variables de entorno, nunca del código.
 */
@Component
public class DatosIniciales implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private record UsuarioDemo(String email, String nombre, Rol rol, String tienda) {
    }

    private record ProductoDemo(String tienda, String sku, String nombre, String categoria, String precio, int stock) {
    }

    private static final List<UsuarioDemo> USUARIOS_DEMO = List.of(
        new UsuarioDemo("gerente.lima@techstore.demo", "Laura Méndez", Rol.GERENTE_TIENDA, "LIM-01"),
        new UsuarioDemo("ventas.lima@techstore.demo", "Carlos Ruiz", Rol.EMPLEADO_VENTAS, "LIM-01"),
        new UsuarioDemo("gerente.arequipa@techstore.demo", "Marta Quispe", Rol.GERENTE_TIENDA, "AQP-01"),
        new UsuarioDemo("ventas.arequipa@techstore.demo", "Diego Salas", Rol.EMPLEADO_VENTAS, "AQP-01"),
        new UsuarioDemo("auditor@techstore.demo", "Sofía Paredes", Rol.AUDITOR, null));

    private static final List<ProductoDemo> PRODUCTOS_DEMO = List.of(
        new ProductoDemo("LIM-01", "LAP-001", "Laptop Pro 14\"", "Laptops", "3499.00", 12),
        new ProductoDemo("LIM-01", "LAP-002", "Laptop Air 13\"", "Laptops", "2899.00", 3),
        new ProductoDemo("LIM-01", "MON-001", "Monitor 27\" 4K", "Monitores", "1299.90", 8),
        new ProductoDemo("LIM-01", "TEC-001", "Teclado mecánico", "Periféricos", "249.50", 25),
        new ProductoDemo("LIM-01", "MOU-001", "Mouse inalámbrico", "Periféricos", "89.90", 4),
        new ProductoDemo("LIM-01", "AUD-001", "Audífonos con cancelación", "Audio", "599.00", 0),
        new ProductoDemo("AQP-01", "LAP-001", "Laptop Pro 14\"", "Laptops", "3549.00", 6),
        new ProductoDemo("AQP-01", "MON-001", "Monitor 27\" 4K", "Monitores", "1319.90", 2),
        new ProductoDemo("AQP-01", "TEC-001", "Teclado mecánico", "Periféricos", "254.50", 14),
        new ProductoDemo("AQP-01", "CAM-001", "Cámara web Full HD", "Periféricos", "179.00", 9),
        new ProductoDemo("TRU-01", "LAP-002", "Laptop Air 13\"", "Laptops", "2949.00", 7),
        new ProductoDemo("TRU-01", "TAB-001", "Tablet 11\"", "Tablets", "1599.00", 5),
        new ProductoDemo("TRU-01", "AUD-001", "Audífonos con cancelación", "Audio", "609.00", 11));

    private final UsuarioRepository usuarios;
    private final TiendaRepository tiendas;
    private final ProductoRepository productos;
    private final PasswordEncoder codificador;
    private final PropiedadesTechStore propiedades;
    private final Clock reloj;

    public DatosIniciales(UsuarioRepository usuarios, TiendaRepository tiendas, ProductoRepository productos,
                          PasswordEncoder codificador, PropiedadesTechStore propiedades, Clock reloj) {
        this.usuarios = usuarios;
        this.tiendas = tiendas;
        this.productos = productos;
        this.codificador = codificador;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        crearAdministrador();
        if (propiedades.demo().activo()) {
            cargarDemostracion();
        }
    }

    private void crearAdministrador() {
        String email = propiedades.admin().email();
        String password = propiedades.admin().password();
        if (vacio(email) || vacio(password)) {
            log.info("ADMIN_EMAIL y ADMIN_PASSWORD no están definidos: no se crea administrador inicial");
            return;
        }
        if (usuarios.existsByEmail(email.toLowerCase(Locale.ROOT))) {
            return;
        }
        exigirPasswordFuerte("ADMIN_PASSWORD", password);
        crearUsuario(email, "Administrador del Sistema", Rol.ADMINISTRADOR, null, password);
        log.info("Administrador inicial creado: {}", email.toLowerCase(Locale.ROOT));
    }

    private void cargarDemostracion() {
        String password = propiedades.demo().password();
        if (vacio(password)) {
            throw new IllegalStateException("TECHSTORE_DATOS_DEMO está activo pero falta DEMO_PASSWORD");
        }
        exigirPasswordFuerte("DEMO_PASSWORD", password);
        for (UsuarioDemo demo : USUARIOS_DEMO) {
            if (!usuarios.existsByEmail(demo.email())) {
                crearUsuario(demo.email(), demo.nombre(), demo.rol(),
                    demo.tienda() == null ? null : tienda(demo.tienda()), password);
            }
        }
        for (ProductoDemo demo : PRODUCTOS_DEMO) {
            Tienda tienda = tienda(demo.tienda());
            if (!productos.existsByTiendaIdAndSku(tienda.getId(), demo.sku())) {
                Producto producto = new Producto();
                producto.setSku(demo.sku());
                producto.setNombre(demo.nombre());
                producto.setCategoria(demo.categoria());
                producto.setPrecio(new BigDecimal(demo.precio()));
                producto.setStock(demo.stock());
                producto.setTienda(tienda);
                producto.setCreadoEn(reloj.instant());
                producto.setActualizadoEn(reloj.instant());
                productos.save(producto);
            }
        }
        log.info("Datos de demostración cargados ({} usuarios, {} productos)", USUARIOS_DEMO.size(),
            PRODUCTOS_DEMO.size());
    }

    private void crearUsuario(String email, String nombre, Rol rol, Tienda tienda, String password) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email.toLowerCase(Locale.ROOT));
        usuario.setPasswordHash(codificador.encode(password));
        usuario.setNombreCompleto(nombre);
        usuario.setRol(rol);
        usuario.setTienda(tienda);
        usuario.setProveedor(ProveedorIdentidad.LOCAL);
        usuario.setCreadoEn(reloj.instant());
        usuarios.save(usuario);
    }

    private Tienda tienda(String codigo) {
        return tiendas.findByCodigo(codigo)
            .orElseThrow(() -> new IllegalStateException("No existe la tienda semilla " + codigo));
    }

    private static void exigirPasswordFuerte(String variable, String password) {
        List<String> faltas = PoliticaPassword.validar(password);
        if (!faltas.isEmpty()) {
            throw new IllegalStateException(variable + " no cumple la política de contraseñas: "
                + String.join("; ", faltas));
        }
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
