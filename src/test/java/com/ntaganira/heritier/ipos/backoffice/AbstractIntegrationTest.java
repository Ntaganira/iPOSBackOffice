package com.ntaganira.heritier.ipos.backoffice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    private static final PostgreSQLContainer<?> POSTGRES;
    private static final boolean USING_CONTAINER;

    static {
        boolean useContainer = false;
        PostgreSQLContainer<?> container = null;
        if (env("IPOS_TEST_DB_URL", "").isBlank()) {
            try {
                container = new PostgreSQLContainer<>("postgres:16-alpine")
                        .withDatabaseName("ipos_test")
                        .withUsername("postgres")
                        .withPassword("postgres");
                container.start();
                useContainer = true;
            } catch (RuntimeException ex) {
                log.warn("Testcontainers could not start PostgreSQL; using IPOS_TEST_DB_URL fallback. {}", ex.getMessage());
                if (container != null) {
                    try {
                        container.stop();
                    } catch (RuntimeException ignored) {
                        // The container never started.
                    }
                }
                container = null;
            }
        }
        POSTGRES = container;
        USING_CONTAINER = useContainer;
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        if (USING_CONTAINER && POSTGRES != null) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
            return;
        }
        registry.add("spring.datasource.url",
                () -> env("IPOS_TEST_DB_URL", "jdbc:postgresql://localhost:5437/ipos_test"));
        registry.add("spring.datasource.username", () -> env("IPOS_TEST_DB_USER", "postgres"));
        registry.add("spring.datasource.password", () -> env("IPOS_TEST_DB_PASSWORD", "postgres"));
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
