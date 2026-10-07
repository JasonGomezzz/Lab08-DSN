package com.techstore.inventario;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import com.techstore.inventario.autenticacion.DesafioMfa;
import com.techstore.inventario.autenticacion.DesafioMfaRepository;
import com.techstore.inventario.autenticacion.LimpiezaSesiones;
import com.techstore.inventario.autenticacion.TokenRevocado;
import com.techstore.inventario.autenticacion.TokenRevocadoRepository;
import com.techstore.inventario.usuarios.Rol;
import com.techstore.inventario.usuarios.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LimpiezaSesionesTest extends PruebaIntegracion {
    @Autowired LimpiezaSesiones limpieza;
    @Autowired TokenRevocadoRepository tokens;
    @Autowired DesafioMfaRepository desafios;
    @Autowired FabricaUsuarios fabrica;
    @Autowired Clock reloj;

    @Test
    void laLimpiezaBorraLoVencidoYConservaLoVigente() {
        Instant ahora = reloj.instant();
        Usuario usuario = fabrica.crear(Rol.EMPLEADO_VENTAS, fabrica.tienda("LIM-01"));
        TokenRevocado viejo = token(ahora.minus(Duration.ofHours(2)));
        TokenRevocado vigente = token(ahora.plus(Duration.ofMinutes(30)));
        DesafioMfa desafioViejo = desafio(usuario, ahora.minus(Duration.ofHours(2)));
        DesafioMfa desafioVigente = desafio(usuario, ahora.plus(Duration.ofMinutes(4)));

        limpieza.limpiar();

        assertThat(tokens.existsById(viejo.getJti())).isFalse();
        assertThat(tokens.existsById(vigente.getJti())).isTrue();
        assertThat(desafios.existsById(desafioViejo.getId())).isFalse();
        assertThat(desafios.existsById(desafioVigente.getId())).isTrue();
    }

    private TokenRevocado token(Instant expira) {
        TokenRevocado token = new TokenRevocado();
        token.setJti(UUID.randomUUID().toString());
        token.setExpiraEn(expira);
        return tokens.save(token);
    }

    private DesafioMfa desafio(Usuario usuario, Instant expira) {
        DesafioMfa desafio = new DesafioMfa();
        desafio.setId(UUID.randomUUID().toString());
        desafio.setUsuarioId(usuario.getId());
        desafio.setCreadoEn(reloj.instant());
        desafio.setExpiraEn(expira);
        return desafios.save(desafio);
    }
}
