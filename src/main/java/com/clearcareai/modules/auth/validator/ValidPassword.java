package com.clearcareai.modules.auth.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)


public @interface ValidPassword {
    String message() default "Password must be atleast 8 characters and contain at least one uppercase letter,one lowercase letter,one digit";
    Class<?>[] groups() default {}; 
    Class<? extends Payload >[] payload() default {};      
}
