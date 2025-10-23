package com.example.bankcards.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    @NotNull
    private final Crypto crypto = new Crypto();

    @NotNull
    private final Jwt jwt = new Jwt();

    @NotNull
    private final Init init = new Init();

    @Getter
    @Setter
    public static class Crypto {
        @NotBlank
        private String secretKey;
    }

    @Getter
    @Setter
    public static class Jwt {
        @NotBlank
        private String secret;

        @NotBlank
        private String issuer;

        @Min(1)
        private long accessTokenExpirationMinutes;

        @Min(1)
        private long refreshTokenExpirationDays;
    }

    @Getter
    @Setter
    public static class Init {
        @NotNull
        private final Admin admin = new Admin();

        @NotNull
        private final SampleCards sampleCards = new SampleCards();
    }

    @Getter
    @Setter
    public static class Admin {
        @NotBlank
        private String username;

        @NotBlank
        private String password;

        @NotBlank
        private String fullName;

        @Email
        private String email;
    }

    @Getter
    @Setter
    public static class SampleCards {
        private boolean enabled;

        @Min(0)
        private int count;

        private String ownerUsername;
    }
}
