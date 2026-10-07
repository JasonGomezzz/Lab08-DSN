package com.techstore.inventario.comun;

import java.util.List;
import java.util.Map;

/** Errores de validación agrupados por campo. */
public class DatosInvalidosException extends RuntimeException {
    private final Map<String, List<String>> errores;

    public DatosInvalidosException(Map<String, List<String>> errores) {
        super("Hay datos inválidos en la solicitud");
        this.errores = errores;
    }

    public DatosInvalidosException(String campo, String mensaje) {
        this(Map.of(campo, List.of(mensaje)));
    }

    public Map<String, List<String>> getErrores() {
        return errores;
    }
}
