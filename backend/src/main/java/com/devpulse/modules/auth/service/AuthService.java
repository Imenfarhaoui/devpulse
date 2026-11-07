package com.devpulse.modules.auth.service;

import com.devpulse.common.security.JwtTokenProvider;
import com.devpulse.modules.auth.dto.*;
import com.devpulse.modules.organization.entity.Membership;
import com.devpulse.modules.organization.entity.Organization;
import com.devpulse.modules.organization.entity.RefreshToken;
import com.devpulse.modules.organization.repository.MembershipRepository;
import com.devpulse.modules.organization.repository.OrganizationRepository;
import com.devpulse.modules.organization.repository.RefreshTokenRepository;
import com.devpulse.modules.user.entity.User;
import com.devpulse.modules.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       OrganizationRepository organizationRepository,
                       MembershipRepository membershipRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase(Locale.ROOT).trim())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        User user = new User(
                request.getFirstName().trim(),
                request.getLastName().trim(),
                request.getEmail().toLowerCase(Locale.ROOT).trim(),
                passwordEncoder.encode(request.getPassword()),
                "USER"
        );
        user = userRepository.save(user);

        String orgName = request.getOrganizationName();
        if (orgName == null || orgName.isBlank()) {
            orgName = user.getFirstName() + "'s Team";
        }
        String slug = generateUniqueSlug(orgName);

        Organization org = new Organization(orgName.trim(), slug, "FREE");
        org = organizationRepository.save(org);

        Membership membership = new Membership(user, org, "ORG_OWNER");
        membershipRepository.save(membership);

        return buildAuthResponse(user, org, "ORG_OWNER");
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase(Locale.ROOT).trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        List<Membership> memberships = membershipRepository.findAllByUserIdWithOrganization(user.getId());

        Organization activeOrg = null;
        String activeRole = null;

        if (request.getOrganizationId() != null) {
            Membership selected = memberships.stream()
                    .filter(m -> m.getOrganization().getId().equals(request.getOrganizationId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("User is not a member of specified organization"));
            activeOrg = selected.getOrganization();
            activeRole = selected.getRole();
        } else if (!memberships.isEmpty()) {
            Membership first = memberships.get(0);
            activeOrg = first.getOrganization();
            activeRole = first.getRole();
        }

        return buildAuthResponse(user, activeOrg, activeRole, memberships);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (storedToken.isRevoked() || storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token is expired or revoked");
        }

        User user = storedToken.getUser();
        List<Membership> memberships = membershipRepository.findAllByUserIdWithOrganization(user.getId());

        Organization activeOrg = null;
        String activeRole = null;

        if (request.getOrganizationId() != null) {
            Membership selected = memberships.stream()
                    .filter(m -> m.getOrganization().getId().equals(request.getOrganizationId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("User is not a member of specified organization"));
            activeOrg = selected.getOrganization();
            activeRole = selected.getRole();
        } else if (!memberships.isEmpty()) {
            Membership first = memberships.get(0);
            activeOrg = first.getOrganization();
            activeRole = first.getRole();
        }

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                activeOrg != null ? activeOrg.getId() : null,
                activeRole,
                user.getGlobalRole()
        );

        AuthResponse.UserDto userDto = new AuthResponse.UserDto(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getGlobalRole());
        AuthResponse.OrgDto orgDto = activeOrg != null ?
                new AuthResponse.OrgDto(activeOrg.getId(), activeOrg.getName(), activeOrg.getSlug(), activeOrg.getPlan()) : null;

        List<AuthResponse.OrgMembershipDto> userOrgs = memberships.stream()
                .map(m -> new AuthResponse.OrgMembershipDto(
                        m.getOrganization().getId(),
                        m.getOrganization().getName(),
                        m.getOrganization().getSlug(),
                        m.getRole()
                )).collect(Collectors.toList());

        return new AuthResponse(accessToken, storedToken.getToken(), userDto, orgDto, activeRole, userOrgs);
    }

    @Transactional(readOnly = true)
    public AuthResponse switchOrganization(UUID userId, UUID targetOrgId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Membership> memberships = membershipRepository.findAllByUserIdWithOrganization(user.getId());

        Membership selected = memberships.stream()
                .filter(m -> m.getOrganization().getId().equals(targetOrgId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of requested organization"));

        return buildAuthResponse(user, selected.getOrganization(), selected.getRole(), memberships);
    }

    private AuthResponse buildAuthResponse(User user, Organization org, String role) {
        List<Membership> memberships = membershipRepository.findAllByUserIdWithOrganization(user.getId());
        return buildAuthResponse(user, org, role, memberships);
    }

    private AuthResponse buildAuthResponse(User user, Organization org, String role, List<Membership> memberships) {
        UUID orgId = org != null ? org.getId() : null;
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                orgId,
                role,
                user.getGlobalRole()
        );

        String refreshTokenStr = jwtTokenProvider.generateRefreshToken(user.getId());
        RefreshToken refreshToken = new RefreshToken(user, refreshTokenStr, LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(refreshToken);

        AuthResponse.UserDto userDto = new AuthResponse.UserDto(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getGlobalRole());
        AuthResponse.OrgDto orgDto = org != null ?
                new AuthResponse.OrgDto(org.getId(), org.getName(), org.getSlug(), org.getPlan()) : null;

        List<AuthResponse.OrgMembershipDto> userOrgs = memberships.stream()
                .map(m -> new AuthResponse.OrgMembershipDto(
                        m.getOrganization().getId(),
                        m.getOrganization().getName(),
                        m.getOrganization().getSlug(),
                        m.getRole()
                )).collect(Collectors.toList());

        return new AuthResponse(accessToken, refreshTokenStr, userDto, orgDto, role, userOrgs);
    }

    private String generateUniqueSlug(String name) {
        String base = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-");
        if (base.startsWith("-")) base = base.substring(1);
        if (base.endsWith("-")) base = base.substring(0, base.length() - 1);
        if (base.isBlank()) base = "org";

        String candidate = base;
        int counter = 1;
        while (organizationRepository.existsBySlug(candidate)) {
            candidate = base + "-" + counter++;
        }
        return candidate;
    }
}

