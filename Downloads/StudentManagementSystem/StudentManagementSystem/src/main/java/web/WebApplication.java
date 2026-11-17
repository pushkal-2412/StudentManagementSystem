package web;

import database.DBConnection;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class WebApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }

    @Bean
    ApplicationRunner databaseCheck() {
        return args -> {
            try {
                DBConnection.testConnection();
                LOGGER.info("Connected to the configured MySQL database.");
            } catch (Exception ex) {
                LOGGER.error("Cannot connect to the configured MySQL database.", ex);
            }
        };
    }
}
