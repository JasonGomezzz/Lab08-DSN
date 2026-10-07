package com.techstore.inventario.autenticacion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import com.techstore.inventario.comun.PropiedadesTechStore;
import com.techstore.inventario.usuarios.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Regla de bloqueo del enunciado: 5 fallos seguidos bloquean la cuenta un tiempo fijo. */
@Component
public class ControlBloqueo {
    private final Clock reloj;
    private final int intentosMaximos;
    private final Duration duracion;

    public ControlBloqueo(Clock reloj, PropiedadesTechStore propiedades) {
        this.reloj = reloj;
        this.intentosMaximos = propiedades.seguridad().intentosMaximosLogin();
        this.duracion = Duration.ofMinutes(propiedades.seguridad().bloqueoMinutos());
    }

    /** Suma un fallo y devuelve true si este fallo bloqueó la cuenta. */
    public boolean registrarFallo(Usuario usuario) {
        usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
        if (usuario.getIntentosFallidos() >= intentosMaximos) {
            usuario.setBloqueadoHasta(reloj.instant().plus(duracion));
            usuario.setIntentosFallidos(0);
            return true;
        }
        return false;
    }

    public void limpiar(Usuario usuario) {
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
    }

    public AutenticacionException error(Usuario usuario) {
        Instant hasta = usuario.getBloqueadoHasta();
        long minutos = Math.max(1, Duration.between(reloj.instant(), hasta).plusSeconds(59).toMinutes());
        return new AutenticacionException(HttpStatus.LOCKED, "CUENTA_BLOQUEADA",
            "La cuenta está bloqueada por demasiados intentos fallidos. Intenta de nuevo en " + minutos
                + (minutos == 1 ? " minuto" : " minutos"),
            Map.of("bloqueadoHasta", hasta.toString(), "minutosRestantes", minutos));
    }
}
