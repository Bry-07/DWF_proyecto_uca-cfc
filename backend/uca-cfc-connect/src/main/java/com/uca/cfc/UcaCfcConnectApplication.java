package com.uca.cfc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal: es la que arranca toda la aplicacion.
 * Spring Boot se encarga de buscar los controladores, servicios y repositorios
 * y conectarlos solo.
 */
@SpringBootApplication
public class UcaCfcConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(UcaCfcConnectApplication.class, args);
    }
}
