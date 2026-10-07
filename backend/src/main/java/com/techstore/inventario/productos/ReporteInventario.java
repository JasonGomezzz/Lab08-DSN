package com.techstore.inventario.productos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ReporteInventario(Instant generadoEn, int umbralStockBajo, List<ResumenTienda> tiendas,
                                ResumenTienda total) {

    public static ReporteInventario de(Instant generadoEn, int umbral, List<ResumenTienda> tiendas) {
        ResumenTienda total = new ResumenTienda(null, "TOTAL", "Todas las tiendas visibles",
            tiendas.stream().mapToLong(ResumenTienda::productos).sum(),
            tiendas.stream().mapToLong(ResumenTienda::unidades).sum(),
            tiendas.stream().map(ResumenTienda::valorInventario).reduce(BigDecimal.ZERO, BigDecimal::add),
            tiendas.stream().mapToLong(ResumenTienda::productosConPocoStock).sum());
        return new ReporteInventario(generadoEn, umbral, tiendas, total);
    }
}
