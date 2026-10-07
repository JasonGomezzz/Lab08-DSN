package com.techstore.inventario.autenticacion;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.techstore.inventario.comun.ConflictoEstadoException;
import com.techstore.inventario.comun.DatosInvalidosException;
import com.techstore.inventario.tiendas.Tienda;
import com.techstore.inventario.tiendas.TiendaRepository;
import com.techstore.inventario.usuarios.ProveedorIdentidad;
import com.techstore.inventario.usuarios.Rol;
import com.techstore.inventario.usuarios.Usuario;
import com.techstore.inventario.usuarios.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioRegistro {
    private final UsuarioRepository usuarios;
    private final TiendaRepository tiendas;
    private final PasswordEncoder codificador;
    private final Clock reloj;

    public ServicioRegistro(UsuarioRepository usuarios, TiendaRepository tiendas, PasswordEncoder codificador,
                            Clock reloj) {
        this.usuarios = usuarios;
        this.tiendas = tiendas;
        this.codificador = codificador;
        this.reloj = reloj;
    }

    /** Todo registro público nace con el rol de menor privilegio; los demás roles los asigna un administrador. */
    @Transactional
    public Usuario registrar(SolicitudRegistro solicitud) {
        Map<String, List<String>> errores = new LinkedHashMap<>();
        List<String> faltasPassword = PoliticaPassword.validar(solicitud.password());
        if (!faltasPassword.isEmpty()) {
            errores.put("password", new ArrayList<>(faltasPassword));
        }
        Tienda tienda = tiendas.findById(solicitud.tiendaId()).orElse(null);
        if (tienda == null) {
            errores.put("tiendaId", List.of("La tienda indicada no existe"));
        }
        if (!errores.isEmpty()) {
            throw new DatosInvalidosException(errores);
        }
        String email = solicitud.email().toLowerCase(Locale.ROOT);
        if (usuarios.existsByEmail(email)) {
            throw new ConflictoEstadoException("EMAIL_EXISTENTE", "Ya existe una cuenta con ese correo");
        }
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setPasswordHash(codificador.encode(solicitud.password()));
        usuario.setNombreCompleto(solicitud.nombreCompleto().trim());
        usuario.setTienda(tienda);
        usuario.setRol(Rol.EMPLEADO_VENTAS);
        usuario.setProveedor(ProveedorIdentidad.LOCAL);
        usuario.setCreadoEn(reloj.instant());
        return usuarios.save(usuario);
    }
}
