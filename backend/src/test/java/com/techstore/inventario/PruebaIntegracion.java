package com.techstore.inventario;

import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;

/**
 * Base de las pruebas de integración: un único MySQL real compartido por todas las clases,
 * para que el contexto de Spring se reutilice y las pruebas no arranquen un contenedor cada una.
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIf("dockerDisponible")
public abstract class PruebaIntegracion {
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("techstore")
        .withUsername("techstore")
        .withPassword("prueba_local")
        .withCommand("--skip-log-bin");

    static boolean dockerDisponible() {
        return DockerClientFactory.instance().isDockerAvailable();
    }

    @DynamicPropertySource
    static void baseDeDatos(DynamicPropertyRegistry propiedades) {
        synchronized (MYSQL) {
            if (!MYSQL.isRunning()) {
                MYSQL.start();
            }
        }
        propiedades.add("spring.datasource.url", MYSQL::getJdbcUrl);
        propiedades.add("spring.datasource.username", MYSQL::getUsername);
        propiedades.add("spring.datasource.password", MYSQL::getPassword);
    }
}
