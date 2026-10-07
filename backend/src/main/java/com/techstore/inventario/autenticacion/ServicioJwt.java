package com.techstore.inventario.autenticacion;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.techstore.inventario.comun.PropiedadesTechStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emite y verifica los dos tokens del flujo: el token MFA parcial (solo sirve para completar el
 * segundo factor) y el token de acceso completo que se entrega cuando el segundo factor es correcto.
 */
@Service
public class ServicioJwt {
    private static final String CLAIM_TIPO = "tipo";

    public enum Tipo { ACCESO, MFA }

    public record Emitido(String token, String jti, Instant expiraEn) {
    }

    public record DatosToken(Long usuarioId, String jti, Tipo tipo, Instant expiraEn) {
    }

    private final byte[] clave;
    private final Clock reloj;
    private final TokenRevocadoRepository revocados;
    private final int minutosAcceso;
    private final int minutosMfa;

    public ServicioJwt(PropiedadesTechStore propiedades, Clock reloj, TokenRevocadoRepository revocados) {
        String secreto = propiedades.jwt().secreto();
        this.clave = secreto == null ? new byte[0] : secreto.getBytes(StandardCharsets.UTF_8);
        if (clave.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 bytes UTF-8");
        }
        this.reloj = reloj;
        this.revocados = revocados;
        this.minutosAcceso = propiedades.jwt().expiracionMinutos();
        this.minutosMfa = propiedades.jwt().expiracionMfaMinutos();
    }

    public Emitido emitirAcceso(Long usuarioId) {
        return emitir(usuarioId, UUID.randomUUID().toString(), Tipo.ACCESO, minutosAcceso);
    }

    /** El jti del token MFA es el identificador del desafío que lo respalda en la base de datos. */
    public Emitido emitirMfa(Long usuarioId, String desafioId) {
        return emitir(usuarioId, desafioId, Tipo.MFA, minutosMfa);
    }

    private Emitido emitir(Long usuarioId, String jti, Tipo tipo, int minutos) {
        Instant ahora = reloj.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expira = ahora.plus(minutos, ChronoUnit.MINUTES);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .subject(usuarioId.toString())
            .jwtID(jti)
            .claim(CLAIM_TIPO, tipo.name())
            .issueTime(Date.from(ahora))
            .expirationTime(Date.from(expira))
            .build();
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(clave));
            return new Emitido(jwt.serialize(), jti, expira);
        } catch (JOSEException ex) {
            throw new IllegalStateException("No se pudo emitir el token", ex);
        }
    }

    public DatosToken verificar(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm()) || !jwt.verify(new MACVerifier(clave))) {
                throw new TokenInvalidoException("TOKEN_INVALIDO");
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date vencimiento = claims.getExpirationTime();
            Date emision = claims.getIssueTime();
            String jti = claims.getJWTID();
            String tipoTexto = claims.getStringClaim(CLAIM_TIPO);
            if (vencimiento == null || emision == null || jti == null || jti.isBlank() || tipoTexto == null) {
                throw new TokenInvalidoException("TOKEN_INVALIDO");
            }
            Instant ahora = reloj.instant();
            if (!vencimiento.toInstant().isAfter(ahora)) {
                throw new TokenInvalidoException("TOKEN_EXPIRADO");
            }
            if (emision.toInstant().isAfter(ahora.plusSeconds(5))) {
                throw new TokenInvalidoException("TOKEN_INVALIDO");
            }
            Tipo tipo = Tipo.valueOf(tipoTexto);
            if (tipo == Tipo.ACCESO && revocados.existsById(jti)) {
                throw new TokenInvalidoException("TOKEN_REVOCADO");
            }
            return new DatosToken(Long.valueOf(claims.getSubject()), jti, tipo, vencimiento.toInstant());
        } catch (ParseException | JOSEException | IllegalArgumentException | NullPointerException ex) {
            throw new TokenInvalidoException("TOKEN_INVALIDO");
        }
    }

    @Transactional
    public void revocar(DatosToken datos) {
        TokenRevocado revocado = new TokenRevocado();
        revocado.setJti(datos.jti());
        revocado.setExpiraEn(datos.expiraEn());
        revocados.save(revocado);
    }
}
