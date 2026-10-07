package com.techstore.inventario;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/** Registra "google" y "github" apuntando al {@link ProveedorFalso}, con la misma forma que en producción. */
@TestConfiguration
public class ConfiguracionProveedorFalso {

    @Bean
    ClientRegistrationRepository registrosFalsos() {
        String base = ProveedorFalso.base();
        return new InMemoryClientRegistrationRepository(
            CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId("id-google").clientSecret("secreto-google")
                .scope("profile", "email")
                .redirectUri("http://localhost:8080/login/oauth2/code/google")
                .authorizationUri(base + "/autorizar").tokenUri(base + "/token").userInfoUri(base + "/perfil")
                .build(),
            CommonOAuth2Provider.GITHUB.getBuilder("github")
                .clientId("id-github").clientSecret("secreto-github")
                .scope("read:user", "user:email")
                .redirectUri("http://localhost:8080/login/oauth2/code/github")
                .authorizationUri(base + "/autorizar").tokenUri(base + "/token").userInfoUri(base + "/perfil")
                .build());
    }
}
