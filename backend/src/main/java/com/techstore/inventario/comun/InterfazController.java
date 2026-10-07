package com.techstore.inventario.comun;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * La interfaz React viaja dentro del jar. Las rutas de la aplicación de una sola página se reenvían a
 * index.html para que recargar o abrir un enlace directo (por ejemplo, /mfa) no devuelva un 404.
 */
@Controller
public class InterfazController {

    @GetMapping({"/login", "/registro", "/mfa", "/tienda", "/inventario", "/reportes", "/usuarios"})
    public String interfaz() {
        return "forward:/index.html";
    }
}
