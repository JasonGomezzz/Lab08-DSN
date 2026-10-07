package com.techstore.inventario.autorizacion;

/** Operaciones protegidas del sistema; la matriz de qué rol puede cuál vive en {@link PoliticaAcceso}. */
public enum Accion {
    VER_PRODUCTOS,
    CREAR_PRODUCTO,
    EDITAR_PRODUCTO,
    ACTUALIZAR_STOCK,
    ELIMINAR_PRODUCTO,
    VER_REPORTE,
    VER_USUARIOS,
    GESTIONAR_USUARIOS
}
