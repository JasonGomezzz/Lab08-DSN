package com.techstore.inventario.productos;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import com.techstore.inventario.autorizacion.Accion;
import com.techstore.inventario.autorizacion.PoliticaAcceso;
import com.techstore.inventario.usuarios.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioReportes {
    public static final int UMBRAL_STOCK_BAJO = 5;

    private final ProductoRepository productos;
    private final PoliticaAcceso politica;
    private final Clock reloj;

    public ServicioReportes(ProductoRepository productos, PoliticaAcceso politica, Clock reloj) {
        this.productos = productos;
        this.politica = politica;
        this.reloj = reloj;
    }

    /** El administrador y el auditor ven todas las tiendas; un gerente, solo la suya. */
    @Transactional(readOnly = true)
    public ReporteInventario inventario(Usuario usuario) {
        politica.exigir(usuario, Accion.VER_REPORTE);
        Long tiendaId = politica.alcanceGlobal(usuario.getRol()) ? null : politica.tiendaDe(usuario);
        List<ResumenTienda> filas = productos.resumenPorTienda(tiendaId, UMBRAL_STOCK_BAJO).stream()
            .map(fila -> new ResumenTienda(((Number) fila[0]).longValue(), (String) fila[1], (String) fila[2],
                ((Number) fila[3]).longValue(), ((Number) fila[4]).longValue(),
                new BigDecimal(String.valueOf(fila[5])).setScale(2), ((Number) fila[6]).longValue()))
            .toList();
        return ReporteInventario.de(reloj.instant(), UMBRAL_STOCK_BAJO, filas);
    }
}
