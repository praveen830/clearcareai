package com.clearcareai.security;

import com.clearcareai.common.ApiResponse;
import com.clearcareai.common.AppConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthEntryPoint authEntryPoint;

    // B1's CorsConfig bean does nothing until the .cors(...) line below wires it
    private final CorsConfigurationSource corsConfigurationSource;

    // returns the INTERFACE, so swapping BCrypt for Argon2 is a one-line change
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // not building one - exposing the manager Spring Security already assembled,
    // so AuthServiceImpl can inject it in Part 6
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // safe because there is no ambient credential: no session cookie,
                // and a cross-site form cannot set an Authorization header.
                // If tokens ever move into cookies, this must come back on
                .csrf(csrf -> csrf.disable())

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)        // 401
                        .accessDeniedHandler(accessDeniedHandler()))     // 403

                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // FIRST MATCH WINS - order is significant throughout this block
                .authorizeHttpRequests(auth -> auth

                        // ===== ADDED FOR B1's TOOLING (not in the reference) =====
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // without this, every 404 and 500 comes back as a 401
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/doctors").permitAll()

                        // MUST precede the {id} rule below, or "profile" matches {id}
                        // and a doctor's own profile endpoint becomes public
                        .requestMatchers("/api/doctors/profile").hasRole(roleName(AppConstants.ROLE_DOCTOR))
                        .requestMatchers(HttpMethod.GET, "/api/doctors/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/doctor/{id}").permitAll()

                        // internal endpoints for the FastAPI service (A1) - a
                        // server-to-server call carries no user token
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/{id}/process").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/by-call-id/**").permitAll()

                        .requestMatchers("/api/admin/**").hasRole(roleName(AppConstants.ROLE_ADMIN))
                        .requestMatchers("/api/analytics/platform").hasRole(roleName(AppConstants.ROLE_ADMIN))

                        .requestMatchers(HttpMethod.GET, "/api/slots/doctor/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/slots/available").hasRole(roleName(AppConstants.ROLE_PATIENT))
                        .requestMatchers(HttpMethod.POST, "/api/slots").hasRole(roleName(AppConstants.ROLE_DOCTOR))
                        .requestMatchers(HttpMethod.DELETE, "/api/slots/**").hasRole(roleName(AppConstants.ROLE_DOCTOR))
                        .requestMatchers(HttpMethod.POST, "/api/consultations").hasRole(roleName(AppConstants.ROLE_DOCTOR))
                        .requestMatchers(HttpMethod.PUT, "/api/consultations/**").hasRole(roleName(AppConstants.ROLE_DOCTOR))
                        .requestMatchers("/api/voice-reviews/**").hasRole(roleName(AppConstants.ROLE_DOCTOR))
                        .requestMatchers("/api/analytics/doctor/**").hasRole(roleName(AppConstants.ROLE_DOCTOR))
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/{id}/flag").hasRole(roleName(AppConstants.ROLE_DOCTOR))

                        .requestMatchers(HttpMethod.POST, "/api/patients/profile").hasRole(roleName(AppConstants.ROLE_PATIENT))
                        .requestMatchers(HttpMethod.PUT, "/api/patients/profile").hasRole(roleName(AppConstants.ROLE_PATIENT))
                        .requestMatchers(HttpMethod.GET, "/api/patients/profile").hasRole(roleName(AppConstants.ROLE_PATIENT))
                        .requestMatchers(HttpMethod.POST, "/api/appointments").hasRole(roleName(AppConstants.ROLE_PATIENT))
                        .requestMatchers("/api/appointments/my").hasRole(roleName(AppConstants.ROLE_PATIENT))
                        .requestMatchers(HttpMethod.PUT, "/api/appointments/{id}/cancel").hasRole(roleName(AppConstants.ROLE_PATIENT))
                        .requestMatchers(HttpMethod.GET, "/api/appointments/doctor").hasRole(roleName(AppConstants.ROLE_DOCTOR))
                        .requestMatchers(HttpMethod.GET, "/api/appointments").hasRole(roleName(AppConstants.ROLE_ADMIN))
                        .requestMatchers(HttpMethod.POST, "/api/reviews").hasRole(roleName(AppConstants.ROLE_PATIENT))
                        .requestMatchers("/api/reviews/my").hasRole(roleName(AppConstants.ROLE_PATIENT))

                        .requestMatchers(HttpMethod.GET, "/api/patients/{id}")
                            .hasAnyRole(roleName(AppConstants.ROLE_DOCTOR), roleName(AppConstants.ROLE_ADMIN))

                        // deny by default: a forgotten endpoint fails closed
                        .anyRequest().authenticated()
                )
                // before, so the SecurityContext is populated by the time the
                // authorization filter reads it
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 403, and deliberately vague about which role was required
    private AccessDeniedHandler accessDeniedHandler() {
        ObjectMapper objectMapper = new ObjectMapper();
        return (request, response, deniedException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.error("Access denied: insufficient permissions")));
        };
    }

    // hasRole() prepends ROLE_ itself, so "ROLE_DOCTOR" would become
    // ROLE_ROLE_DOCTOR and silently never match
    private String roleName(String role) {
        return role.replace("ROLE_", "");
    }
}