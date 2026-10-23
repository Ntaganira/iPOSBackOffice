package com.ntaganira.heritier.ipos.backoffice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
@EnableScheduling
public class IposBackOfficeApplication {

    public static void main(String[] args) {
        SpringApplication.run(IposBackOfficeApplication.class, args);
    }
}
