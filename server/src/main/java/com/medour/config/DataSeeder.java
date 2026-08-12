package com.medour.config;

import com.medour.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

  private final UserService userService;

  @Value("${admin.email}")
  private String adminEmail;

  @Value("${admin.password}")
  private String adminPassword;

  public DataSeeder(UserService userService) {
    this.userService = userService;
  }

  @Override
  public void run(String... args) {
    userService.seedAdmin(adminEmail, adminPassword);
  }
}
