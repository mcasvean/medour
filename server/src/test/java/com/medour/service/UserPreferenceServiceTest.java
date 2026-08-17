package com.medour.service;

import com.medour.dto.UpdatePreferencesRequest;
import com.medour.entity.UserPreference;
import com.medour.repository.UserPreferenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {

  @Mock
  private UserPreferenceRepository userPreferenceRepository;

  @InjectMocks
  private UserPreferenceService userPreferenceService;

  @Test
  void getOrCreate_noExistingRow_createsDefaultAndReturnsFalse() {
    when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());
    when(userPreferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var dto = userPreferenceService.getOrCreate(1L);

    assertThat(dto.pinnedSidebar()).isFalse();
  }

  @Test
  void getOrCreate_existingRow_returnsStoredValue() {
    var pref = UserPreference.builder().userId(1L).pinnedSidebar(true).build();
    when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.of(pref));

    var dto = userPreferenceService.getOrCreate(1L);

    assertThat(dto.pinnedSidebar()).isTrue();
  }

  @Test
  void update_setsPinnedSidebarTrue() {
    when(userPreferenceRepository.findByUserId(2L)).thenReturn(Optional.empty());
    when(userPreferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var dto = userPreferenceService.update(2L, new UpdatePreferencesRequest(true));

    assertThat(dto.pinnedSidebar()).isTrue();
  }

  @Test
  void update_setsPinnedSidebarFalse() {
    var pref = UserPreference.builder().userId(3L).pinnedSidebar(true).build();
    when(userPreferenceRepository.findByUserId(3L)).thenReturn(Optional.of(pref));
    when(userPreferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var dto = userPreferenceService.update(3L, new UpdatePreferencesRequest(false));

    assertThat(dto.pinnedSidebar()).isFalse();
  }
}
