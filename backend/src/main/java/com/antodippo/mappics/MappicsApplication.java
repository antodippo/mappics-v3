package com.antodippo.mappics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MappicsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MappicsApplication.class, args);
    }
}
