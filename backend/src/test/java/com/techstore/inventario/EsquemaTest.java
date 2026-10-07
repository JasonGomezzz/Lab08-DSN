package com.techstore.inventario;

import static org.assertj.core.api.Assertions.assertThat;

import com.techstore.inventario.tiendas.Tienda;
import com.techstore.inventario.tiendas.TiendaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EsquemaTest extends PruebaIntegracion {
    @Autowired TiendaRepository tiendas;

    @Test
    void lasEntidadesCoincidenConElEsquemaDeFlywayYEstanLasTresTiendasSemilla() {
        assertThat(tiendas.findAllByOrderByNombreAsc())
            .extracting(Tienda::getCodigo)
            .contains("LIM-01", "AQP-01", "TRU-01");
    }
}
