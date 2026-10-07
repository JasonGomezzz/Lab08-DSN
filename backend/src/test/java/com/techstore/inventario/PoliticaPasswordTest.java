package com.techstore.inventario;

import static org.assertj.core.api.Assertions.assertThat;

import com.techstore.inventario.autenticacion.PoliticaPassword;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PoliticaPasswordTest {

    @ParameterizedTest
    @ValueSource(strings = {"Segura#2026", "Abcde1!x", "Ñandú9?cuenta", "Con espacio 1!"})
    void aceptaContrasenasQueCumplenTodasLasReglas(String password) {
        assertThat(PoliticaPassword.validar(password)).isEmpty();
    }

    @Test
    void rechazaMenosDeOchoCaracteres() {
        assertThat(PoliticaPassword.validar("Ab1!xyz")).containsExactly("Debe tener entre 8 y 64 caracteres");
    }

    @Test
    void rechazaMasDeSesentaYCuatroCaracteres() {
        String larga = "Aa1!" + "x".repeat(61);
        assertThat(PoliticaPassword.validar(larga)).containsExactly("Debe tener entre 8 y 64 caracteres");
    }

    @Test
    void rechazaLoQueBCryptTruncariaPorSuperarSetentaYDosBytes() {
        String multibyte = "Aa1!" + "ñ".repeat(40);
        assertThat(PoliticaPassword.validar(multibyte)).hasSize(1)
            .first().asString().contains("72 bytes");
    }

    @Test
    void exigeMayuscula() {
        assertThat(PoliticaPassword.validar("segura#2026")).containsExactly("Debe incluir al menos una letra mayúscula");
    }

    @Test
    void exigeNumero() {
        assertThat(PoliticaPassword.validar("Segura#Clave")).containsExactly("Debe incluir al menos un número");
    }

    @Test
    void exigeCaracterEspecialYNoCuentaElEspacio() {
        assertThat(PoliticaPassword.validar("Segura2026")).containsExactly("Debe incluir al menos un carácter especial");
        assertThat(PoliticaPassword.validar("Segura 2026")).containsExactly("Debe incluir al menos un carácter especial");
    }

    @Test
    void informaTodasLasReglasIncumplidasAlMismoTiempo() {
        assertThat(PoliticaPassword.validar("abc")).hasSize(4);
    }

    @Test
    void rechazaNulo() {
        assertThat(PoliticaPassword.validar(null)).containsExactly("La contraseña es obligatoria");
    }
}
