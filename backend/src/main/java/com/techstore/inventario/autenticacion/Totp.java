package com.techstore.inventario.autenticacion;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.OptionalLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * TOTP según RFC 6238 con los parámetros que entienden Google Authenticator y similares:
 * HMAC-SHA1, 6 dígitos y paso de 30 segundos.
 */
public final class Totp {
    public static final int DIGITOS = 6;
    public static final int PERIODO_SEGUNDOS = 30;
    private static final int BYTES_SECRETO = 20;
    private static final String ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Totp() {
    }

    /** Secreto aleatorio de 160 bits en Base32, el formato que espera la app autenticadora. */
    public static String generarSecreto() {
        byte[] bytes = new byte[BYTES_SECRETO];
        ALEATORIO.nextBytes(bytes);
        return codificarBase32(bytes);
    }

    public static long pasoDe(Instant instante) {
        return instante.getEpochSecond() / PERIODO_SEGUNDOS;
    }

    public static String codigo(String secretoBase32, long paso) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(decodificarBase32(secretoBase32), "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(paso).array());
            int desplazamiento = hash[hash.length - 1] & 0x0f;
            int binario = ((hash[desplazamiento] & 0x7f) << 24)
                | ((hash[desplazamiento + 1] & 0xff) << 16)
                | ((hash[desplazamiento + 2] & 0xff) << 8)
                | (hash[desplazamiento + 3] & 0xff);
            return String.format("%0" + DIGITOS + "d", binario % 1_000_000);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo calcular el código TOTP", ex);
        }
    }

    /**
     * Busca el código en una ventana de ±{@code ventana} pasos para tolerar relojes desfasados.
     * Solo acepta pasos posteriores a {@code ultimoPasoUsado}, de modo que un código no se pueda reutilizar.
     *
     * @return el paso que coincidió, o vacío si el código no es válido
     */
    public static OptionalLong verificar(String secretoBase32, String ingresado, Instant ahora, int ventana,
                                         Long ultimoPasoUsado) {
        if (ingresado == null || !ingresado.matches("\\d{" + DIGITOS + "}")) {
            return OptionalLong.empty();
        }
        long actual = pasoDe(ahora);
        long minimo = ultimoPasoUsado == null ? Long.MIN_VALUE : ultimoPasoUsado + 1;
        long coincidencia = -1;
        boolean encontrado = false;
        for (long paso = actual - ventana; paso <= actual + ventana; paso++) {
            boolean igual = MessageDigest.isEqual(
                codigo(secretoBase32, paso).getBytes(StandardCharsets.UTF_8),
                ingresado.getBytes(StandardCharsets.UTF_8));
            if (igual && paso >= minimo && !encontrado) {
                coincidencia = paso;
                encontrado = true;
            }
        }
        return encontrado ? OptionalLong.of(coincidencia) : OptionalLong.empty();
    }

    /** URI que codifica el QR de enrolamiento: otpauth://totp/Emisor:cuenta?secret=... */
    public static String uriOtpAuth(String emisor, String cuenta, String secretoBase32) {
        String emisorCodificado = codificarUri(emisor);
        return "otpauth://totp/" + emisorCodificado + ":" + codificarUri(cuenta)
            + "?secret=" + secretoBase32
            + "&issuer=" + emisorCodificado
            + "&algorithm=SHA1&digits=" + DIGITOS + "&period=" + PERIODO_SEGUNDOS;
    }

    private static String codificarUri(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public static String codificarBase32(byte[] datos) {
        StringBuilder resultado = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte dato : datos) {
            buffer = (buffer << 8) | (dato & 0xff);
            bits += 8;
            while (bits >= 5) {
                resultado.append(ALFABETO.charAt((buffer >> (bits - 5)) & 0x1f));
                bits -= 5;
            }
        }
        if (bits > 0) {
            resultado.append(ALFABETO.charAt((buffer << (5 - bits)) & 0x1f));
        }
        return resultado.toString();
    }

    public static byte[] decodificarBase32(String texto) {
        String limpio = texto.replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT);
        ByteBuffer salida = ByteBuffer.allocate(limpio.length() * 5 / 8);
        int buffer = 0;
        int bits = 0;
        for (char c : limpio.toCharArray()) {
            int valor = ALFABETO.indexOf(c);
            if (valor < 0) {
                throw new IllegalArgumentException("Carácter Base32 inválido");
            }
            buffer = (buffer << 5) | valor;
            bits += 5;
            if (bits >= 8) {
                salida.put((byte) ((buffer >> (bits - 8)) & 0xff));
                bits -= 8;
            }
        }
        return salida.array();
    }
}
