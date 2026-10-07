package com.techstore.inventario.comun;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionTiempo {

    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }
}
