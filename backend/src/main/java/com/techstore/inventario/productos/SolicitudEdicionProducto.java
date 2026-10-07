package com.techstore.inventario.productos;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos editables de un producto; el stock cambia solo por ajustes y el SKU y la tienda no se mueven. */
public record SolicitudEdicionProducto(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
    String nombre,
    @NotBlank(message = "La categoría es obligatoria")
    @Size(max = 60, message = "La categoría no puede superar 60 caracteres")
    String categoria,
    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El precio admite hasta 8 enteros y 2 decimales")
    BigDecimal precio) {
}
