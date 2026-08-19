package com.clearcareai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.clearcareai.common.ApiResponse;

import lombok.extern.slf4j.Slf4j;
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("resource not found :{}",ex.getMessage());
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()),HttpStatus.NOT_FOUND);

    }
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequestException(BadRequestException ex) {
       log.error("bad request:{}",ex.getMessage());
       return new ResponseEntity<>(ApiResponse.error(ex.getMessage()),HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorizedException(UnauthorizedException ex) {
        log.error("unauthorized:{}",ex.getMessage());
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()),HttpStatus.UNAUTHORIZED);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {
        log.error("internal server error:{}",ex.getMessage());
        return new ResponseEntity<>(ApiResponse.error("Internal Server Error"),HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
}
