package com.techstore.inventario.autenticacion;

import java.io.IOException;
import com.techstore.inventario.comun.PropiedadesTechStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/** El usuario canceló el consentimiento, el estado no coincide o el proveedor rechazó el código. */
@Component
public class ManejadorFalloOAuth2 implements AuthenticationFailureHandler {
    private static final Logger log = LoggerFactory.getLogger(ManejadorFalloOAuth2.class);

    private final String interfaz;

    public ManejadorFalloOAuth2(PropiedadesTechStore propiedades) {
        this.interfaz = propiedades.urls().interfaz();
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException ex) throws IOException {
        log.warn("Falló el login social: {}", ex.getMessage());
        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
        response.sendRedirect(interfaz + "/login#error=LOGIN_SOCIAL_FALLIDO");
    }
}
