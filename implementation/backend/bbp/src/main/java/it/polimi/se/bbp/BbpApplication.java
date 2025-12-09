package it.polimi.se.bbp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application class.
 * Entry point for the application.
 */
@SpringBootApplication
public class BbpApplication {

    /**
     * Application entry point.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(BbpApplication.class, args);
    }

}