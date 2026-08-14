package com.medour.service;

import com.medour.exception.WherebyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Service
public class WherebyService {

  private final RestTemplate restTemplate;

  @Value("${whereby.api-url}")
  private String apiUrl;

  @Value("${whereby.api-key:}")
  private String apiKey;

  @Autowired
  public WherebyService(RestTemplateBuilder builder) {
    this.restTemplate = builder.build();
  }

  // Package-private for unit test injection
  WherebyService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @SuppressWarnings("unchecked")
  public String createRoom(LocalDate scheduledDate) {
    if (apiKey == null || apiKey.isBlank()) {
      return "https://whereby.com/dev-room-" + UUID.randomUUID();
    }
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(apiKey);
      headers.setContentType(MediaType.APPLICATION_JSON);
      String endDate = scheduledDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toString();
      Map<String, String> body = Map.of("roomMode", "group_hd", "endDate", endDate);
      HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
      ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
      String roomUrl = response.getBody() != null ? (String) response.getBody().get("roomUrl") : null;
      if (roomUrl == null)
        throw new WherebyException("Missing roomUrl in Whereby response", null);
      return roomUrl;
    } catch (Exception e) {
      throw new WherebyException("Whereby API error", e);
    }
  }
}
