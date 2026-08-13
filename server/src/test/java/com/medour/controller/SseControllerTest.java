package com.medour.controller;

import com.medour.security.JwtUtil;
import com.medour.service.SseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SseController.class)
@AutoConfigureMockMvc(addFilters = false)
class SseControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private SseService sseService;

  @MockBean
  private JwtUtil jwtUtil;

  @Test
  void subscribeSlots_returns200WithEventStreamContentType() throws Exception {
    SseEmitter emitter = new SseEmitter();
    when(sseService.subscribe()).thenReturn(emitter);

    MvcResult async = mockMvc.perform(get("/api/v1/sse/slots"))
        .andExpect(request().asyncStarted())
        .andReturn();

    emitter.complete();

    mockMvc.perform(asyncDispatch(async))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/event-stream"));
  }
}
