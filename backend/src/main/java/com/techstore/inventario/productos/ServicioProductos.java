package com.techstore.inventario.productos;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import com.techstore.inventario.autorizacion.Accion;
import com.techstore.inventario.autorizacion.PoliticaAcceso;
import com.techstore.inventario.comun.ConflictoEstadoException;
import com.techstore.inventario.comun.DatosInvalidosException;
import com.techstore.inventario.tiendas.Tienda;
import com.techstore.inventario.tiendas.TiendaRepository;
import com.techstore.inventario.usuarios.Rol;
import com.techstore.inventario.usuarios.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cada operación valida primero el permiso (rol) y luego la tienda del recurso (atributo). */
@Service
public class ServicioProductos {
    private final ProductoRepository productos;
    private final TiendaRepository tiendas;
    private final PoliticaAcceso politica;
    private final Clock reloj;

    public ServicioProductos(ProductoRepository productos, TiendaRepository tiendas, PoliticaAcceso politica,
                             Clock reloj) {
        this.productos = productos;
        this.tiendas = tiendas;
        this.politica = politica;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<ProductoDto> listar(Usuario usuario, Long tiendaId) {
        politica.exigir(usuario, Accion.VER_PRODUCTOS);
        List<Producto> lista;
        if (politica.alcanceGlobal(usuario.getRol())) {
            lista = tiendaId == null ? productos.findAllByOrderByNombreAsc()
                : productos.findByTiendaIdOrderByNombreAsc(tiendaId);
        } else {
            politica.exigir(usuario, Accion.VER_PRODUCTOS, tiendaId == null ? politica.tiendaDe(usuario) : tiendaId);
            lista = productos.findByTiendaIdOrderByNombreAsc(politica.tiendaDe(usuario));
        }
        return lista.stream().map(ProductoDto::de).toList();
    }

    @Transactional(readOnly = true)
    public ProductoDto obtener(Usuario usuario, Long id) {
        Producto producto = buscar(id);
        politica.exigir(usuario, Accion.VER_PRODUCTOS, producto.getTienda().getId());
        return ProductoDto.de(producto);
    }

    @Transactional
    public ProductoDto crear(Usuario usuario, SolicitudProducto solicitud) {
        politica.exigir(usuario, Accion.CREAR_PRODUCTO);
        Long tiendaId = tiendaDestino(usuario, solicitud.tiendaId());
        politica.exigir(usuario, Accion.CREAR_PRODUCTO, tiendaId);
        Tienda tienda = tiendas.findById(tiendaId)
            .orElseThrow(() -> new DatosInvalidosException("tiendaId", "La tienda indicada no existe"));
        String sku = solicitud.sku().toUpperCase(Locale.ROOT);
        if (productos.existsByTiendaIdAndSku(tiendaId, sku)) {
            throw new ConflictoEstadoException("SKU_DUPLICADO", "Ya existe un producto con ese SKU en la tienda");
        }
        Producto producto = new Producto();
        producto.setSku(sku);
        producto.setNombre(solicitud.nombre().trim());
        producto.setCategoria(solicitud.categoria().trim());
        producto.setPrecio(solicitud.precio());
        producto.setStock(solicitud.stock() == null ? 0 : solicitud.stock());
        producto.setTienda(tienda);
        producto.setCreadoEn(reloj.instant());
        producto.setActualizadoEn(reloj.instant());
        return ProductoDto.de(productos.save(producto));
    }

    @Transactional
    public ProductoDto editar(Usuario usuario, Long id, SolicitudEdicionProducto solicitud) {
        Producto producto = buscar(id);
        politica.exigir(usuario, Accion.EDITAR_PRODUCTO, producto.getTienda().getId());
        producto.setNombre(solicitud.nombre().trim());
        producto.setCategoria(solicitud.categoria().trim());
        producto.setPrecio(solicitud.precio());
        producto.setActualizadoEn(reloj.instant());
        return ProductoDto.de(producto);
    }

    @Transactional
    public ProductoDto ajustarStock(Usuario usuario, Long id, int ajuste) {
        Producto producto = buscar(id);
        politica.exigir(usuario, Accion.ACTUALIZAR_STOCK, producto.getTienda().getId());
        if (ajuste == 0) {
            throw new DatosInvalidosException("ajuste", "El ajuste no puede ser cero");
        }
        if (productos.ajustarStock(id, ajuste, reloj.instant()) == 0) {
            throw new ConflictoEstadoException("STOCK_INSUFICIENTE",
                "El ajuste dejaría el stock en negativo (stock actual: " + producto.getStock() + ")");
        }
        return ProductoDto.de(buscar(id));
    }

    @Transactional
    public void eliminar(Usuario usuario, Long id) {
        Producto producto = buscar(id);
        politica.exigir(usuario, Accion.ELIMINAR_PRODUCTO, producto.getTienda().getId());
        productos.delete(producto);
    }

    private Producto buscar(Long id) {
        return productos.findWithTiendaById(id).orElseThrow(() -> new NoSuchElementException("Producto " + id));
    }

    /** El administrador elige la tienda; los perfiles de tienda solo pueden usar la suya. */
    private Long tiendaDestino(Usuario usuario, Long solicitada) {
        if (usuario.getRol() == Rol.ADMINISTRADOR) {
            if (solicitada == null) {
                throw new DatosInvalidosException("tiendaId", "La tienda es obligatoria");
            }
            return solicitada;
        }
        return solicitada == null ? politica.tiendaDe(usuario) : solicitada;
    }
}
