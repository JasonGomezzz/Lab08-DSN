package com.techstore.inventario;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.OptionalLong;
import com.techstore.inventario.autenticacion.Totp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TotpTest {
    /** Secreto ASCII "12345678901234567890" del Apéndice B del RFC 6238, en Base32. */
    private static final String SECRETO_RFC = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    /** Vectores SHA-1 del RFC 6238; el RFC da 8 dígitos y aquí se usan los 6 últimos. */
    @ParameterizedTest
    @CsvSource({"59,287082", "1111111109,081804", "1111111111,050471", "1234567890,005924", "2000000000,279037",
        "20000000000,353130"})
    void coincideConLosVectoresOficialesDelRfc6238(long segundos, String esperado) {
        assertThat(Totp.codigo(SECRETO_RFC, Totp.pasoDe(Instant.ofEpochSecond(segundos)))).isEqualTo(esperado);
    }

    @Test
    void elSecretoDelRfcEsElBase32DeSuTextoAscii() {
        assertThat(Totp.codificarBase32("12345678901234567890".getBytes(StandardCharsets.US_ASCII)))
            .isEqualTo(SECRETO_RFC);
        assertThat(Totp.decodificarBase32(SECRETO_RFC)).isEqualTo("12345678901234567890".getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    void generaSecretosAleatoriosDe32CaracteresBase32() {
        String uno = Totp.generarSecreto();
        String otro = Totp.generarSecreto();

        assertThat(uno).matches("[A-Z2-7]{32}").isNotEqualTo(otro);
    }

    @Test
    void aceptaElCodigoDelPasoActualYDevuelveEsePaso() {
        Instant ahora = Instant.ofEpochSecond(1_700_000_000L);
        long paso = Totp.pasoDe(ahora);

        OptionalLong resultado = Totp.verificar(SECRETO_RFC, Totp.codigo(SECRETO_RFC, paso), ahora, 1, null);

        assertThat(resultado).hasValue(paso);
    }

    @Test
    void toleraUnPasoDeDesfaseHaciaCadaLadoPeroNoDos() {
        Instant ahora = Instant.ofEpochSecond(1_700_000_000L);
        long paso = Totp.pasoDe(ahora);

        assertThat(Totp.verificar(SECRETO_RFC, Totp.codigo(SECRETO_RFC, paso - 1), ahora, 1, null)).hasValue(paso - 1);
        assertThat(Totp.verificar(SECRETO_RFC, Totp.codigo(SECRETO_RFC, paso + 1), ahora, 1, null)).hasValue(paso + 1);
        assertThat(Totp.verificar(SECRETO_RFC, Totp.codigo(SECRETO_RFC, paso - 2), ahora, 1, null)).isEmpty();
        assertThat(Totp.verificar(SECRETO_RFC, Totp.codigo(SECRETO_RFC, paso + 2), ahora, 1, null)).isEmpty();
    }

    @Test
    void noPermiteReutilizarUnCodigoYaAceptado() {
        Instant ahora = Instant.ofEpochSecond(1_700_000_000L);
        long paso = Totp.pasoDe(ahora);
        String codigo = Totp.codigo(SECRETO_RFC, paso);

        assertThat(Totp.verificar(SECRETO_RFC, codigo, ahora, 1, paso)).isEmpty();
        assertThat(Totp.verificar(SECRETO_RFC, codigo, ahora, 1, paso - 1)).hasValue(paso);
    }

    @ParameterizedTest
    @CsvSource(value = {"12345", "1234567", "abcdef", "12 456", "NULL"}, nullValues = "NULL")
    void rechazaCodigosQueNoSonSeisDigitos(String ingresado) {
        assertThat(Totp.verificar(SECRETO_RFC, ingresado, Instant.ofEpochSecond(59), 1, null)).isEmpty();
    }

    @Test
    void rechazaUnCodigoDeOtroSecreto() {
        Instant ahora = Instant.ofEpochSecond(1_700_000_000L);
        String ajeno = Totp.codigo(Totp.generarSecreto(), Totp.pasoDe(ahora));

        assertThat(Totp.verificar(SECRETO_RFC, ajeno, ahora, 1, null)).isEmpty();
    }

    @Test
    void laUriDeEnrolamientoUsaElFormatoQueEntiendenLasAppsAutenticadoras() {
        String uri = Totp.uriOtpAuth("TechStore", "ana+ventas@techstore.test", SECRETO_RFC);

        assertThat(uri).isEqualTo("otpauth://totp/TechStore:ana%2Bventas%40techstore.test?secret=" + SECRETO_RFC
            + "&issuer=TechStore&algorithm=SHA1&digits=6&period=30");
    }
}
