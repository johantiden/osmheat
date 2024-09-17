package com.github.johantiden.osmheat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@SpringBootApplication
public class Main {
    public static final String VERSION;
    public static final String NAME;

    static {
        try (Scanner sc = new Scanner(Main.class.getResourceAsStream("/app.version"), StandardCharsets.UTF_8.name())) {
            VERSION = sc.next();
        }
        try (Scanner sc = new Scanner(Main.class.getResourceAsStream("/app.name"), StandardCharsets.UTF_8.name())) {
            NAME = sc.next();
        }
    }

    public static void main(String[] args) {
        System.out.println("hej");
        final ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
        context.registerShutdownHook();
    }
}
