package com.library.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

// Entry point for the Library API application
// @EnableCaching activates Spring's caching abstraction so @Cacheable annotations work
@SpringBootApplication
@EnableCaching
public class LibraryApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryApiApplication.class, args);
    }
}
