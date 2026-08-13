package com.medour.service;

import com.medour.exception.WherebyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WherebyServiceTest {

  @Mock
  private RestTemplate restTemplate;

  private WherebyService wherebyService;

  @BeforeEach
  void setUp() {
    wherebyService = new WherebyService(restTemplate);
    ReflectionTestUtils.setField(wherebyService, "apiUrl", "https://api.whereby.dev/v1/meetings");
  }

  @Test
  void createRoom_blankApiKey_returnsPlaceholderUrl() {
    ReflectionTestUtils.setField(wherebyService, "apiKey", "");

    String url = wherebyService.createRoom(LocalDate.of(2026, 9, 1));

    assertThat(url).startsWith("https://whereby.com/dev-room-");
  }

  @Test
  @SuppressWarnings("unchecked")
  void createRoom_validApiKey_returnsRoomUrlFromResponse() {
    ReflectionTestUtils.setField(wherebyService, "apiKey", "test-api-key");
    Map<String, Object> body = Map.of("roomUrl", "https://whereby.com/test-room");
    given(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
        .willReturn(ResponseEntity.ok(body));

    String url = wherebyService.createRoom(LocalDate.of(2026, 9, 1));

    assertThat(url).isEqualTo("https://whereby.com/test-room");
  }

  @Test
  @SuppressWarnings("unchecked")
  void createRoom_apiThrows_throwsWherebyException() {
    ReflectionTestUtils.setField(wherebyService, "apiKey", "test-api-key");
    given(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
        .willThrow(new RestClientException("connection refused"));

    assertThrows(WherebyException.class,
        () -> wherebyService.createRoom(LocalDate.of(2026, 9, 1)));
  }
}
