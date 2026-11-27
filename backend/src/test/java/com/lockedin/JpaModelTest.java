package com.lockedin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * JPA slice against a throwaway PostgreSQL container. Flyway builds the real schema and
 * Hibernate validates every entity against it, so a passing model test also proves the
 * mapping matches the migrations. Skipped automatically when Docker isn't available.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
public @interface JpaModelTest {
}
