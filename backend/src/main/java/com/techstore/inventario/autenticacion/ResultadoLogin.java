package com.techstore.inventario.autenticacion;

/** Resultado del primer factor: nunca contiene el token completo, solo el de MFA. */
public record ResultadoLogin(String estado, String mfaToken, long expiraEnSegundos) {
    public static final String MFA_REQUERIDO = "MFA_REQUERIDO";
    public static final String MFA_ENROLAMIENTO = "MFA_ENROLAMIENTO";
}
