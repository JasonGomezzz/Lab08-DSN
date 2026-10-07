package com.techstore.inventario.autenticacion;

import java.time.Clock;
import com.techstore.inventario.usuarios.Rol;
import com.techstore.inventario.usuarios.Usuario;
import com.techstore.inventario.usuarios.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea o reconoce la cuenta de quien entra con Google o GitHub y la lleva al segundo factor.
 * Nunca fusiona la cuenta social con una cuenta local del mismo correo: el registro con contraseña
 * no verifica el correo, así que fusionarlas permitiría que alguien se adueñe de una cuenta ajena.
 */
@Service
public class ServicioLoginSocial {
    private final UsuarioRepository usuarios;
    private final ServicioAutenticacion autenticacion;
    private final Clock reloj;

    public ServicioLoginSocial(UsuarioRepository usuarios, ServicioAutenticacion autenticacion, Clock reloj) {
        this.usuarios = usuarios;
        this.autenticacion = autenticacion;
        this.reloj = reloj;
    }

    @Transactional
    public ResultadoLogin ingresar(IdentidadExterna identidad) {
        Usuario usuario = usuarios.findByProveedorAndProveedorId(identidad.proveedor(), identidad.idExterno())
            .orElseGet(() -> crear(identidad));
        return autenticacion.iniciarSegundoFactor(usuario);
    }

    private Usuario crear(IdentidadExterna identidad) {
        if (!identidad.emailVerificado()) {
            throw new AutenticacionException(HttpStatus.FORBIDDEN, "EMAIL_NO_VERIFICADO",
                "El proveedor no entregó un correo verificado");
        }
        if (usuarios.existsByEmail(identidad.email())) {
            throw new AutenticacionException(HttpStatus.CONFLICT, "CUENTA_EXISTENTE",
                "Ya existe una cuenta con ese correo registrada de otra forma");
        }
        Usuario usuario = new Usuario();
        usuario.setEmail(identidad.email());
        usuario.setNombreCompleto(identidad.nombre());
        usuario.setRol(Rol.EMPLEADO_VENTAS);
        usuario.setProveedor(identidad.proveedor());
        usuario.setProveedorId(identidad.idExterno());
        usuario.setCreadoEn(reloj.instant());
        return usuarios.save(usuario);
    }
}
