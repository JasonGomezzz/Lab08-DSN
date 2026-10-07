package com.techstore.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import com.techstore.inventario.autenticacion.ServicioJwt;
import com.techstore.inventario.autenticacion.ServicioJwt.DatosToken;
import com.techstore.inventario.autenticacion.ServicioJwt.Tipo;
import com.techstore.inventario.autenticacion.TokenInvalidoException;
import com.techstore.inventario.autenticacion.TokenRevocadoRepository;
import com.techstore.inventario.comun.PropiedadesTechStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicioJwtTest {
    private static final String SECRETO = "secreto_de_prueba_con_mas_de_32_bytes_01234567";

    private RelojDePrueba reloj;
    private TokenRevocadoRepository revocados;
    private ServicioJwt jwt;

    @BeforeEach
    void preparar() {
        reloj = new RelojDePrueba();
        revocados = mock(TokenRevocadoRepository.class);
        jwt = nuevoServicio(SECRETO);
    }

    private ServicioJwt nuevoServicio(String secreto) {
        PropiedadesTechStore propiedades = new PropiedadesTechStore(
            new PropiedadesTechStore.Jwt(secreto, 60, 5), null, null, null, null, null, null);
        return new ServicioJwt(propiedades, reloj, revocados);
    }

    @Test
    void exigeUnSecretoDeAlMenos32Bytes() {
        assertThatThrownBy(() -> nuevoServicio("corto")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> nuevoServicio("")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> nuevoServicio(null)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rechazaElSecretoDeEjemploQueTraeElEnvExample() {
        assertThatThrownBy(() -> nuevoServicio("CAMBIAR_POR_UN_SECRETO_ALEATORIO_DE_32_BYTES_O_MAS"))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("valor de ejemplo");
    }

    @Test
    void elTokenDeAccesoSeEmiteYSeVerificaConUsuarioYTipo() {
        ServicioJwt.Emitido emitido = jwt.emitirAcceso(42L);

        DatosToken datos = jwt.verificar(emitido.token());

        assertThat(datos.usuarioId()).isEqualTo(42L);
        assertThat(datos.tipo()).isEqualTo(Tipo.ACCESO);
        assertThat(datos.jti()).isEqualTo(emitido.jti());
        assertThat(emitido.expiraEn()).isAfter(reloj.instant().plus(Duration.ofMinutes(59)));
    }

    @Test
    void elTokenMfaUsaElIdDelDesafioComoJtiYDuraCincoMinutos() {
        String desafio = UUID.randomUUID().toString();

        ServicioJwt.Emitido emitido = jwt.emitirMfa(7L, desafio);

        DatosToken datos = jwt.verificar(emitido.token());
        assertThat(datos.tipo()).isEqualTo(Tipo.MFA);
        assertThat(datos.jti()).isEqualTo(desafio);
        assertThat(emitido.expiraEn()).isBefore(reloj.instant().plus(Duration.ofMinutes(6)));
    }

    @Test
    void rechazaUnTokenExpirado() {
        String token = jwt.emitirMfa(7L, UUID.randomUUID().toString()).token();
        reloj.avanzar(Duration.ofMinutes(6));

        assertThatThrownBy(() -> jwt.verificar(token))
            .isInstanceOf(TokenInvalidoException.class).hasMessage("TOKEN_EXPIRADO");
    }

    @Test
    void rechazaUnTokenConLaFirmaAlterada() {
        String token = jwt.emitirAcceso(1L).token();
        String alterado = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");

        assertThatThrownBy(() -> jwt.verificar(alterado)).isInstanceOf(TokenInvalidoException.class);
    }

    @Test
    void rechazaUnTokenFirmadoConOtroSecreto() {
        String ajeno = nuevoServicio("otro_secreto_distinto_con_mas_de_32_bytes_9876").emitirAcceso(1L).token();

        assertThatThrownBy(() -> jwt.verificar(ajeno))
            .isInstanceOf(TokenInvalidoException.class).hasMessage("TOKEN_INVALIDO");
    }

    @Test
    void rechazaUnTokenSinFirmaAlgNone() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder().subject("1").jwtID("x").claim("tipo", "ACCESO")
            .issueTime(new Date()).expirationTime(new Date(System.currentTimeMillis() + 60_000)).build();

        assertThatThrownBy(() -> jwt.verificar(new PlainJWT(claims).serialize()))
            .isInstanceOf(TokenInvalidoException.class);
    }

    @Test
    void rechazaUnTokenFirmadoSinElClaimDeTipo() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder().subject("1").jwtID("x")
            .issueTime(new Date()).expirationTime(new Date(System.currentTimeMillis() + 60_000)).build();
        SignedJWT firmado = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        firmado.sign(new MACSigner(SECRETO.getBytes()));

        assertThatThrownBy(() -> jwt.verificar(firmado.serialize())).isInstanceOf(TokenInvalidoException.class);
    }

    @Test
    void rechazaTextoQueNoEsUnJwt() {
        assertThatThrownBy(() -> jwt.verificar("no-es-un-jwt")).isInstanceOf(TokenInvalidoException.class);
    }

    @Test
    void rechazaUnTokenDeAccesoRevocado() {
        ServicioJwt.Emitido emitido = jwt.emitirAcceso(1L);
        when(revocados.existsById(emitido.jti())).thenReturn(true);

        assertThatThrownBy(() -> jwt.verificar(emitido.token()))
            .isInstanceOf(TokenInvalidoException.class).hasMessage("TOKEN_REVOCADO");
    }
}
