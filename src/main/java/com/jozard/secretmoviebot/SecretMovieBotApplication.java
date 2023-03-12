package com.jozard.secretmoviebot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.jozard.secretmoviebot")
public class SecretMovieBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecretMovieBotApplication.class, args);

    }

}
