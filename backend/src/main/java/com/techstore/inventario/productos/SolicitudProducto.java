package com.techstore.inventario.productos;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** {@code tiendaId} solo lo elige el administrador; un gerente siempre crea en su propia tienda. */
public record SolicitudProducto(
    @NotBlank(message = "El SKU es obligatorio")
    @Size(max = 40, message = "El SKU no puede superar 40 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "El SKU solo admite letras, números, punto, guion y guion bajo")
    String sku,
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
    String nombre,
    @NotBlank(message = "La categoría es obligatoria")
    @Size(max = 60, message = "La categoría no puede superar 60 caracteres")
    String categoria,
    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El precio admite hasta 8 enteros y 2 decimales")
    BigDecimal precio,
    @Min(value = 0, message = "El stock no puede ser negativo")
    Integer stock,
    Long tiendaId) {
}
