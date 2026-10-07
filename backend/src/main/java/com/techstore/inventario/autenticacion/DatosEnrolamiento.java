package com.techstore.inventario.autenticacion;

/** Lo que la app autenticadora necesita para registrar la cuenta: el secreto y la URI del QR. */
public record DatosEnrolamiento(String secreto, String otpauthUri) {
}
