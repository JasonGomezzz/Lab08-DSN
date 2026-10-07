package com.techstore.inventario.productos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Variación de unidades: negativa al vender, positiva al reponer. */
public record SolicitudAjusteStock(
    @NotNull(message = "El ajuste es obligatorio")
    @Min(value = -100_000, message = "El ajuste mínimo es -100000")
    @Max(value = 100_000, message = "El ajuste máximo es 100000")
    Integer ajuste) {
}
