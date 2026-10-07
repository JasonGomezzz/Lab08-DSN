package com.techstore.inventario.autenticacion;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

/** Reglas del enunciado: mínimo 8 caracteres, mayúscula, número y carácter especial. */
public final class PoliticaPassword {
    public static final int MINIMO = 8;
    public static final int MAXIMO = 64;
    /** BCrypt solo usa los primeros 72 bytes; se rechaza lo que se truncaría en silencio. */
    private static final int MAXIMO_BYTES = 72;

    private PoliticaPassword() {
    }

    /** Devuelve las reglas incumplidas; una lista vacía significa que la contraseña es válida. */
    public static List<String> validar(String password) {
        List<String> faltas = new ArrayList<>();
        if (password == null) {
            faltas.add("La contraseña es obligatoria");
            return faltas;
        }
        if (password.length() < MINIMO || password.length() > MAXIMO) {
            faltas.add("Debe tener entre " + MINIMO + " y " + MAXIMO + " caracteres");
        } else if (password.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES) {
            faltas.add("Es demasiado larga: no puede superar " + MAXIMO_BYTES + " bytes en UTF-8");
        }
        if (!contiene(password, Character::isUpperCase)) {
            faltas.add("Debe incluir al menos una letra mayúscula");
        }
        if (!contiene(password, Character::isDigit)) {
            faltas.add("Debe incluir al menos un número");
        }
        if (!contiene(password, c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))) {
            faltas.add("Debe incluir al menos un carácter especial");
        }
        return faltas;
    }

    private static boolean contiene(String texto, IntPredicate regla) {
        return texto.codePoints().anyMatch(regla);
    }
}
