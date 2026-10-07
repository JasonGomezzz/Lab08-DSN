package com.techstore.inventario.autenticacion;

import java.util.ArrayList;
import java.util.List;
import com.techstore.inventario.autorizacion.PoliticaAcceso;
import com.techstore.inventario.usuarios.PerfilDto;
import com.techstore.inventario.usuarios.ServicioUsuarios;
import com.techstore.inventario.usuarios.Usuario;
import com.techstore.inventario.usuarios.UsuarioDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final ServicioRegistro registro;
    private final ServicioAutenticacion autenticacion;
    private final ObjectProvider<ClientRegistrationRepository> proveedores;
    private final PoliticaAcceso politica;
    private final ServicioUsuarios servicioUsuarios;

    public AuthController(ServicioRegistro registro, ServicioAutenticacion autenticacion,
                          ObjectProvider<ClientRegistrationRepository> proveedores, PoliticaAcceso politica,
                          ServicioUsuarios servicioUsuarios) {
        this.registro = registro;
        this.autenticacion = autenticacion;
        this.proveedores = proveedores;
        this.politica = politica;
        this.servicioUsuarios = servicioUsuarios;
    }

    /** Proveedores sociales con credenciales: la interfaz solo muestra los botones de estos. */
    @GetMapping("/proveedores")
    public List<String> proveedoresSociales() {
        List<String> habilitados = new ArrayList<>();
        if (proveedores.getIfAvailable() instanceof Iterable<?> registros) {
            for (Object registro : registros) {
                habilitados.add(((ClientRegistration) registro).getRegistrationId());
            }
        }
        habilitados.sort(String::compareTo);
        return habilitados;
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
    public PerfilDto me(@AuthenticationPrincipal Usuario usuario) {
        return PerfilDto.de(usuario, politica.permisosDe(usuario.getRol()));
    }

    /** Para quien entró con Google o GitHub y aún no tiene tienda; solo se puede hacer una vez. */
    @PutMapping("/me/tienda")
    public PerfilDto elegirTienda(@AuthenticationPrincipal Usuario usuario, @Valid @RequestBody ElegirTienda datos) {
        Usuario actualizado = servicioUsuarios.elegirTiendaPropia(usuario, datos.tiendaId());
        return PerfilDto.de(actualizado, politica.permisosDe(actualizado.getRol()));
    }

    public record ElegirTienda(@NotNull(message = "La tienda es obligatoria") Long tiendaId) {
    }

    public record Credenciales(
        @NotBlank(message = "El correo es obligatorio") String email,
        @NotBlank(message = "La contraseña es obligatoria") String password) {
    }

    public record CodigoMfa(@NotBlank(message = "El código es obligatorio") String codigo) {
    }
}
