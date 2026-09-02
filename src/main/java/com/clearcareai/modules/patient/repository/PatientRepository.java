package com.clearcareai.modules.patient.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clearcareai.modules.patient.entity.Patient;

public interface PatientRepository extends JpaRepository<Patient,Long >{

    Optional<Patient> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}  