package com.medour.config;

import com.medour.service.AutoCancelService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AutoCancelJob {

  private final AutoCancelService autoCancelService;

  public AutoCancelJob(AutoCancelService autoCancelService) {
    this.autoCancelService = autoCancelService;
  }

  @Scheduled(fixedRate = 60_000)
  public void run() {
    autoCancelService.autoCancelOverdue();
  }
}
