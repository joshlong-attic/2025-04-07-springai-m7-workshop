package com.example.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@SpringBootApplication
public class McpApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpApplication.class, args);
    }

    @Bean
    ToolCallbackProvider toolCallbackProvider (DogSchedulerService schedulerService){
        return MethodToolCallbackProvider
                .builder()
                .toolObjects(schedulerService)
                .build();
    }
}

@Service
class DogSchedulerService {

    @Tool (description = "schedule an appointment to pickup or adopt a dog " +
            "from a Pooch Palace location")
    String schedule(@ToolParam int dogId,
                    @ToolParam String dogName) {
        var instant = Instant
                .now()
                .plus(3, ChronoUnit.DAYS)
                .toString();
        System.out.println("scheduling " + dogName + " / " + dogId + " for " + instant);
        return instant;
    }
}