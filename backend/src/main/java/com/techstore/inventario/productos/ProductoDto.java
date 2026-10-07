package com.techstore.inventario.productos;

import java.math.BigDecimal;
import com.techstore.inventario.tiendas.TiendaDto;

public record ProductoDto(Long id, String sku, String nombre, String categoria, BigDecimal precio, int stock,
                          TiendaDto tienda) {

    public static ProductoDto de(Producto producto) {
        return new ProductoDto(producto.getId(), producto.getSku(), producto.getNombre(), producto.getCategoria(),
            producto.getPrecio(), producto.getStock(), TiendaDto.de(producto.getTienda()));
    }
}
