package com.clearcareai.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiResponse<T>{
    private boolean success;
    private String message;
    private T data;
    public static <T> ApiResponse<T> success(String message,T data){
        return  ApiResponse.<T>builder().success(true).message(message).data(data).build();

    }
    public static ApiResponse error(String message){
        return ApiResponse.builder().success(false).message(message).build();
    }    
}
