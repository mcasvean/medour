package com.medour.service;

import com.medour.dto.UpdatePreferencesRequest;
import com.medour.dto.UserPreferenceDto;
import com.medour.entity.UserPreference;
import com.medour.repository.UserPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserPreferenceService {

  private final UserPreferenceRepository userPreferenceRepository;

  public UserPreferenceService(UserPreferenceRepository userPreferenceRepository) {
    this.userPreferenceRepository = userPreferenceRepository;
  }

  @Transactional
  public UserPreferenceDto getOrCreate(Long userId) {
    UserPreference pref = userPreferenceRepository.findByUserId(userId)
        .orElseGet(() -> userPreferenceRepository.save(
            UserPreference.builder().userId(userId).build()));
    return new UserPreferenceDto(pref.isPinnedSidebar());
  }

  @Transactional
  public UserPreferenceDto update(Long userId, UpdatePreferencesRequest req) {
    UserPreference pref = userPreferenceRepository.findByUserId(userId)
        .orElseGet(() -> UserPreference.builder().userId(userId).build());
    pref.setPinnedSidebar(req.pinnedSidebar());
    userPreferenceRepository.save(pref);
    return new UserPreferenceDto(pref.isPinnedSidebar());
  }
}
