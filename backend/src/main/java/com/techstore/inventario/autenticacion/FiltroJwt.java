package com.techstore.inventario.autenticacion;

import java.io.IOException;
import java.util.List;
import com.techstore.inventario.usuarios.Usuario;
import com.techstore.inventario.usuarios.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica la petición con el JWT del encabezado Authorization. Un token inválido no corta la
 * petición aquí: se deja sin autenticar y el punto de entrada responde 401 con el motivo concreto
 * solo si la ruta lo exige, así las rutas públicas (login, registro) siguen funcionando.
 */
public class FiltroJwt extends OncePerRequestFilter {
    public static final String ATRIBUTO_ERROR = FiltroJwt.class.getName() + ".ERROR";
    public static final String AUTORIDAD_ACCESO = "ACCESO_COMPLETO";
    public static final String AUTORIDAD_MFA = "MFA_PENDIENTE";

    private final ServicioJwt jwt;
    private final UsuarioRepository usuarios;

    public FiltroJwt(ServicioJwt jwt, UsuarioRepository usuarios) {
        this.jwt = jwt;
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null) {
            if (!cabecera.startsWith("Bearer ") || cabecera.substring(7).isBlank()) {
                request.setAttribute(ATRIBUTO_ERROR, "TOKEN_INVALIDO");
            } else {
                autenticar(request, cabecera.substring(7));
            }
        }
        chain.doFilter(request, response);
    }

    private void autenticar(HttpServletRequest request, String token) {
        try {
            ServicioJwt.DatosToken datos = jwt.verificar(token);
            Usuario usuario = usuarios.findWithTiendaById(datos.usuarioId())
                .orElseThrow(() -> new TokenInvalidoException("TOKEN_INVALIDO"));
            String autoridad = datos.tipo() == ServicioJwt.Tipo.ACCESO ? AUTORIDAD_ACCESO : AUTORIDAD_MFA;
            UsernamePasswordAuthenticationToken autenticacion = new UsernamePasswordAuthenticationToken(
                usuario, datos, List.of(new SimpleGrantedAuthority(autoridad)));
            autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        } catch (TokenInvalidoException ex) {
            SecurityContextHolder.clearContext();
            request.setAttribute(ATRIBUTO_ERROR, ex.getMessage());
        }
    }
}
