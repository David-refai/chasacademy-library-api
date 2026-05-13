package com.library.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

// نقطة البداية للتطبيق
// @EnableCaching تُفعّل ميزة التخزين المؤقت (Cache) في كل مكان في التطبيق
@SpringBootApplication
@EnableCaching
public class LibraryApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryApiApplication.class, args);
    }
}
