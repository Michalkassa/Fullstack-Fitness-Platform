package com.lockedin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Boots the whole application against a throwaway PostgreSQL container, which also
 * runs every Flyway migration. Skipped automatically when Docker isn't available.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
