package com.clearcareai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "app.omnidim")
@Validated
public class OmniDimProperties {
    @NotBlank
    private String apiKey;
    @NotNull
    private Integer agentId;
    @NotBlank
    private String baseUrl;
    
}
