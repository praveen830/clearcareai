package com.clearcareai.modules.auth.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String>{
    private static final int MIN_LENGTH=8;
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context){
        if(password==null){
            return true;
        }
        if(password.length()<MIN_LENGTH){
            return false;
        }
        boolean hasUpper=false;
        boolean hasLower=false;
        boolean hasDigit=false;
        for(int i=0;i<password.length();i++){
            char current=password.charAt(i);
            if(Character.isUpperCase(current)){
                hasUpper=true;
            }
            if(Character.isLowerCase(current)){
                hasLower=true;

            }
            if(Character.isDigit(current)){
                hasDigit=true;
            }

        }
        return hasUpper && hasLower && hasDigit;
   }
} 
