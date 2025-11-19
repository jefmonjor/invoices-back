package com.invoices;

import com.invoices.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for Invoices Monolith Application.
 * Tests that the Spring application context loads successfully.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
class InvoicesMonolithApplicationTests {

	@Test
	void contextLoads() {
		// This test verifies that the entire monolith application context
		// can be loaded without errors
	}

}
