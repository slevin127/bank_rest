package com.example.bankcards.config;

import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.UserAccount;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.UserService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final CardService cardService;
    private final AppProperties properties;

    public DataInitializer(
            UserService userService,
            PasswordEncoder passwordEncoder,
            CardService cardService,
            AppProperties properties) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.cardService = cardService;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        AppProperties.Admin admin = properties.getInit().getAdmin();
        UserAccount adminAccount = userService.findByUsername(admin.getUsername()).orElseGet(() -> {
            UserAccount user = new UserAccount();
            user.setUsername(admin.getUsername());
            user.setPasswordHash(passwordEncoder.encode(admin.getPassword()));
            user.setFullName(admin.getFullName());
            user.setEmail(admin.getEmail());
            user.setEnabled(true);
            userService.save(user, Set.of(Role.ADMIN, Role.USER));
            log.info("Default admin user '{}' ensured", admin.getUsername());
            return user;
        });

        createSampleCardsIfConfigured(adminAccount);
    }

    private void createSampleCardsIfConfigured(UserAccount fallbackOwner) {
        AppProperties.SampleCards sample = properties.getInit().getSampleCards();
        if (!sample.isEnabled()) {
            return;
        }
        int targetCount = Math.max(sample.getCount(), 0);
        if (targetCount == 0) {
            return;
        }

        String resolvedOwnerUsername = (sample.getOwnerUsername() == null || sample.getOwnerUsername().isBlank())
                ? fallbackOwner.getUsername()
                : sample.getOwnerUsername();
        final String ownerUsername = resolvedOwnerUsername;

        UserAccount owner = userService
                .findByUsername(ownerUsername)
                .orElseGet(() -> {
                    if (!ownerUsername.equals(fallbackOwner.getUsername())) {
                        log.warn(
                                "Sample cards owner '{}' not found, falling back to admin user '{}'",
                                ownerUsername,
                                fallbackOwner.getUsername());
                    }
                    return fallbackOwner;
                });

        long existingCards = cardService
                .findCardsForOwner(owner.getId(), null, PageRequest.of(0, 1))
                .totalElements();
        if (existingCards >= targetCount) {
            log.info(
                    "Skipping sample card generation; {} cards already exist for user '{}'",
                    existingCards,
                    owner.getUsername());
            return;
        }

        LocalDate expiration = LocalDate.now().plusYears(4);
        for (int i = 0; i < targetCount; i++) {
            String cardNumber = String.format("520000000000%04d", i);
            BigDecimal balance = BigDecimal.valueOf(1000 + (i * 250L));
            CardCreateRequest request = new CardCreateRequest(owner.getId(), cardNumber, expiration, balance);
            try {
                cardService.createCard(request);
            } catch (BusinessException ex) {
                log.debug("Skipping demo card {}: {}", cardNumber, ex.getMessage());
            } catch (Exception ex) {
                log.warn("Failed to create demo card {}", cardNumber, ex);
            }
        }
        log.info("Sample cards generated for user '{}'", owner.getUsername());
    }
}
