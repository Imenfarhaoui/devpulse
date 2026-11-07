package com.devpulse;

import com.devpulse.modules.auth.dto.AuthResponse;
import com.devpulse.modules.auth.dto.LoginRequest;
import com.devpulse.modules.auth.dto.RegisterRequest;
import com.devpulse.modules.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Test
    @DisplayName("User registration creates user, organization, and ORG_OWNER membership")
    void testRegistrationAndLogin() {
        String uniqueEmail = "user_" + UUID.randomUUID().toString().substring(0, 8) + "@devpulse.io";

        RegisterRequest reg = new RegisterRequest(
                "Jane",
                "Doe",
                uniqueEmail,
                "SecurePass123!",
                "Acme Corp"
        );

        AuthResponse regResponse = authService.register(reg);

        assertNotNull(regResponse.getAccessToken());
        assertNotNull(regResponse.getRefreshToken());
        assertEquals("ORG_OWNER", regResponse.getOrganizationRole());
        assertEquals("Acme Corp", regResponse.getActiveOrganization().getName());
        assertEquals("Jane", regResponse.getUser().getFirstName());

        // Now test Login
        LoginRequest login = new LoginRequest(uniqueEmail, "SecurePass123!", null);
        AuthResponse loginResponse = authService.login(login);

        assertNotNull(loginResponse.getAccessToken());
        assertEquals(regResponse.getUser().getId(), loginResponse.getUser().getId());
        assertEquals("ORG_OWNER", loginResponse.getOrganizationRole());
    }
}

