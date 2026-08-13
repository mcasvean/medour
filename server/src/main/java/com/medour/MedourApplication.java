package com.medour;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MedourApplication {

  public static void main(String[] args) {
    SpringApplication.run(MedourApplication.class, args);
  }
}
