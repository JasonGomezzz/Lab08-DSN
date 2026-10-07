package com.techstore.inventario.autenticacion;

import java.util.ArrayList;
import java.util.List;
import com.techstore.inventario.comun.PropiedadesTechStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.util.StringUtils;

/**
 * Registra Google y GitHub solo si tienen credenciales. Sin ninguna, la aplicación arranca igual y
 * simplemente no ofrece login social, en vez de fallar por un client-id vacío.
 */
@Configuration
public class ConfiguracionOAuth2 {

    @Bean
    @Conditional(HayProveedoresConfigurados.class)
    ClientRegistrationRepository registrosClientes(PropiedadesTechStore propiedades) {
        String base = propiedades.urls().base();
        List<ClientRegistration> registros = new ArrayList<>();
        PropiedadesTechStore.Proveedor google = propiedades.oauth2().google();
        if (google.configurado()) {
            // Sin el alcance "openid" Google se trata como OAuth2 puro y el perfil sale de /userinfo.
            registros.add(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(google.clientId()).clientSecret(google.clientSecret())
                .scope("profile", "email")
                .redirectUri(base + "/login/oauth2/code/google").build());
        }
        PropiedadesTechStore.Proveedor github = propiedades.oauth2().github();
        if (github.configurado()) {
            registros.add(CommonOAuth2Provider.GITHUB.getBuilder("github")
                .clientId(github.clientId()).clientSecret(github.clientSecret())
                .scope("read:user", "user:email")
                .redirectUri(base + "/login/oauth2/code/github").build());
        }
        return new InMemoryClientRegistrationRepository(registros);
    }

    static class HayProveedoresConfigurados implements Condition {
        @Override
        public boolean matches(ConditionContext contexto, AnnotatedTypeMetadata metadatos) {
            Environment entorno = contexto.getEnvironment();
            return tiene(entorno, "google") || tiene(entorno, "github");
        }

        private static boolean tiene(Environment entorno, String proveedor) {
            String prefijo = "techstore.oauth2." + proveedor;
            return StringUtils.hasText(entorno.getProperty(prefijo + ".client-id"))
                && StringUtils.hasText(entorno.getProperty(prefijo + ".client-secret"));
        }
    }
}
