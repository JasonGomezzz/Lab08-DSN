package com.techstore.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.inventario.autenticacion.ServicioJwt;
import com.techstore.inventario.comun.ConflictoEstadoException;
import com.techstore.inventario.productos.Producto;
import com.techstore.inventario.productos.ProductoRepository;
import com.techstore.inventario.productos.ServicioProductos;
import com.techstore.inventario.tiendas.Tienda;
import com.techstore.inventario.tiendas.TiendaRepository;
import com.techstore.inventario.usuarios.Rol;
import com.techstore.inventario.usuarios.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@AutoConfigureMockMvc
class ProductosTest extends PruebaIntegracion {
    @Autowired MockMvc api;
    @Autowired ObjectMapper json;
    @Autowired ServicioJwt jwt;
    @Autowired FabricaUsuarios fabrica;
    @Autowired ProductoRepository productos;
    @Autowired TiendaRepository tiendas;
    @Autowired ServicioProductos servicio;
    @Autowired java.time.Clock reloj;

    Tienda tiendaA;
    Tienda tiendaB;
    Usuario admin;
    Usuario gerenteA;
    Usuario gerenteB;
    Usuario ventasA;
    Usuario auditor;
    Producto productoA;
    Producto productoB;

    @BeforeEach
    void preparar() {
        tiendaA = nuevaTienda();
        tiendaB = nuevaTienda();
        admin = fabrica.crear(Rol.ADMINISTRADOR, null);
        auditor = fabrica.crear(Rol.AUDITOR, null);
        gerenteA = fabrica.crear(Rol.GERENTE_TIENDA, tiendaA);
        gerenteB = fabrica.crear(Rol.GERENTE_TIENDA, tiendaB);
        ventasA = fabrica.crear(Rol.EMPLEADO_VENTAS, tiendaA);
        productoA = producto(tiendaA, "Laptop A", "100.00", 10);
        productoB = producto(tiendaB, "Laptop B", "200.00", 10);
    }

    private Tienda nuevaTienda() {
        Tienda tienda = new Tienda();
        tienda.setCodigo("T" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        tienda.setNombre("Tienda de prueba");
        tienda.setCiudad("Lima");
        return tiendas.save(tienda);
    }

    private Producto producto(Tienda tienda, String nombre, String precio, int stock) {
        Producto producto = new Producto();
        producto.setSku("SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        producto.setNombre(nombre);
        producto.setCategoria("Laptops");
        producto.setPrecio(new BigDecimal(precio));
        producto.setStock(stock);
        producto.setTienda(tienda);
        producto.setCreadoEn(reloj.instant());
        producto.setActualizadoEn(reloj.instant());
        return productos.save(producto);
    }

    private ResultActions como(Usuario usuario, MockHttpServletRequestBuilder peticion) throws Exception {
        return api.perform(peticion.header("Authorization", "Bearer " + jwt.emitirAcceso(usuario.getId()).token()));
    }

    private MockHttpServletRequestBuilder conCuerpo(MockHttpServletRequestBuilder peticion, Object cuerpo)
            throws Exception {
        return peticion.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo));
    }

    private Map<String, Object> nuevoProducto(String sku, Long tiendaId) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("sku", sku);
        cuerpo.put("nombre", "Monitor 27 pulgadas");
        cuerpo.put("categoria", "Monitores");
        cuerpo.put("precio", "850.50");
        cuerpo.put("stock", 7);
        if (tiendaId != null) {
            cuerpo.put("tiendaId", tiendaId);
        }
        return cuerpo;
    }

    private String sku() {
        return "n-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private int stockActual(Producto producto) {
        return productos.findById(producto.getId()).orElseThrow().getStock();
    }

    // --- Consulta -------------------------------------------------------------------------------

    @Test
    void sinTokenNoSeAccedeAlInventario() throws Exception {
        api.perform(get("/api/productos")).andExpect(status().isUnauthorized());
    }

    @Test
    void elEmpleadoDeVentasSoloVeLosProductosDeSuTienda() throws Exception {
        como(ventasA, get("/api/productos")).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(productoA.getId()));
    }

    @Test
    void unPerfilDeTiendaNoPuedePedirOtraTiendaPorParametro() throws Exception {
        como(ventasA, get("/api/productos").param("tiendaId", tiendaB.getId().toString()))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("TIENDA_AJENA"));
        como(ventasA, get("/api/productos").param("tiendaId", tiendaA.getId().toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void unPerfilDeTiendaNoPuedeLeerUnProductoDeOtraTienda() throws Exception {
        como(gerenteA, get("/api/productos/" + productoB.getId())).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("TIENDA_AJENA"));
        como(gerenteA, get("/api/productos/" + productoA.getId())).andExpect(status().isOk());
    }

    @Test
    void elAdministradorYElAuditorVenTodasLasTiendasYPuedenFiltrar() throws Exception {
        for (Usuario global : List.of(admin, auditor)) {
            como(global, get("/api/productos")).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + productoA.getId() + ")]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.id==" + productoB.getId() + ")]").isNotEmpty());
            como(global, get("/api/productos").param("tiendaId", tiendaB.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(productoB.getId()));
        }
    }

    @Test
    void unPerfilDeTiendaSinTiendaAsignadaNoVeNada() throws Exception {
        Usuario sinTienda = fabrica.crear(Rol.EMPLEADO_VENTAS, null);

        como(sinTienda, get("/api/productos")).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("SIN_TIENDA"));
    }

    @Test
    void unProductoInexistenteDevuelve404() throws Exception {
        como(admin, get("/api/productos/999999999")).andExpect(status().isNotFound());
    }

    // --- Auditor: solo lectura -------------------------------------------------------------------

    @Test
    void elAuditorNoPuedeModificarNada() throws Exception {
        como(auditor, conCuerpo(post("/api/productos"), nuevoProducto(sku(), tiendaA.getId())))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("ROL_SIN_PERMISO"));
        como(auditor, conCuerpo(put("/api/productos/" + productoA.getId()),
            Map.of("nombre", "X", "categoria", "Y", "precio", "1.00"))).andExpect(status().isForbidden());
        como(auditor, conCuerpo(patch("/api/productos/" + productoA.getId() + "/stock"), Map.of("ajuste", 1)))
            .andExpect(status().isForbidden());
        como(auditor, delete("/api/productos/" + productoA.getId())).andExpect(status().isForbidden());
        assertThat(stockActual(productoA)).isEqualTo(10);
    }

    // --- Empleado de ventas ------------------------------------------------------------------------

    @Test
    void elEmpleadoActualizaElStockDeSuTienda() throws Exception {
        como(ventasA, conCuerpo(patch("/api/productos/" + productoA.getId() + "/stock"), Map.of("ajuste", -3)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(7));
        como(ventasA, conCuerpo(patch("/api/productos/" + productoA.getId() + "/stock"), Map.of("ajuste", 5)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(12));
    }

    @Test
    void elEmpleadoNoPuedeModificarPreciosNiCrearNiEliminar() throws Exception {
        como(ventasA, conCuerpo(put("/api/productos/" + productoA.getId()),
            Map.of("nombre", "Laptop A", "categoria", "Laptops", "precio", "1.00")))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("ROL_SIN_PERMISO"));
        como(ventasA, conCuerpo(post("/api/productos"), nuevoProducto(sku(), null))).andExpect(status().isForbidden());
        como(ventasA, delete("/api/productos/" + productoA.getId())).andExpect(status().isForbidden());
        assertThat(productos.findById(productoA.getId()).orElseThrow().getPrecio()).isEqualByComparingTo("100.00");
    }

    @Test
    void elEmpleadoNoPuedeTocarElStockDeOtraTienda() throws Exception {
        como(ventasA, conCuerpo(patch("/api/productos/" + productoB.getId() + "/stock"), Map.of("ajuste", -1)))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("TIENDA_AJENA"));
        assertThat(stockActual(productoB)).isEqualTo(10);
    }

    // --- Gerente de tienda -----------------------------------------------------------------------------

    @Test
    void elGerenteCreaProductosEnSuTiendaSinIndicarlaYElSkuQuedaEnMayusculas() throws Exception {
        String sku = sku();

        como(gerenteA, conCuerpo(post("/api/productos"), nuevoProducto(sku, null)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sku").value(sku.toUpperCase()))
            .andExpect(jsonPath("$.tienda.id").value(tiendaA.getId()))
            .andExpect(jsonPath("$.precio").value(850.50)).andExpect(jsonPath("$.stock").value(7));
    }

    @Test
    void elGerenteNoPuedeCrearEnOtraTienda() throws Exception {
        como(gerenteA, conCuerpo(post("/api/productos"), nuevoProducto(sku(), tiendaB.getId())))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("TIENDA_AJENA"));
    }

    @Test
    void noSePuedeRepetirUnSkuEnLaMismaTiendaPeroSiEnOtra() throws Exception {
        String sku = sku();
        como(gerenteA, conCuerpo(post("/api/productos"), nuevoProducto(sku, null))).andExpect(status().isCreated());

        como(gerenteA, conCuerpo(post("/api/productos"), nuevoProducto(sku.toUpperCase(), null)))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("SKU_DUPLICADO"));
        como(gerenteB, conCuerpo(post("/api/productos"), nuevoProducto(sku, null))).andExpect(status().isCreated());
    }

    @Test
    void elGerenteEditaPreciosDeSuTiendaPeroNoDeLasAjenas() throws Exception {
        Map<String, Object> cambio = Map.of("nombre", "Laptop A Pro", "categoria", "Laptops", "precio", "129.90");

        como(gerenteA, conCuerpo(put("/api/productos/" + productoA.getId()), cambio)).andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Laptop A Pro")).andExpect(jsonPath("$.precio").value(129.90));
        como(gerenteA, conCuerpo(put("/api/productos/" + productoB.getId()), cambio))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("TIENDA_AJENA"));
        assertThat(productos.findById(productoB.getId()).orElseThrow().getPrecio()).isEqualByComparingTo("200.00");
    }

    @Test
    void elGerenteEliminaSoloProductosDeSuTienda() throws Exception {
        como(gerenteA, delete("/api/productos/" + productoB.getId())).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("TIENDA_AJENA"));
        assertThat(productos.existsById(productoB.getId())).isTrue();

        como(gerenteA, delete("/api/productos/" + productoA.getId())).andExpect(status().isNoContent());
        assertThat(productos.existsById(productoA.getId())).isFalse();
        como(gerenteA, delete("/api/productos/" + productoA.getId())).andExpect(status().isNotFound());
    }

    // --- Administrador -----------------------------------------------------------------------------------

    @Test
    void elAdministradorDebeIndicarLaTiendaYPuedeOperarEnCualquiera() throws Exception {
        como(admin, conCuerpo(post("/api/productos"), nuevoProducto(sku(), null))).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.tiendaId").isNotEmpty());
        como(admin, conCuerpo(post("/api/productos"), nuevoProducto(sku(), tiendaB.getId())))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.tienda.id").value(tiendaB.getId()));
        como(admin, conCuerpo(post("/api/productos"), nuevoProducto(sku(), 999_999_999L)))
            .andExpect(status().isBadRequest());
        como(admin, delete("/api/productos/" + productoA.getId())).andExpect(status().isNoContent());
    }

    // --- Stock -----------------------------------------------------------------------------------------------

    @Test
    void unAjusteQueDejariaElStockNegativoSeRechazaYNoCambiaNada() throws Exception {
        como(ventasA, conCuerpo(patch("/api/productos/" + productoA.getId() + "/stock"), Map.of("ajuste", -11)))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"));
        assertThat(stockActual(productoA)).isEqualTo(10);

        como(ventasA, conCuerpo(patch("/api/productos/" + productoA.getId() + "/stock"), Map.of("ajuste", -10)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(0));
    }

    @Test
    void unAjusteDeCeroOFueraDeRangoSeRechaza() throws Exception {
        como(ventasA, conCuerpo(patch("/api/productos/" + productoA.getId() + "/stock"), Map.of("ajuste", 0)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.ajuste").isNotEmpty());
        como(ventasA, conCuerpo(patch("/api/productos/" + productoA.getId() + "/stock"), Map.of("ajuste", 1_000_000)))
            .andExpect(status().isBadRequest());
        como(ventasA, conCuerpo(patch("/api/productos/" + productoA.getId() + "/stock"), Map.of()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void ventasSimultaneasNuncaDejanElStockEnNegativo() throws Exception {
        int hilos = 20;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        try {
            List<Future<Boolean>> resultados = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                Callable<Boolean> venta = () -> {
                    try {
                        servicio.ajustarStock(admin, productoA.getId(), -1);
                        return true;
                    } catch (ConflictoEstadoException ex) {
                        return false;
                    }
                };
                resultados.add(pool.submit(venta));
            }
            long exitosas = 0;
            for (Future<Boolean> resultado : resultados) {
                if (resultado.get()) {
                    exitosas++;
                }
            }
            assertThat(exitosas).isEqualTo(10);
            assertThat(stockActual(productoA)).isZero();
        } finally {
            pool.shutdownNow();
        }
    }

    // --- Validación ----------------------------------------------------------------------------------------------

    @Test
    void rechazaDatosInvalidosAlCrear() throws Exception {
        Map<String, Object> cuerpo = nuevoProducto("sku con espacios!", null);
        cuerpo.put("precio", "-1");
        cuerpo.put("stock", -4);
        como(gerenteA, conCuerpo(post("/api/productos"), cuerpo)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.sku").isNotEmpty()).andExpect(jsonPath("$.errores.precio").isNotEmpty())
            .andExpect(jsonPath("$.errores.stock").isNotEmpty());

        Map<String, Object> decimales = nuevoProducto(sku(), null);
        decimales.put("precio", "10.999");
        como(gerenteA, conCuerpo(post("/api/productos"), decimales)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.precio").isNotEmpty());

        como(gerenteA, conCuerpo(post("/api/productos"), Map.of())).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.sku").isNotEmpty()).andExpect(jsonPath("$.errores.nombre").isNotEmpty())
            .andExpect(jsonPath("$.errores.categoria").isNotEmpty()).andExpect(jsonPath("$.errores.precio").isNotEmpty());
    }

    // --- Perfil y permisos -------------------------------------------------------------------------------------------

    @Test
    void elPerfilIncluyeLosPermisosDelRol() throws Exception {
        como(ventasA, get("/api/auth/me")).andExpect(status().isOk())
            .andExpect(jsonPath("$.rol").value("EMPLEADO_VENTAS"))
            .andExpect(jsonPath("$.tienda.id").value(tiendaA.getId()))
            .andExpect(jsonPath("$.permisos.length()").value(2))
            .andExpect(jsonPath("$.permisos[?(@=='ACTUALIZAR_STOCK')]").isNotEmpty())
            .andExpect(jsonPath("$.permisos[?(@=='EDITAR_PRODUCTO')]").isEmpty());
        como(admin, get("/api/auth/me")).andExpect(jsonPath("$.permisos.length()").value(8));
    }

    // --- Reportes ------------------------------------------------------------------------------------------------------

    @Test
    void elReporteDelGerenteSoloIncluyeSuTiendaConLosTotalesCorrectos() throws Exception {
        producto(tiendaA, "Mouse", "50.50", 4);
        producto(tiendaA, "Teclado", "30.00", 0);

        como(gerenteA, get("/api/reportes/inventario")).andExpect(status().isOk())
            .andExpect(jsonPath("$.tiendas.length()").value(1))
            .andExpect(jsonPath("$.tiendas[0].tiendaId").value(tiendaA.getId()))
            .andExpect(jsonPath("$.tiendas[0].productos").value(3))
            .andExpect(jsonPath("$.tiendas[0].unidades").value(14))
            .andExpect(jsonPath("$.tiendas[0].valorInventario").value(1202.00))
            .andExpect(jsonPath("$.tiendas[0].productosConPocoStock").value(2))
            .andExpect(jsonPath("$.total.productos").value(3))
            .andExpect(jsonPath("$.umbralStockBajo").value(5));
    }

    @Test
    void elAdministradorYElAuditorVenElReporteDeTodasLasTiendas() throws Exception {
        for (Usuario global : List.of(admin, auditor)) {
            como(global, get("/api/reportes/inventario")).andExpect(status().isOk())
                .andExpect(jsonPath("$.tiendas[?(@.tiendaId==" + tiendaA.getId() + ")].productos").value(1))
                .andExpect(jsonPath("$.tiendas[?(@.tiendaId==" + tiendaB.getId() + ")].productos").value(1));
        }
    }

    @Test
    void unaTiendaSinProductosApareceEnElReporteConCeros() throws Exception {
        Tienda vacia = nuevaTienda();

        como(admin, get("/api/reportes/inventario")).andExpect(status().isOk())
            .andExpect(jsonPath("$.tiendas[?(@.tiendaId==" + vacia.getId() + ")].productos").value(0))
            .andExpect(jsonPath("$.tiendas[?(@.tiendaId==" + vacia.getId() + ")].unidades").value(0))
            .andExpect(jsonPath("$.tiendas[?(@.tiendaId==" + vacia.getId() + ")].valorInventario").value(0.0));
    }

    @Test
    void elEmpleadoDeVentasNoPuedeVerReportes() throws Exception {
        como(ventasA, get("/api/reportes/inventario")).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("ROL_SIN_PERMISO"));
    }
}
