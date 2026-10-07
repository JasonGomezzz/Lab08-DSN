package com.techstore.inventario.autenticacion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.techstore.inventario.comun.PropiedadesTechStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Carga el perfil del proveedor. GitHub no garantiza un correo en /user (puede ser privado y no
 * está marcado como verificado), así que el correo se toma solo de /user/emails: el principal verificado.
 */
@Service
public class ServicioUsuarioOAuth2 implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private static final Logger log = LoggerFactory.getLogger(ServicioUsuarioOAuth2.class);

    private final DefaultOAuth2UserService delegado = new DefaultOAuth2UserService();
    private final RestClient cliente;
    private final String urlCorreosGithub;

    public ServicioUsuarioOAuth2(RestClient.Builder constructor, PropiedadesTechStore propiedades) {
        this.cliente = constructor.build();
        this.urlCorreosGithub = propiedades.oauth2().githubCorreosUri();
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest solicitud) throws OAuth2AuthenticationException {
        OAuth2User usuario = delegado.loadUser(solicitud);
        if (!"github".equals(solicitud.getClientRegistration().getRegistrationId())) {
            return usuario;
        }
        Map<String, Object> atributos = new HashMap<>(usuario.getAttributes());
        atributos.remove("email");
        atributos.remove("email_verified");
        correoVerificado(solicitud.getAccessToken().getTokenValue()).ifPresent(correo -> {
            atributos.put("email", correo);
            atributos.put("email_verified", true);
        });
        String clave = solicitud.getClientRegistration().getProviderDetails().getUserInfoEndpoint()
            .getUserNameAttributeName();
        return new DefaultOAuth2User(usuario.getAuthorities(), atributos, clave);
    }

    public Optional<String> correoVerificado(String tokenAcceso) {
        try {
            List<Map<String, Object>> correos = cliente.get().uri(urlCorreosGithub)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAcceso)
                .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .retrieve().body(new ParameterizedTypeReference<>() {
                });
            if (correos == null) {
                return Optional.empty();
            }
            return correos.stream()
                .filter(correo -> Boolean.TRUE.equals(correo.get("primary")) && Boolean.TRUE.equals(correo.get("verified")))
                .map(correo -> String.valueOf(correo.get("email")))
                .findFirst();
        } catch (RestClientException ex) {
            log.warn("No se pudo leer el correo de GitHub: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
