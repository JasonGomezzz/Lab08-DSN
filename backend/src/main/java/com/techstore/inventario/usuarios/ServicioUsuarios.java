package com.techstore.inventario.usuarios;

import java.time.Clock;
import java.util.List;
import java.util.NoSuchElementException;
import com.techstore.inventario.autenticacion.ControlBloqueo;
import com.techstore.inventario.autenticacion.DesafioMfaRepository;
import com.techstore.inventario.autorizacion.Accion;
import com.techstore.inventario.autorizacion.PoliticaAcceso;
import com.techstore.inventario.comun.ConflictoEstadoException;
import com.techstore.inventario.comun.DatosInvalidosException;
import com.techstore.inventario.tiendas.Tienda;
import com.techstore.inventario.tiendas.TiendaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioUsuarios {
    private final UsuarioRepository usuarios;
    private final TiendaRepository tiendas;
    private final DesafioMfaRepository desafios;
    private final PoliticaAcceso politica;
    private final ControlBloqueo bloqueo;
    private final Clock reloj;

    public ServicioUsuarios(UsuarioRepository usuarios, TiendaRepository tiendas, DesafioMfaRepository desafios,
                            PoliticaAcceso politica, ControlBloqueo bloqueo, Clock reloj) {
        this.usuarios = usuarios;
        this.tiendas = tiendas;
        this.desafios = desafios;
        this.politica = politica;
        this.bloqueo = bloqueo;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<UsuarioAdminDto> listar(Usuario actor) {
        politica.exigir(actor, Accion.VER_USUARIOS);
        return usuarios.findAllByOrderByNombreCompletoAsc().stream()
            .map(usuario -> UsuarioAdminDto.de(usuario, reloj.instant())).toList();
    }

    /** Un administrador no puede cambiarse su propio rol: así nunca se queda el sistema sin administradores. */
    @Transactional
    public UsuarioAdminDto cambiarRol(Usuario actor, Long id, Rol rol) {
        politica.exigir(actor, Accion.GESTIONAR_USUARIOS);
        if (actor.getId().equals(id)) {
            throw new ConflictoEstadoException("NO_CAMBIAR_PROPIO_ROL", "No puedes cambiar tu propio rol");
        }
        Usuario usuario = buscar(id);
        usuario.setRol(rol);
        return UsuarioAdminDto.de(usuario, reloj.instant());
    }

    @Transactional
    public UsuarioAdminDto asignarTienda(Usuario actor, Long id, Long tiendaId) {
        politica.exigir(actor, Accion.GESTIONAR_USUARIOS);
        Usuario usuario = buscar(id);
        usuario.setTienda(tienda(tiendaId));
        return UsuarioAdminDto.de(usuario, reloj.instant());
    }

    @Transactional
    public UsuarioAdminDto desbloquear(Usuario actor, Long id) {
        politica.exigir(actor, Accion.GESTIONAR_USUARIOS);
        Usuario usuario = buscar(id);
        bloqueo.limpiar(usuario);
        return UsuarioAdminDto.de(usuario, reloj.instant());
    }

    /** Para quien perdió o cambió el celular: borra el secreto y pide enrolar la app de nuevo en el próximo inicio. */
    @Transactional
    public UsuarioAdminDto reiniciarMfa(Usuario actor, Long id) {
        politica.exigir(actor, Accion.GESTIONAR_USUARIOS);
        Usuario usuario = buscar(id);
        usuario.setMfaSecreto(null);
        usuario.setMfaHabilitado(false);
        usuario.setMfaUltimoPaso(null);
        desafios.consumirPendientes(usuario.getId());
        return UsuarioAdminDto.de(usuario, reloj.instant());
    }

    /** Quien entra con Google o GitHub llega sin tienda: puede elegirla una sola vez; después solo un administrador. */
    @Transactional
    public Usuario elegirTiendaPropia(Usuario actor, Long tiendaId) {
        Usuario usuario = usuarios.bloquearPorId(actor.getId()).orElseThrow();
        if (usuario.getTienda() != null) {
            throw new ConflictoEstadoException("TIENDA_YA_ASIGNADA",
                "Ya tienes una tienda asignada; pide a un administrador que la cambie");
        }
        usuario.setTienda(tienda(tiendaId));
        return usuario;
    }

    private Usuario buscar(Long id) {
        return usuarios.findWithTiendaById(id).orElseThrow(() -> new NoSuchElementException("Usuario " + id));
    }

    private Tienda tienda(Long tiendaId) {
        return tiendas.findById(tiendaId)
            .orElseThrow(() -> new DatosInvalidosException("tiendaId", "La tienda indicada no existe"));
    }
}
