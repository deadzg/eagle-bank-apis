package com.eaglebank.api; // <--- This line must match your main application's package

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test for the EagleBankApiApplication.
 * This test confirms that the Spring Boot application context loads successfully.
 * * Note: If your main application class is in a different package (e.g., com.myapp.bank),
 * the package name at the top of this file must be updated accordingly.
 */
@SpringBootTest
class EagleBankApiApplicationTests {

    // Autowire the ApplicationContext to verify it was instantiated successfully.
    @Autowired
    private ApplicationContext context;

    /**
     * Test case to verify that the application context loads.
     * This checks for fundamental configuration errors and ensures the application
     * can start without throwing an exception.
     */
    @Test
    void contextLoads() {
        // Assert that the context object is not null, indicating a successful load.
        System.out.println("The Eagle Bank API application context loaded successfully.");
        assert context != null : "Application context should not be null after loading.";
    }
}