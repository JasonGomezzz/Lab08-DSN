package com.techstore.inventario;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class InterfazTest extends PruebaIntegracion {
    @Autowired MockMvc api;

    @Test
    void lasRutasDeLaInterfazSeReenvianAIndexHtmlSinSesion() throws Exception {
        for (String ruta : new String[] {"/login", "/registro", "/mfa", "/tienda", "/inventario", "/reportes", "/usuarios"}) {
            api.perform(get(ruta)).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
        }
    }

    @Test
    void laRaizSirveLaInterfaz() throws Exception {
        api.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    void lasRutasDeLaApiNoSeReenvianALaInterfaz() throws Exception {
        api.perform(get("/api/inventario")).andExpect(status().isUnauthorized());
    }
}
