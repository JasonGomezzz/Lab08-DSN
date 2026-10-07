package com.techstore.inventario.tiendas;

public record TiendaDto(Long id, String codigo, String nombre, String ciudad) {

    public static TiendaDto de(Tienda tienda) {
        return tienda == null ? null : new TiendaDto(tienda.getId(), tienda.getCodigo(), tienda.getNombre(), tienda.getCiudad());
    }
}
