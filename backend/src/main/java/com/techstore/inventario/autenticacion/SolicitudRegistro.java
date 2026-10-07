package com.techstore.inventario.autenticacion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** No incluye el rol: el registro público nunca puede asignar permisos. */
public record SolicitudRegistro(
    @NotBlank(message = "El correo es obligatorio")
    @Size(max = 160, message = "El correo no puede superar 160 caracteres")
    @Email(message = "El correo no tiene un formato válido")
    @Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$", message = "El correo no tiene un formato válido")
    String email,
    @NotBlank(message = "La contraseña es obligatoria")
    String password,
    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 3, max = 120, message = "El nombre completo debe tener entre 3 y 120 caracteres")
    String nombreCompleto,
    @NotNull(message = "La tienda es obligatoria")
    Long tiendaId) {
}
