package com.erpnext;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Classe principale de l'application Spring Boot
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.erpnext.controller", "com.erpnext.service", "com.erpnext.repository", "com.erpnext.utils"})
public class ErpNextApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErpNextApplication.class, args);
    }
}
