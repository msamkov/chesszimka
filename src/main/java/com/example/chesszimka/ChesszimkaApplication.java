package com.example.chesszimka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Точка входа Spring Boot приложения «Шахматы». */
@SpringBootApplication
public class ChesszimkaApplication {

    /** Запускает встроенный сервер и весь контекст Spring. */
    public static void main(String[] args) {
        SpringApplication.run(ChesszimkaApplication.class, args);
    }

}

