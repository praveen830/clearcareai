package com.clearcareai.modules.patient.exception;

import com.clearcareai.exception.BadRequestException;

public class PatientException extends BadRequestException {
    public PatientException(String message){
        super(message);
        }

    
}
