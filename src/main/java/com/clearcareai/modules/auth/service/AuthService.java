package com.clearcareai.modules.auth.service;

import com.clearcareai.modules.auth.dto.AuthResponse;
import com.clearcareai.modules.auth.dto.LoginRequest;
import com.clearcareai.modules.auth.dto.RefreshTokenRequest;
import com.clearcareai.modules.auth.dto.RegisterRequest;

// no annotations here - @Service goes on the implementation, because Spring
// cannot instantiate an interface
public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);
}