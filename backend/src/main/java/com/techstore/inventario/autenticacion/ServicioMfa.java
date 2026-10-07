package com.techstore.inventario.autenticacion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import com.techstore.inventario.comun.ConflictoEstadoException;
import com.techstore.inventario.comun.PropiedadesTechStore;
import com.techstore.inventario.usuarios.Usuario;
import com.techstore.inventario.usuarios.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Segundo factor TOTP. Cada desafío vive 5 minutos, admite 3 intentos y se consume al acertar;
 * agotarlo cuenta como un fallo de inicio de sesión para que no se pueda forzar el código
 * repitiendo el login.
 */
@Service
public class ServicioMfa {
    private static final int VENTANA_PASOS = 1;

    private final DesafioMfaRepository desafios;
    private final UsuarioRepository usuarios;
    private final CifradoSecretos cifrado;
    private final ControlBloqueo bloqueo;
    private final Clock reloj;
    private final Duration vigencia;
    private final int intentosMaximos;
    private final String emisor;

    public ServicioMfa(DesafioMfaRepository desafios, UsuarioRepository usuarios, CifradoSecretos cifrado,
                       ControlBloqueo bloqueo, Clock reloj, PropiedadesTechStore propiedades) {
        this.desafios = desafios;
        this.usuarios = usuarios;
        this.cifrado = cifrado;
        this.bloqueo = bloqueo;
        this.reloj = reloj;
        this.vigencia = Duration.ofMinutes(propiedades.jwt().expiracionMfaMinutos());
        this.intentosMaximos = propiedades.seguridad().intentosMaximosMfa();
        this.emisor = propiedades.mfa().emisor();
    }

    /** Crea el desafío del usuario e invalida los anteriores para que no se acumulen intentos. */
    @Transactional
    public DesafioMfa crearDesafio(Usuario usuario) {
        desafios.consumirPendientes(usuario.getId());
        Instant ahora = reloj.instant();
        DesafioMfa desafio = new DesafioMfa();
        desafio.setId(UUID.randomUUID().toString());
        desafio.setUsuarioId(usuario.getId());
        desafio.setCreadoEn(ahora);
        desafio.setExpiraEn(ahora.plus(vigencia));
        return desafios.save(desafio);
    }

    /** Genera un secreto nuevo; solo se permite mientras el usuario no haya activado MFA. */
    @Transactional
    public DatosEnrolamiento iniciarEnrolamiento(Usuario usuario, String desafioId) {
        desafioActivo(desafios.buscarParaActualizar(desafioId).orElse(null), usuario.getId());
        Usuario bloqueado = usuarios.bloquearPorId(usuario.getId()).orElseThrow();
        if (bloqueado.isMfaHabilitado()) {
            throw new ConflictoEstadoException("MFA_YA_CONFIGURADO",
                "El segundo factor ya está activado; pide a un administrador que lo reinicie si perdiste el dispositivo");
        }
        String secreto = Totp.generarSecreto();
        bloqueado.setMfaSecreto(cifrado.cifrar(secreto, asociado(bloqueado)));
        bloqueado.setMfaUltimoPaso(null);
        return new DatosEnrolamiento(secreto, Totp.uriOtpAuth(emisor, bloqueado.getEmail(), secreto));
    }

    /** Valida el código; si acierta consume el desafío y activa MFA, si falla suma un intento. */
    @Transactional(noRollbackFor = AutenticacionException.class)
    public Usuario verificar(Usuario usuario, String desafioId, String codigo) {
        DesafioMfa desafio = desafios.buscarParaActualizar(desafioId).orElse(null);
        desafioActivo(desafio, usuario.getId());
        Usuario bloqueado = usuarios.bloquearPorId(usuario.getId()).orElseThrow();
        if (bloqueado.getMfaSecreto() == null) {
            throw new ConflictoEstadoException("MFA_NO_ENROLADO", "Primero debes registrar la app autenticadora");
        }
        String secreto = cifrado.descifrar(bloqueado.getMfaSecreto(), asociado(bloqueado));
        OptionalLong paso = Totp.verificar(secreto, codigo, reloj.instant(), VENTANA_PASOS,
            bloqueado.getMfaUltimoPaso());
        if (paso.isEmpty()) {
            fallar(desafio, bloqueado);
        }
        desafio.setConsumido(true);
        bloqueado.setMfaHabilitado(true);
        bloqueado.setMfaUltimoPaso(paso.getAsLong());
        bloqueo.limpiar(bloqueado);
        return bloqueado;
    }

    private void fallar(DesafioMfa desafio, Usuario usuario) {
        desafio.setIntentos(desafio.getIntentos() + 1);
        int restantes = intentosMaximos - desafio.getIntentos();
        if (restantes > 0) {
            throw new AutenticacionException(HttpStatus.UNAUTHORIZED, "MFA_CODIGO_INVALIDO",
                "El código es incorrecto. Te quedan " + restantes + (restantes == 1 ? " intento" : " intentos"),
                Map.of("intentosRestantes", restantes));
        }
        desafio.setConsumido(true);
        if (bloqueo.registrarFallo(usuario)) {
            throw bloqueo.error(usuario);
        }
        throw new AutenticacionException(HttpStatus.UNAUTHORIZED, "MFA_INTENTOS_AGOTADOS",
            "Superaste los " + intentosMaximos + " intentos. Inicia sesión de nuevo", Map.of("intentosRestantes", 0));
    }

    private void desafioActivo(DesafioMfa desafio, Long usuarioId) {
        if (desafio == null || !desafio.getUsuarioId().equals(usuarioId) || desafio.isConsumido()
            || !desafio.getExpiraEn().isAfter(reloj.instant())) {
            throw new AutenticacionException(HttpStatus.UNAUTHORIZED, "MFA_DESAFIO_INVALIDO",
                "La verificación expiró o ya fue usada. Inicia sesión de nuevo");
        }
    }

    private static String asociado(Usuario usuario) {
        return "usuario:" + usuario.getId();
    }
}
