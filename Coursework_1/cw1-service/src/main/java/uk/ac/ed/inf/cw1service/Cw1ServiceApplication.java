package uk.ac.ed.inf.cw1service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class Cw1ServiceApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(Cw1ServiceApplication.class, args);
    }
    @Bean
    public RestTemplate restTemplate()
    {
        return new RestTemplate();
    }
}
