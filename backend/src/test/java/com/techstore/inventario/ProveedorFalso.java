package com.techstore.inventario;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Proveedor OAuth2 de mentira para probar el flujo completo sin cuentas reales: intercambia un
 * código por un token y sirve el perfil y los correos que cada prueba haya registrado.
 */
public final class ProveedorFalso {
    private static final Map<String, String> PERFILES = new ConcurrentHashMap<>();
    private static final Map<String, String> CORREOS = new ConcurrentHashMap<>();
    private static final HttpServer SERVIDOR = iniciar();

    private ProveedorFalso() {
    }

    public static String base() {
        return "http://localhost:" + SERVIDOR.getAddress().getPort();
    }

    /** El código OAuth2 que el "usuario" devolverá al callback, con su perfil y sus correos de GitHub. */
    public static void registrar(String codigo, String perfilJson, String correosJson) {
        PERFILES.put(codigo, perfilJson);
        CORREOS.put(codigo, correosJson == null ? "[]" : correosJson);
    }

    private static HttpServer iniciar() {
        try {
            HttpServer servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            servidor.createContext("/token", ProveedorFalso::token);
            servidor.createContext("/perfil", intercambio -> responderDatos(intercambio, PERFILES));
            servidor.createContext("/correos", intercambio -> responderDatos(intercambio, CORREOS));
            servidor.setExecutor(null);
            servidor.start();
            return servidor;
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo iniciar el proveedor falso", ex);
        }
    }

    private static void token(HttpExchange intercambio) throws IOException {
        String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String codigo = null;
        for (String par : cuerpo.split("&")) {
            if (par.startsWith("code=")) {
                codigo = URLDecoder.decode(par.substring(5), StandardCharsets.UTF_8);
            }
        }
        if (codigo == null || !PERFILES.containsKey(codigo)) {
            responder(intercambio, 400, "{\"error\":\"invalid_grant\"}");
            return;
        }
        responder(intercambio, 200,
            "{\"access_token\":\"tok-" + codigo + "\",\"token_type\":\"bearer\",\"expires_in\":3600}");
    }

    private static void responderDatos(HttpExchange intercambio, Map<String, String> datos) throws IOException {
        String cabecera = intercambio.getRequestHeaders().getFirst("Authorization");
        String codigo = cabecera != null && cabecera.startsWith("Bearer tok-") ? cabecera.substring(11) : null;
        if (codigo == null || !datos.containsKey(codigo)) {
            responder(intercambio, 401, "{\"error\":\"invalid_token\"}");
            return;
        }
        responder(intercambio, 200, datos.get(codigo));
    }

    private static void responder(HttpExchange intercambio, int estado, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().add("Content-Type", "application/json");
        intercambio.sendResponseHeaders(estado, bytes.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(bytes);
        }
    }
}
