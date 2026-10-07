package com.techstore.inventario;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Reloj que sigue el tiempo real pero permite adelantarlo para probar expiraciones y bloqueos. */
public class RelojDePrueba extends Clock {
    private volatile Duration desfase = Duration.ZERO;

    public void avanzar(Duration duracion) {
        desfase = desfase.plus(duracion);
    }

    public void restablecer() {
        desfase = Duration.ZERO;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return this;
    }

    @Override
    public Instant instant() {
        return Instant.now().plus(desfase);
    }
}
