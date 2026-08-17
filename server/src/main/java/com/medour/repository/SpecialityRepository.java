package com.medour.repository;

import com.medour.model.Speciality;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpecialityRepository extends JpaRepository<Speciality, Long> {

  Optional<Speciality> findByNameIgnoreCase(String name);

  List<Speciality> findAllByOrderByNameAsc();
}
