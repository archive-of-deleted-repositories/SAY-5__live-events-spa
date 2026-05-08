package com.say5.livevents;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class LiveEventsApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(LiveEventsApplication.class)
                .web(WebApplicationType.SERVLET)
                .run(args);
    }
}
