package com.edinburgh.acp_demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

// @RestController tells Spring this class handles web requests
@RestController
public class HelloController
{

    // @GetMapping tells Spring to run this method when a user visits /hello
    @GetMapping("/hello")
    public Map<String, String> sayHello()
    {
        // We return a Map, which Spring Boot automatically converts to JSON
        return Map.of(
                "message", "Hello from your first Cloud Service!",
                "status", "Success",
                "course", "Applied Cloud Programming"
        );
    }

    @PostMapping("/greet")
    public Map<String, String> greetUser(@RequestBody Map<String, String> userData) {
        // We read the "name" from the JSON sent by the user
        String userName = userData.get("name");

        // We return a personalized response
        return Map.of(
                "message", "Hello " + userName + ", your POST request worked!",
                "status", "Created"
        );
    }
}