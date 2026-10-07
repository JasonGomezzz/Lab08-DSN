package com.techstore.inventario.productos;

import com.techstore.inventario.usuarios.Usuario;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {
    private final ServicioReportes reportes;

    public ReporteController(ServicioReportes reportes) {
        this.reportes = reportes;
    }

    @GetMapping("/inventario")
    public ReporteInventario inventario(@AuthenticationPrincipal Usuario usuario) {
        return reportes.inventario(usuario);
    }
}
