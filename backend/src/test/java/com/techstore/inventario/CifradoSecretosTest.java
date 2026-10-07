package com.techstore.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.techstore.inventario.autenticacion.CifradoSecretos;
import com.techstore.inventario.comun.PropiedadesTechStore;
import org.junit.jupiter.api.Test;

class CifradoSecretosTest {
    private static final String CLAVE = "clave_de_cifrado_de_prueba_con_mas_de_32_bytes";

    private CifradoSecretos con(String clave) {
        return new CifradoSecretos(new PropiedadesTechStore(null, null, new PropiedadesTechStore.Mfa(clave, "TechStore"), null, null, null, null));
    }

    @Test
    void descifraLoQueCifro() {
        CifradoSecretos cifrado = con(CLAVE);

        String almacenado = cifrado.cifrar("JBSWY3DPEHPK3PXP", "usuario:7");

        assertThat(almacenado).startsWith("v1:").doesNotContain("JBSWY3DPEHPK3PXP");
        assertThat(cifrado.descifrar(almacenado, "usuario:7")).isEqualTo("JBSWY3DPEHPK3PXP");
    }

    @Test
    void elMismoTextoSeCifraDistintoCadaVezPorElIvAleatorio() {
        CifradoSecretos cifrado = con(CLAVE);

        assertThat(cifrado.cifrar("secreto", "usuario:7")).isNotEqualTo(cifrado.cifrar("secreto", "usuario:7"));
    }

    @Test
    void unSecretoNoSePuedeUsarEnOtroUsuarioPorElDatoAsociado() {
        CifradoSecretos cifrado = con(CLAVE);
        String deUsuarioSiete = cifrado.cifrar("secreto", "usuario:7");

        assertThatThrownBy(() -> cifrado.descifrar(deUsuarioSiete, "usuario:8"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unaClaveDistintaNoPuedeDescifrar() {
        String almacenado = con(CLAVE).cifrar("secreto", "usuario:7");

        assertThatThrownBy(() -> con("otra_clave_totalmente_distinta_con_mas_de_32_bytes").descifrar(almacenado, "usuario:7"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void detectaUnTextoCifradoAlterado() {
        CifradoSecretos cifrado = con(CLAVE);
        String almacenado = cifrado.cifrar("secreto", "usuario:7");
        String alterado = almacenado.substring(0, almacenado.length() - 4) + "AAAA";

        assertThatThrownBy(() -> cifrado.descifrar(alterado, "usuario:7")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rechazaFormatosDesconocidos() {
        assertThatThrownBy(() -> con(CLAVE).descifrar("texto-plano", "usuario:7"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rechazaLaClaveDeEjemploQueTraeElEnvExample() {
        assertThatThrownBy(() -> con("CAMBIAR_POR_OTRO_SECRETO_ALEATORIO_DE_32_BYTES_O_MAS"))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("valor de ejemplo");
    }

    @Test
    void exigeUnaClaveDeAlMenos32Bytes() {
        assertThatThrownBy(() -> con("corta")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> con(null)).isInstanceOf(IllegalStateException.class);
    }
}
