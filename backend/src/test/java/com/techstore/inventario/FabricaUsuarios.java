package com.techstore.inventario;

import java.time.Clock;
import java.util.UUID;
import com.techstore.inventario.tiendas.Tienda;
import com.techstore.inventario.tiendas.TiendaRepository;
import com.techstore.inventario.usuarios.ProveedorIdentidad;
import com.techstore.inventario.usuarios.Rol;
import com.techstore.inventario.usuarios.Usuario;
import com.techstore.inventario.usuarios.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Crea usuarios con correo único para que las pruebas compartan base de datos sin chocar. */
@Component
public class FabricaUsuarios {
    public static final String PASSWORD = "Segura#2026";

    private final UsuarioRepository usuarios;
    private final TiendaRepository tiendas;
    private final PasswordEncoder codificador;
    private final Clock reloj;

    public FabricaUsuarios(UsuarioRepository usuarios, TiendaRepository tiendas, PasswordEncoder codificador,
                           Clock reloj) {
        this.usuarios = usuarios;
        this.tiendas = tiendas;
        this.codificador = codificador;
        this.reloj = reloj;
    }

    public Tienda tienda(String codigo) {
        return tiendas.findByCodigo(codigo).orElseThrow();
    }

    public Usuario crear(Rol rol, Tienda tienda) {
        return crear("u-" + UUID.randomUUID() + "@techstore.test", PASSWORD, rol, tienda);
    }

    public Usuario crear(String email, String password, Rol rol, Tienda tienda) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setPasswordHash(codificador.encode(password));
        usuario.setNombreCompleto("Usuario de prueba");
        usuario.setTienda(tienda);
        usuario.setRol(rol);
        usuario.setProveedor(ProveedorIdentidad.LOCAL);
        usuario.setCreadoEn(reloj.instant());
        return usuarios.save(usuario);
    }
}
