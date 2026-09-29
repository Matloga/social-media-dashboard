package com.socialdashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SocialDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialDashboardApplication.class, args);
    }
}
