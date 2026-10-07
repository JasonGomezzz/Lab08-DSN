package com.techstore.inventario.autenticacion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Borra los tokens revocados y los desafíos MFA que ya vencieron para que las tablas no crezcan sin límite. */
@Component
public class LimpiezaSesiones {
    private static final Logger log = LoggerFactory.getLogger(LimpiezaSesiones.class);

    private final TokenRevocadoRepository tokens;
    private final DesafioMfaRepository desafios;
    private final Clock reloj;

    public LimpiezaSesiones(TokenRevocadoRepository tokens, DesafioMfaRepository desafios, Clock reloj) {
        this.tokens = tokens;
        this.desafios = desafios;
        this.reloj = reloj;
    }

    @Scheduled(initialDelay = 60_000, fixedRate = 3_600_000)
    @Transactional
    public void limpiar() {
        Instant limite = reloj.instant().minus(Duration.ofMinutes(5));
        int eliminadosTokens = tokens.eliminarExpirados(limite);
        int eliminadosDesafios = desafios.eliminarExpirados(limite);
        if (eliminadosTokens + eliminadosDesafios > 0) {
            log.info("Limpieza de sesiones: {} tokens revocados y {} desafíos MFA vencidos", eliminadosTokens,
                eliminadosDesafios);
        }
    }
}
