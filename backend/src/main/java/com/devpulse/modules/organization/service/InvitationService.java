package com.devpulse.modules.organization.service;

import com.devpulse.modules.auth.dto.AcceptInviteRequest;
import com.devpulse.modules.auth.dto.InviteRequest;
import com.devpulse.modules.organization.entity.Invitation;
import com.devpulse.modules.organization.entity.Membership;
import com.devpulse.modules.organization.entity.Organization;
import com.devpulse.modules.organization.repository.InvitationRepository;
import com.devpulse.modules.organization.repository.MembershipRepository;
import com.devpulse.modules.organization.repository.OrganizationRepository;
import com.devpulse.modules.user.entity.User;
import com.devpulse.modules.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public InvitationService(InvitationRepository invitationRepository,
                             OrganizationRepository organizationRepository,
                             MembershipRepository membershipRepository,
                             UserRepository userRepository,
                             PasswordEncoder passwordEncoder) {
        this.invitationRepository = invitationRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Invitation createInvitation(UUID callerId, UUID orgId, InviteRequest request) {
        Membership callerMembership = membershipRepository.findByUserIdAndOrganizationId(callerId, orgId)
                .orElseThrow(() -> new AccessDeniedException("Access denied: Not a member of this organization"));

        if (!"ORG_OWNER".equals(callerMembership.getRole())) {
            throw new AccessDeniedException("Only Organization Owners can invite new members");
        }

        Organization org = callerMembership.getOrganization();
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        Invitation invitation = new Invitation(
                org,
                request.getEmail().toLowerCase(Locale.ROOT).trim(),
                request.getRole(),
                token,
                expiresAt
        );

        return invitationRepository.save(invitation);
    }

    @Transactional(readOnly = true)
    public List<Invitation> getInvitations(UUID callerId, UUID orgId) {
        membershipRepository.findByUserIdAndOrganizationId(callerId, orgId)
                .orElseThrow(() -> new AccessDeniedException("Access denied"));

        return invitationRepository.findByOrganizationId(orgId);
    }

    @Transactional
    public void acceptInvitation(AcceptInviteRequest request) {
        Invitation invitation = invitationRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid invitation token"));

        if (!"PENDING".equals(invitation.getStatus()) || invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invitation is no longer valid or has expired");
        }

        String email = invitation.getEmail();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                throw new IllegalArgumentException("Password is required for new user registration");
            }
            String firstName = request.getFirstName() != null ? request.getFirstName() : "Team";
            String lastName = request.getLastName() != null ? request.getLastName() : "Member";

            user = new User(firstName, lastName, email, passwordEncoder.encode(request.getPassword()), "USER");
            user = userRepository.save(user);
        }

        if (!membershipRepository.existsByUserIdAndOrganizationId(user.getId(), invitation.getOrganization().getId())) {
            Membership membership = new Membership(user, invitation.getOrganization(), invitation.getRole());
            membershipRepository.save(membership);
        }

        invitation.setStatus("ACCEPTED");
        invitationRepository.save(invitation);
    }

    @Transactional
    public void revokeInvitation(UUID callerId, UUID orgId, UUID invitationId) {
        Membership callerMembership = membershipRepository.findByUserIdAndOrganizationId(callerId, orgId)
                .orElseThrow(() -> new AccessDeniedException("Access denied"));

        if (!"ORG_OWNER".equals(callerMembership.getRole())) {
            throw new AccessDeniedException("Only Organization Owners can revoke invitations");
        }

        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));

        invitation.setStatus("REVOKED");
        invitationRepository.save(invitation);
    }
}

