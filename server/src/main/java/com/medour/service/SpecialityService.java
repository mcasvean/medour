package com.medour.service;

import com.medour.dto.SpecialityResponse;
import com.medour.model.Speciality;
import com.medour.repository.SpecialityRepository;
import com.medour.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SpecialityService {

  private final SpecialityRepository specialityRepository;
  private final UserRepository userRepository;

  public SpecialityService(SpecialityRepository specialityRepository, UserRepository userRepository) {
    this.specialityRepository = specialityRepository;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<SpecialityResponse> findAll() {
    return specialityRepository.findAllByOrderByNameAsc().stream()
        .map(s -> new SpecialityResponse(s.getId(), s.getName()))
        .toList();
  }

  @Transactional
  public SpecialityResponse create(String name) {
    String trimmed = name.trim();
    if (specialityRepository.findByNameIgnoreCase(trimmed).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Speciality already exists.");
    }
    Speciality saved = specialityRepository.save(Speciality.builder().name(trimmed).build());
    return new SpecialityResponse(saved.getId(), saved.getName());
  }

  @Transactional
  public SpecialityResponse update(Long id, String name) {
    Speciality speciality = specialityRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Speciality not found."));
    String trimmed = name.trim();
    specialityRepository.findByNameIgnoreCase(trimmed)
        .filter(existing -> !existing.getId().equals(id))
        .ifPresent(dup -> {
          throw new ResponseStatusException(HttpStatus.CONFLICT, "Speciality already exists.");
        });
    speciality.setName(trimmed);
    Speciality saved = specialityRepository.save(speciality);
    return new SpecialityResponse(saved.getId(), saved.getName());
  }

  @Transactional
  public void delete(Long id) {
    Speciality speciality = specialityRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Speciality not found."));
    if (userRepository.existsBySpecialityIgnoreCaseAndDeletedAtIsNull(speciality.getName())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Speciality is in use by one or more doctors and cannot be deleted.");
    }
    specialityRepository.delete(speciality);
  }
}
