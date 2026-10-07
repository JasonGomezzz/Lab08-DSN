package com.techstore.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import com.techstore.inventario.autenticacion.IdentidadExterna;
import com.techstore.inventario.usuarios.ProveedorIdentidad;
import org.junit.jupiter.api.Test;

class IdentidadExternaTest {

    @Test
    void googleConCorreoVerificadoSeNormaliza() {
        IdentidadExterna id = IdentidadExterna.de("google", Map.of("sub", "1098", "email", "  Ana.Torres@Gmail.com ",
            "email_verified", true, "name", "Ana Torres"));

        assertThat(id.proveedor()).isEqualTo(ProveedorIdentidad.GOOGLE);
        assertThat(id.idExterno()).isEqualTo("1098");
        assertThat(id.email()).isEqualTo("ana.torres@gmail.com");
        assertThat(id.nombre()).isEqualTo("Ana Torres");
        assertThat(id.emailVerificado()).isTrue();
    }

    @Test
    void googleAceptaElVerificadoComoTextoYLoRechazaSiEsFalso() {
        assertThat(IdentidadExterna.de("google", Map.of("sub", "1", "email", "a@b.co", "email_verified", "true"))
            .emailVerificado()).isTrue();
        assertThat(IdentidadExterna.de("google", Map.of("sub", "1", "email", "a@b.co", "email_verified", false))
            .emailVerificado()).isFalse();
        assertThat(IdentidadExterna.de("google", Map.of("sub", "1", "email", "a@b.co")).emailVerificado()).isFalse();
    }

    @Test
    void githubUsaElIdNumericoYSoloConfiaEnElCorreoInyectadoComoVerificado() {
        Map<String, Object> atributos = new HashMap<>();
        atributos.put("id", 583231);
        atributos.put("login", "anatorres");
        atributos.put("email", "ana@correo.com");
        atributos.put("email_verified", true);

        IdentidadExterna id = IdentidadExterna.de("github", atributos);

        assertThat(id.proveedor()).isEqualTo(ProveedorIdentidad.GITHUB);
        assertThat(id.idExterno()).isEqualTo("583231");
        assertThat(id.nombre()).isEqualTo("anatorres");
        assertThat(id.emailVerificado()).isTrue();
    }

    @Test
    void githubSinCorreoVerificadoNoQuedaVerificado() {
        IdentidadExterna sinCorreo = IdentidadExterna.de("github", Map.of("id", 1, "login", "x"));
        IdentidadExterna sinMarca = IdentidadExterna.de("github", Map.of("id", 1, "login", "x", "email", "x@y.co"));

        assertThat(sinCorreo.email()).isNull();
        assertThat(sinCorreo.emailVerificado()).isFalse();
        assertThat(sinMarca.emailVerificado()).isFalse();
    }

    @Test
    void sinNombreUsaLaParteLocalDelCorreo() {
        IdentidadExterna id = IdentidadExterna.de("google", Map.of("sub", "1", "email", "pedro@x.co",
            "email_verified", true));

        assertThat(id.nombre()).isEqualTo("pedro");
    }

    @Test
    void recortaNombresMuyLargos() {
        IdentidadExterna id = IdentidadExterna.de("google", Map.of("sub", "1", "name", "N".repeat(300)));

        assertThat(id.nombre()).hasSize(120);
    }

    @Test
    void rechazaProveedoresDesconocidosYRespuestasSinIdentificador() {
        assertThatThrownBy(() -> IdentidadExterna.de("facebook", Map.of("id", "1")))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IdentidadExterna.de("google", Map.of("email", "a@b.co")))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
