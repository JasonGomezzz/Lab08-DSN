package com.techstore.inventario.tiendas;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lista pública de tiendas: el formulario de registro la necesita antes de que exista una sesión. */
@RestController
@RequestMapping("/api/tiendas")
public class TiendaController {
    private final TiendaRepository tiendas;

    public TiendaController(TiendaRepository tiendas) {
        this.tiendas = tiendas;
    }

    @GetMapping
    public List<TiendaDto> listar() {
        return tiendas.findAllByOrderByNombreAsc().stream().map(TiendaDto::de).toList();
    }
}
