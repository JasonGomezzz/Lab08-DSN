package com.techstore.inventario.usuarios;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final ServicioUsuarios servicio;

    public UsuarioController(ServicioUsuarios servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<UsuarioAdminDto> listar(@AuthenticationPrincipal Usuario actor) {
        return servicio.listar(actor);
    }

    @PatchMapping("/{id}/rol")
    public UsuarioAdminDto cambiarRol(@AuthenticationPrincipal Usuario actor, @PathVariable Long id,
                                      @Valid @RequestBody CambioRol cambio) {
        return servicio.cambiarRol(actor, id, cambio.rol());
    }

    @PatchMapping("/{id}/tienda")
    public UsuarioAdminDto asignarTienda(@AuthenticationPrincipal Usuario actor, @PathVariable Long id,
                                         @Valid @RequestBody CambioTienda cambio) {
        return servicio.asignarTienda(actor, id, cambio.tiendaId());
    }

    @PostMapping("/{id}/desbloquear")
    public UsuarioAdminDto desbloquear(@AuthenticationPrincipal Usuario actor, @PathVariable Long id) {
        return servicio.desbloquear(actor, id);
    }

    @PostMapping("/{id}/reiniciar-mfa")
    public UsuarioAdminDto reiniciarMfa(@AuthenticationPrincipal Usuario actor, @PathVariable Long id) {
        return servicio.reiniciarMfa(actor, id);
    }

    public record CambioRol(@NotNull(message = "El rol es obligatorio") Rol rol) {
    }

    public record CambioTienda(@NotNull(message = "La tienda es obligatoria") Long tiendaId) {
    }
}
