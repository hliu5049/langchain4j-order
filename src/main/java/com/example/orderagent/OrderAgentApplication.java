package com.example.orderagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OrderAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderAgentApplication.class, args);
        System.out.println("Order Agent Application is running...");
    }
}
