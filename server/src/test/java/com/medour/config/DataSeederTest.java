package com.medour.config;

import com.medour.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

  @Mock
  private UserService userService;

  @InjectMocks
  private DataSeeder dataSeeder;

  @Test
  void run_callsSeedAdminWithConfiguredCredentials() throws Exception {
    ReflectionTestUtils.setField(dataSeeder, "adminEmail", "admin@medour.com");
    ReflectionTestUtils.setField(dataSeeder, "adminPassword", "Admin1234!");

    dataSeeder.run();

    verify(userService, times(1)).seedAdmin("admin@medour.com", "Admin1234!");
  }
}
