package com.techstore.inventario.autenticacion;

import com.techstore.inventario.usuarios.Usuario;
import com.techstore.inventario.usuarios.UsuarioDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final ServicioRegistro registro;
    private final ServicioAutenticacion autenticacion;

    public AuthController(ServicioRegistro registro, ServicioAutenticacion autenticacion) {
        this.registro = registro;
        this.autenticacion = autenticacion;
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioDto> registrar(@Valid @RequestBody SolicitudRegistro solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioDto.de(registro.registrar(solicitud)));
    }

    @PostMapping("/login")
    public ResultadoLogin login(@Valid @RequestBody Credenciales credenciales) {
        return autenticacion.ingresar(credenciales.email(), credenciales.password());
    }

    /** Requiere el token MFA parcial que entrega el login. */
    @PostMapping("/mfa/enrolar")
    public DatosEnrolamiento enrolar(@AuthenticationPrincipal Usuario usuario, Authentication sesion) {
        return autenticacion.enrolar(usuario, (ServicioJwt.DatosToken) sesion.getCredentials());
    }

    @PostMapping("/mfa/verificar")
    public SesionIniciada verificar(@AuthenticationPrincipal Usuario usuario, Authentication sesion,
                                    @Valid @RequestBody CodigoMfa codigo) {
        return autenticacion.verificarMfa(usuario, (ServicioJwt.DatosToken) sesion.getCredentials(), codigo.codigo());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication sesion) {
        autenticacion.cerrarSesion((ServicioJwt.DatosToken) sesion.getCredentials());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UsuarioDto me(@AuthenticationPrincipal Usuario usuario) {
        return UsuarioDto.de(usuario);
    }

    public record Credenciales(
        @NotBlank(message = "El correo es obligatorio") String email,
        @NotBlank(message = "La contraseña es obligatoria") String password) {
    }

    public record CodigoMfa(@NotBlank(message = "El código es obligatorio") String codigo) {
    }
}
