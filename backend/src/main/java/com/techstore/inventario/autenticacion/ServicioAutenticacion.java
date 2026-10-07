package com.techstore.inventario.autenticacion;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import com.techstore.inventario.usuarios.ProveedorIdentidad;
import com.techstore.inventario.usuarios.Usuario;
import com.techstore.inventario.usuarios.UsuarioDto;
import com.techstore.inventario.usuarios.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flujo de dos factores: la contraseña correcta (o el login social) solo entrega un token MFA
 * parcial; el token de acceso completo se emite únicamente cuando el código TOTP es correcto.
 */
@Service
public class ServicioAutenticacion {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;
    private final ServicioJwt jwt;
    private final ServicioMfa mfa;
    private final ControlBloqueo bloqueo;
    private final Clock reloj;
    /** Hash descartable para gastar el mismo tiempo cuando el correo no existe y no delatar cuentas. */
    private final String hashSenuelo;

    public ServicioAutenticacion(UsuarioRepository usuarios, PasswordEncoder codificador, ServicioJwt jwt,
                                 ServicioMfa mfa, ControlBloqueo bloqueo, Clock reloj) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.jwt = jwt;
        this.mfa = mfa;
        this.bloqueo = bloqueo;
        this.reloj = reloj;
        this.hashSenuelo = codificador.encode("senuelo-" + System.nanoTime());
    }

    /** Primer factor. Los fallos se persisten aunque se lance la excepción, por eso no hay rollback. */
    @Transactional(noRollbackFor = AutenticacionException.class)
    public ResultadoLogin ingresar(String email, String password) {
        Usuario usuario = usuarios.buscarParaAutenticar(email.toLowerCase(Locale.ROOT)).orElse(null);
        if (usuario == null) {
            codificador.matches(password, hashSenuelo);
            throw credencialesInvalidas();
        }
        if (usuario.estaBloqueado(reloj.instant())) {
            throw bloqueo.error(usuario);
        }
        if (usuario.getProveedor() != ProveedorIdentidad.LOCAL || usuario.getPasswordHash() == null) {
            // Las cuentas sociales no tienen contraseña: no se cuentan fallos para que nadie pueda
            // bloquear desde fuera a quien entra con Google o GitHub.
            codificador.matches(password, hashSenuelo);
            throw credencialesInvalidas();
        }
        if (!codificador.matches(password, usuario.getPasswordHash())) {
            if (bloqueo.registrarFallo(usuario)) {
                throw bloqueo.error(usuario);
            }
            throw credencialesInvalidas();
        }
        // El contador no se reinicia aquí sino al completar el segundo factor; si no, quien conozca la
        // contraseña podría alternar login correcto y códigos erróneos sin llegar nunca al bloqueo.
        return iniciarSegundoFactor(usuario);
    }

    /** Punto común del login con contraseña y del login social. */
    @Transactional
    public ResultadoLogin iniciarSegundoFactor(Usuario usuario) {
        if (usuario.estaBloqueado(reloj.instant())) {
            throw bloqueo.error(usuario);
        }
        DesafioMfa desafio = mfa.crearDesafio(usuario);
        ServicioJwt.Emitido token = jwt.emitirMfa(usuario.getId(), desafio.getId());
        String estado = usuario.isMfaHabilitado() ? ResultadoLogin.MFA_REQUERIDO : ResultadoLogin.MFA_ENROLAMIENTO;
        return new ResultadoLogin(estado, token.token(), segundosHasta(token));
    }

    @Transactional
    public DatosEnrolamiento enrolar(Usuario usuario, ServicioJwt.DatosToken sesion) {
        return mfa.iniciarEnrolamiento(usuario, sesion.jti());
    }

    @Transactional(noRollbackFor = AutenticacionException.class)
    public SesionIniciada verificarMfa(Usuario usuario, ServicioJwt.DatosToken sesion, String codigo) {
        Usuario verificado = mfa.verificar(usuario, sesion.jti(), codigo);
        ServicioJwt.Emitido acceso = jwt.emitirAcceso(verificado.getId());
        return new SesionIniciada(acceso.token(), "Bearer", segundosHasta(acceso), UsuarioDto.de(verificado));
    }

    public void cerrarSesion(ServicioJwt.DatosToken sesion) {
        jwt.revocar(sesion);
    }

    private long segundosHasta(ServicioJwt.Emitido token) {
        return Duration.between(reloj.instant(), token.expiraEn()).getSeconds();
    }

    private static AutenticacionException credencialesInvalidas() {
        return new AutenticacionException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS",
            "Correo o contraseña incorrectos");
    }
}
