package com.techstore.inventario.autenticacion;

import com.techstore.inventario.usuarios.UsuarioDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final ServicioRegistro registro;

    public AuthController(ServicioRegistro registro) {
        this.registro = registro;
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioDto> registrar(@Valid @RequestBody SolicitudRegistro solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioDto.de(registro.registrar(solicitud)));
    }
}
