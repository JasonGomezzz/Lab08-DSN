package com.techstore.inventario.autenticacion;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.techstore.inventario.comun.PropiedadesTechStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Cierra el baile de OAuth2: descarta la sesión HTTP (la API es sin estado) y devuelve al navegador
 * a la interfaz con el token MFA parcial en el fragmento de la URL, que no viaja al servidor ni a logs.
 */
@Component
public class ManejadorExitoOAuth2 implements AuthenticationSuccessHandler {
    private static final Logger log = LoggerFactory.getLogger(ManejadorExitoOAuth2.class);

    private final ServicioLoginSocial loginSocial;
    private final String interfaz;

    public ManejadorExitoOAuth2(ServicioLoginSocial loginSocial, PropiedadesTechStore propiedades) {
        this.loginSocial = loginSocial;
        this.interfaz = propiedades.urls().interfaz();
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication autenticacion) throws IOException {
        SecurityContextHolder.clearContext();
        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
        response.sendRedirect(destino((OAuth2AuthenticationToken) autenticacion));
    }

    private String destino(OAuth2AuthenticationToken token) {
        try {
            IdentidadExterna identidad = IdentidadExterna.de(token.getAuthorizedClientRegistrationId(),
                token.getPrincipal().getAttributes());
            ResultadoLogin resultado = loginSocial.ingresar(identidad);
            return interfaz + "/mfa#token=" + URLEncoder.encode(resultado.mfaToken(), StandardCharsets.UTF_8)
                + "&estado=" + resultado.estado();
        } catch (AutenticacionException ex) {
            return interfaz + "/login#error=" + ex.getCodigo();
        } catch (RuntimeException ex) {
            log.error("Falló el inicio de sesión social", ex);
            return interfaz + "/login#error=LOGIN_SOCIAL_FALLIDO";
        }
    }
}
