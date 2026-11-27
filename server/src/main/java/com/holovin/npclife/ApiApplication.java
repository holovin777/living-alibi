package com.holovin.npclife;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Optional Godot-facing API. The existing console and simulation engine
 * remain independent of Spring Boot and retain their own entry point.
 */
@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class ApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class,args);
    }
}
