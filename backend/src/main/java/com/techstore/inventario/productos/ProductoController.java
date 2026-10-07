package com.techstore.inventario.productos;

import java.util.List;
import com.techstore.inventario.usuarios.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {
    private final ServicioProductos productos;

    public ProductoController(ServicioProductos productos) {
        this.productos = productos;
    }

    @GetMapping
    public List<ProductoDto> listar(@AuthenticationPrincipal Usuario usuario,
                                    @RequestParam(required = false) Long tiendaId) {
        return productos.listar(usuario, tiendaId);
    }

    @GetMapping("/{id}")
    public ProductoDto obtener(@AuthenticationPrincipal Usuario usuario, @PathVariable Long id) {
        return productos.obtener(usuario, id);
    }

    @PostMapping
    public ResponseEntity<ProductoDto> crear(@AuthenticationPrincipal Usuario usuario,
                                             @Valid @RequestBody SolicitudProducto solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productos.crear(usuario, solicitud));
    }

    @PutMapping("/{id}")
    public ProductoDto editar(@AuthenticationPrincipal Usuario usuario, @PathVariable Long id,
                              @Valid @RequestBody SolicitudEdicionProducto solicitud) {
        return productos.editar(usuario, id, solicitud);
    }

    @PatchMapping("/{id}/stock")
    public ProductoDto ajustarStock(@AuthenticationPrincipal Usuario usuario, @PathVariable Long id,
                                    @Valid @RequestBody SolicitudAjusteStock solicitud) {
        return productos.ajustarStock(usuario, id, solicitud.ajuste());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal Usuario usuario, @PathVariable Long id) {
        productos.eliminar(usuario, id);
        return ResponseEntity.noContent().build();
    }
}
