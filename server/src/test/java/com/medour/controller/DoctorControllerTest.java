package com.medour.controller;

import com.medour.dto.DoctorSearchResult;
import com.medour.security.JwtUtil;
import com.medour.service.DoctorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DoctorController.class)
@AutoConfigureMockMvc(addFilters = false)
class DoctorControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private DoctorService doctorService;

  @MockBean
  private JwtUtil jwtUtil;

  @Test
  void getAllDoctors_noParams_returns200WithList() throws Exception {
    given(doctorService.searchDoctors(any(), any(), any(), any()))
        .willReturn(List.of(
            new DoctorSearchResult(1L, "John", "Smith", "Cardiology", "Dublin", "Dublin", null),
            new DoctorSearchResult(2L, "Jane", "Doe", "Neurology", "Cork", "Cork", null)));

    mockMvc.perform(get("/api/v1/doctors/"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void getAllDoctors_withSpecialityFilter_returns200WithFilteredList() throws Exception {
    given(doctorService.searchDoctors(eq("Cardiology"), isNull(), isNull(), isNull()))
        .willReturn(List.of(
            new DoctorSearchResult(1L, "John", "Smith", "Cardiology", "Dublin", "Dublin", null)));

    mockMvc.perform(get("/api/v1/doctors/?speciality=Cardiology"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].speciality").value("Cardiology"));
  }
}
