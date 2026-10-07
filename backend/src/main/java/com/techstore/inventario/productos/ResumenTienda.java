package com.techstore.inventario.productos;

import java.math.BigDecimal;

public record ResumenTienda(Long tiendaId, String codigo, String nombre, long productos, long unidades,
                            BigDecimal valorInventario, long productosConPocoStock) {
}
