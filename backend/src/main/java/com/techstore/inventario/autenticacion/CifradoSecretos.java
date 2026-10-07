package com.techstore.inventario.autenticacion;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import com.techstore.inventario.comun.PropiedadesTechStore;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Cifra en reposo los secretos TOTP con AES-256-GCM. Un secreto TOTP no puede guardarse como hash
 * porque hay que recuperarlo para validar cada código; por eso se cifra con una clave que vive
 * fuera de la base de datos. El dato asociado (AAD) ata cada secreto a su usuario.
 */
@Component
public class CifradoSecretos {
    private static final String PREFIJO = "v1:";
    /** Prefijo de los marcadores de .env.example: una clave que lo conserve es pública. */
    private static final String VALOR_DE_EJEMPLO = "CAMBIAR";
    private static final int BYTES_IV = 12;
    private static final int BITS_ETIQUETA = 128;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final SecretKey clave;

    public CifradoSecretos(PropiedadesTechStore propiedades) {
        String material = propiedades.mfa().claveCifrado();
        if (material == null || material.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("MFA_CLAVE_CIFRADO debe tener al menos 32 bytes UTF-8");
        }
        if (material.startsWith(VALOR_DE_EJEMPLO)) {
            throw new IllegalStateException(
                "MFA_CLAVE_CIFRADO sigue con el valor de ejemplo de .env.example: genera una propia");
        }
        try {
            byte[] derivada = MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
            this.clave = new SecretKeySpec(derivada, "AES");
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    public String cifrar(String texto, String asociado) {
        try {
            byte[] iv = new byte[BYTES_IV];
            ALEATORIO.nextBytes(iv);
            Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
            cifrador.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(BITS_ETIQUETA, iv));
            cifrador.updateAAD(asociado.getBytes(StandardCharsets.UTF_8));
            byte[] cifrado = cifrador.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            ByteBuffer salida = ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado);
            return PREFIJO + Base64.getEncoder().encodeToString(salida.array());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo cifrar el secreto", ex);
        }
    }

    public String descifrar(String almacenado, String asociado) {
        if (almacenado == null || !almacenado.startsWith(PREFIJO)) {
            throw new IllegalStateException("Formato de secreto cifrado desconocido");
        }
        try {
            byte[] datos = Base64.getDecoder().decode(almacenado.substring(PREFIJO.length()));
            Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
            cifrador.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(BITS_ETIQUETA, datos, 0, BYTES_IV));
            cifrador.updateAAD(asociado.getBytes(StandardCharsets.UTF_8));
            return new String(cifrador.doFinal(datos, BYTES_IV, datos.length - BYTES_IV), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("No se pudo descifrar el secreto", ex);
        }
    }
}
