package com.devpulse.modules.organization.service;

import com.devpulse.modules.auth.dto.AuthResponse;
import com.devpulse.modules.organization.entity.Membership;
import com.devpulse.modules.organization.entity.Organization;
import com.devpulse.modules.organization.repository.MembershipRepository;
import com.devpulse.modules.organization.repository.OrganizationRepository;
import com.devpulse.modules.user.entity.User;
import com.devpulse.modules.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public OrganizationService(OrganizationRepository organizationRepository,
                               MembershipRepository membershipRepository,
                               UserRepository userRepository) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Organization createOrganization(UUID userId, String name) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String slug = generateUniqueSlug(name);
        Organization org = new Organization(name.trim(), slug, "FREE");
        org = organizationRepository.save(org);

        Membership membership = new Membership(user, org, "ORG_OWNER");
        membershipRepository.save(membership);

        return org;
    }

    @Transactional(readOnly = true)
    public Organization getOrganization(UUID userId, UUID orgId) {
        validateUserInOrg(userId, orgId);
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
    }

    @Transactional(readOnly = true)
    public List<AuthResponse.OrgMembershipDto> getUserOrganizations(UUID userId) {
        return membershipRepository.findAllByUserIdWithOrganization(userId).stream()
                .map(m -> new AuthResponse.OrgMembershipDto(
                        m.getOrganization().getId(),
                        m.getOrganization().getName(),
                        m.getOrganization().getSlug(),
                        m.getRole()
                )).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MemberDto> getOrganizationMembers(UUID userId, UUID orgId) {
        validateUserInOrg(userId, orgId);
        return membershipRepository.findAllByOrganizationIdWithUser(orgId).stream()
                .map(m -> new MemberDto(
                        m.getId(),
                        m.getUser().getId(),
                        m.getUser().getEmail(),
                        m.getUser().getFirstName(),
                        m.getUser().getLastName(),
                        m.getRole(),
                        m.getCreatedAt()
                )).collect(Collectors.toList());
    }

    @Transactional
    public void updateMemberRole(UUID callerId, UUID orgId, UUID targetUserId, String newRole) {
        Membership callerMembership = membershipRepository.findByUserIdAndOrganizationId(callerId, orgId)
                .orElseThrow(() -> new AccessDeniedException("Access denied: Not a member of this organization"));

        if (!"ORG_OWNER".equals(callerMembership.getRole())) {
            throw new AccessDeniedException("Only Organization Owners can update roles");
        }

        Membership target = membershipRepository.findByUserIdAndOrganizationId(targetUserId, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found in organization"));

        target.setRole(newRole);
        membershipRepository.save(target);
    }

    @Transactional
    public void removeMember(UUID callerId, UUID orgId, UUID targetUserId) {
        Membership callerMembership = membershipRepository.findByUserIdAndOrganizationId(callerId, orgId)
                .orElseThrow(() -> new AccessDeniedException("Access denied"));

        if (!callerId.equals(targetUserId) && !"ORG_OWNER".equals(callerMembership.getRole())) {
            throw new AccessDeniedException("Only Organization Owners can remove members");
        }

        Membership target = membershipRepository.findByUserIdAndOrganizationId(targetUserId, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        membershipRepository.delete(target);
    }

    public void validateUserInOrg(UUID userId, UUID orgId) {
        if (!membershipRepository.existsByUserIdAndOrganizationId(userId, orgId)) {
            throw new AccessDeniedException("Access denied: Not a member of organization " + orgId);
        }
    }

    public Membership getMembership(UUID userId, UUID orgId) {
        return membershipRepository.findByUserIdAndOrganizationId(userId, orgId)
                .orElseThrow(() -> new AccessDeniedException("Access denied: Not a member of organization " + orgId));
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

    public static class MemberDto {
        private UUID membershipId;
        private UUID userId;
        private String email;
        private String firstName;
        private String lastName;
        private String role;
        private java.time.LocalDateTime joinedAt;

        public MemberDto(UUID membershipId, UUID userId, String email, String firstName, String lastName, String role, java.time.LocalDateTime joinedAt) {
            this.membershipId = membershipId;
            this.userId = userId;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.role = role;
            this.joinedAt = joinedAt;
        }

        public UUID getMembershipId() { return membershipId; }
        public UUID getUserId() { return userId; }
        public String getEmail() { return email; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getRole() { return role; }
        public java.time.LocalDateTime getJoinedAt() { return joinedAt; }
    }
}

