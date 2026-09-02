package com.clearcareai.modules.patient.validator;

import com.clearcareai.modules.patient.exception.PatientException;
import org.springframework.stereotype.Component;

@Component
public class PatientValidator {

    
    public void validateProfileDoesNotExist(boolean profileExists) {
        if (profileExists) {
            throw new PatientException("Patient profile already exists for this user");
        }
    }

    public void validateProfileExists(boolean profileExists) {
        if (!profileExists) {
            throw new PatientException("Patient profile not found");
        }
    }
}