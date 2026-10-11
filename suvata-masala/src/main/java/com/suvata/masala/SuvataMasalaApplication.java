package com.suvata.masala;

import com.suvata.masala.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class SuvataMasalaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SuvataMasalaApplication.class, args);
    }
}
