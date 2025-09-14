package br.com.teamtacles.team.service;

import br.com.teamtacles.common.service.EmailService;
import br.com.teamtacles.team.dto.request.InvitedMemberRequestDTO;
import br.com.teamtacles.team.dto.request.TeamRequestRegisterDTO;
import br.com.teamtacles.team.dto.request.TeamRequestUpdateDTO;
import br.com.teamtacles.team.dto.request.UpdateMemberRoleTeamRequestDTO;
import br.com.teamtacles.common.dto.response.page.PagedResponse;
import br.com.teamtacles.common.dto.response.InviteLinkResponseDTO;
import br.com.teamtacles.team.dto.response.TeamMemberResponseDTO;
import br.com.teamtacles.team.dto.response.TeamResponseDTO;
import br.com.teamtacles.team.dto.response.UserTeamResponseDTO;
import br.com.teamtacles.team.enumeration.ETeamRole;
import br.com.teamtacles.common.exception.ResourceAlreadyExistsException;
import br.com.teamtacles.common.exception.ResourceNotFoundException;
import br.com.teamtacles.common.mapper.PagedResponseMapper;
import br.com.teamtacles.team.model.Team;
import br.com.teamtacles.team.model.TeamMember;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.team.repository.TeamMemberRepository;
import br.com.teamtacles.team.repository.TeamRepository;
import br.com.teamtacles.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TeamService {
    private static final Logger log = LoggerFactory.getLogger(TeamService.class);

    @Value("${app.base-url}")
    private String baseUrl;

    private TeamRepository teamRepository;
    private TeamMemberRepository teamMemberRepository;
    private UserRepository userRepository;
    private ModelMapper modelMapper;
    private TeamAuthorizationService teamAuthorizationService;
    private PagedResponseMapper pagedResponseMapper;
    private EmailService emailService;

    public TeamService(TeamRepository teamRepository, TeamMemberRepository teamMemberRepository, UserRepository userRepository, ModelMapper modelMapper,
                       TeamAuthorizationService teamAuthorizationService, PagedResponseMapper pagedResponseMapper, EmailService emailService) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.teamAuthorizationService = teamAuthorizationService;
        this.pagedResponseMapper = pagedResponseMapper;
        this.emailService = emailService;
    }

    @Transactional
    public TeamResponseDTO createTeam(TeamRequestRegisterDTO dto, User actingUser) {
        log.info("User {} is attempting to create a new team with name: {}", actingUser.getUsername(), dto.getName());
        validateTeamNameUniqueness(dto.getName(), actingUser);

        Team newTeam = modelMapper.map(dto, Team.class);
        newTeam.setOwner(actingUser);

        TeamMember creatorMembership = new TeamMember(actingUser, newTeam, ETeamRole.OWNER);
        creatorMembership.setAcceptedInvite(true);
        newTeam.getMembers().add(creatorMembership);

        Team savedTeam = teamRepository.save(newTeam);
        log.info("Team '{}' created successfully with ID: {} by user {}", savedTeam.getName(), savedTeam.getId(), actingUser.getUsername());
        return modelMapper.map(savedTeam, TeamResponseDTO.class);
    }

    @Transactional
    public void inviteMember(Long teamId, InvitedMemberRequestDTO dto, User actingUser) {
        log.info("User {} is attempting to invite user with email {} to team ID: {}",
            actingUser.getUsername(), dto.getEmail(), teamId);

        Team team = findTeamByIdOrThrow(teamId);

        if(dto.getRole().equals(ETeamRole.OWNER)) {
            log.warn("Attempt to invite user as OWNER denied. Acting user: {}, Team: {}",
                actingUser.getUsername(), teamId);
            throw new IllegalArgumentException("Cannot invite a user to be OWNER.");
        }

        teamAuthorizationService.checkTeamAdmin(actingUser, team);
        User userToInvite = findByEmailIgnoreCaseOrThrow(dto.getEmail());

        if (teamMemberRepository.findByUserAndTeam(userToInvite, team).isPresent()) {
            log.warn("Invite failed: User {} is already a member of team ID: {}", userToInvite.getUsername(), teamId);
            throw new ResourceAlreadyExistsException("User is already a member of this team.");
        }

        String token = UUID.randomUUID().toString();
        TeamMember newMember = new TeamMember(userToInvite, team, dto.getRole());
        newMember.setInvitationToken(token);
        newMember.setInvitationTokenExpiry(LocalDateTime.now().plusHours(24));

        teamMemberRepository.save(newMember);
        log.info("User {} invited successfully to team ID: {} with role {}. Invitation token generated.",
            userToInvite.getUsername(), teamId, dto.getRole());

        emailService.sendTeamInvitationEmail(userToInvite.getEmail(), team.getName(), token);
    }

    @Transactional
    public void acceptInvitation(String token) {
        log.info("Processing invitation acceptance with token: {}", token);

        if (token == null || token.isEmpty()) {
            log.warn("Attempt to accept invitation with null or empty token");
            throw new IllegalArgumentException("Invitation token cannot be null or empty.");
        }

        TeamMember membership = findByInvitationTokenEmailORThrow(token);

        if(membership.getInvitationTokenExpiry().isBefore(LocalDateTime.now())) {
            log.warn("Attempt to accept expired invitation token for team: {}", membership.getTeam().getId());
            throw new ResourceNotFoundException("Invitation token has expired.");
        }

        log.debug("Accepting invitation for user {} in team {}",
            membership.getUser().getUsername(), membership.getTeam().getId());

        membership.setAcceptedInvite(true);
        membership.setInvitationToken(null);
        membership.setInvitationTokenExpiry(null);

        teamMemberRepository.save(membership);
        log.info("Invitation accepted successfully for user {} in team {}",
            membership.getUser().getUsername(), membership.getTeam().getId());
    }

    @Transactional
    public TeamResponseDTO updateTeam(Long teamId, TeamRequestUpdateDTO dto, User actingUser) {
        log.info("User {} is attempting to update team ID: {}", actingUser.getUsername(), teamId);
        Team team = findTeamByIdOrThrow(teamId);
        teamAuthorizationService.checkTeamOwner(actingUser, team);

        if(dto.getName() != null && !team.getName().equalsIgnoreCase(dto.getName())){
            log.debug("Updating team name from '{}' to '{}'", team.getName(), dto.getName());
            validateTeamNameUniqueness(dto.getName(), team.getOwner());
        }

        if(dto.getName() != null && !dto.getName().isEmpty()){
            team.setName(dto.getName());
        }

        if(dto.getDescription() != null){
            log.debug("Updating team description for team ID: {}", teamId);
            team.setDescription(dto.getDescription());
        }

        Team updatedTeam = teamRepository.save(team);
        log.info("Team ID: {} updated successfully by user {}", teamId, actingUser.getUsername());
        return modelMapper.map(updatedTeam, TeamResponseDTO.class);
    }

    @Transactional
    public TeamMemberResponseDTO updateMemberRole(Long teamId, Long userIdToUpdate, UpdateMemberRoleTeamRequestDTO dto, User actingUser) {
        log.info("User {} is attempting to update role for user {} in team {}. New role: {}",
            actingUser.getUsername(), userIdToUpdate, teamId, dto.getNewRole());

        Team team = findTeamByIdOrThrow(teamId);
        teamAuthorizationService.checkTeamAdmin(actingUser, team);

        User userToUpdate = findUserByIdOrThrow(userIdToUpdate);
        TeamMember membershipToUpdate = findMembershipByIdOrThrow(userToUpdate, team);

        if (membershipToUpdate.getTeamRole().equals(ETeamRole.OWNER)) {
            log.warn("Attempt to change OWNER role denied. Acting user: {}, Target user: {}",
                actingUser.getUsername(), userToUpdate.getUsername());
            throw new AccessDeniedException("The team OWNER's role cannot be changed.");
        }

        if (membershipToUpdate.getTeamRole().equals(ETeamRole.ADMIN)) {
            log.debug("Attempting to change ADMIN role, checking if acting user is OWNER");
            teamAuthorizationService.checkTeamOwner(actingUser, team);
        }

        if (dto.getNewRole().equals(ETeamRole.OWNER)) {
            log.warn("Attempt to promote user to OWNER role denied. Acting user: {}, Target user: {}",
                actingUser.getUsername(), userToUpdate.getUsername());
            throw new IllegalArgumentException("Cannot promote a user to OWNER.");
        }

        log.debug("Updating role from {} to {} for user {} in team {}",
            membershipToUpdate.getTeamRole(), dto.getNewRole(), userToUpdate.getUsername(), teamId);
        membershipToUpdate.setTeamRole(dto.getNewRole());
        TeamMember updatedMembership = teamMemberRepository.save(membershipToUpdate);

        log.info("Role successfully updated for user {} to {} in team {}",
            userToUpdate.getUsername(), dto.getNewRole(), teamId);
        return toTeamMemberResponseDTO(updatedMembership);
    }

    public TeamResponseDTO getTeamById(Long teamId, User actingUser) {
        log.debug("User {} requesting team with ID: {}", actingUser.getUsername(), teamId);
        Team team = findTeamByIdOrThrow(teamId);
        teamAuthorizationService.checkTeamMembership(actingUser, team);
        log.debug("Team {} ({}) successfully accessed by user {}", team.getName(), teamId, actingUser.getUsername());
        return modelMapper.map(team, TeamResponseDTO.class);
    }

    public PagedResponse<UserTeamResponseDTO> getAllTeamsByUser(Pageable pageable, User actingUser) {
        log.debug("Fetching teams for user {}. Page: {}, Size: {}",
            actingUser.getUsername(), pageable.getPageNumber(), pageable.getPageSize());

        Page<TeamMember> userTeamsPage = teamMemberRepository.findByUserAndAcceptedInviteTrue(actingUser, pageable);

        Page<UserTeamResponseDTO> userTeamResponseDTOPage = userTeamsPage.map(membership -> {
            return toUserTeamResponseDTO(membership.getTeam(), membership);
        });

        log.debug("Found {} teams for user {}", userTeamsPage.getTotalElements(), actingUser.getUsername());
        return pagedResponseMapper.toPagedResponse(userTeamResponseDTOPage, UserTeamResponseDTO.class);
    }

    public PagedResponse<TeamMemberResponseDTO> getAllMembersFromTeam(Pageable pageable, Long teamId, User actingUser) {
        log.debug("User {} requesting members for team {}. Page: {}, Size: {}",
            actingUser.getUsername(), teamId, pageable.getPageNumber(), pageable.getPageSize());

        Team team = findTeamByIdOrThrow(teamId);
        teamAuthorizationService.checkTeamMembership(actingUser, team);

        Page<TeamMember> teamsMemberPage = teamMemberRepository.findByTeamAndAcceptedInviteTrue(team, pageable);
        Page<TeamMemberResponseDTO> teamMemberResponseDTOPage = teamsMemberPage.map(this::toTeamMemberResponseDTO);

        log.debug("Found {} members for team {}", teamsMemberPage.getTotalElements(), teamId);
        return pagedResponseMapper.toPagedResponse(teamMemberResponseDTOPage, TeamMemberResponseDTO.class);
    }

    @Transactional
    public void deleteTeam(Long teamId, User actingUser) {
        log.info("User {} attempting to delete team {}", actingUser.getUsername(), teamId);
        Team team = findTeamByIdOrThrow(teamId);
        teamAuthorizationService.checkTeamOwner(actingUser, team);

        log.debug("Deleting team: {}. Name: {}", teamId, team.getName());
        teamRepository.delete(team);
        log.info("Team {} successfully deleted by user {}", teamId, actingUser.getUsername());
    }

    @Transactional
    public void deleteMembershipFromTeam(Long teamId, Long userIdToDelete, User actingUser) {
        log.info("User {} attempting to remove user {} from team {}", actingUser.getUsername(), userIdToDelete, teamId);
        Team team = findTeamByIdOrThrow(teamId);
        teamAuthorizationService.checkTeamAdmin(actingUser, team);

        User userToDelete = findUserByIdOrThrow(userIdToDelete);
        TeamMember membershipToDelete = findMembershipByIdOrThrow(userToDelete, team);
        TeamMember actingMembership = findMembershipByIdOrThrow(actingUser, team);

        boolean actingUserIsOwner = actingMembership.getTeamRole().equals(ETeamRole.OWNER);
        boolean targetIsPrivileged = membershipToDelete.getTeamRole().isPrivileged();

        if (actingUserIsOwner && membershipToDelete.equals(actingMembership)) {
            log.warn("Owner {} attempted to remove themselves from team {}", actingUser.getUsername(), teamId);
            throw new AccessDeniedException("OWNER cannot remove themselves.");
        }

        if (!actingUserIsOwner && targetIsPrivileged) {
            log.warn("Non-owner user {} attempted to remove privileged member {} from team {}",
                actingUser.getUsername(), userToDelete.getUsername(), teamId);
            throw new AccessDeniedException("You cannot remove a member with role OWNER or ADMIN.");
        }

        log.debug("Removing member {} (role: {}) from team {}",
            userToDelete.getUsername(), membershipToDelete.getTeamRole(), teamId);
        teamMemberRepository.delete(membershipToDelete);
        log.info("User {} successfully removed from team {} by {}",
            userToDelete.getUsername(), teamId, actingUser.getUsername());
    }

    @Transactional
    public InviteLinkResponseDTO generateTeamInviteToken(Long teamID, User actingUser) {
        log.info("User {} requesting invitation link for team {}", actingUser.getUsername(), teamID);

        Team team = findTeamByIdOrThrow(teamID);
        teamAuthorizationService.checkTeamAdmin(actingUser, team);

        String token = UUID.randomUUID().toString();
        team.setInvitationToken(token);
        team.setInvitationTokenExpiry(LocalDateTime.now().plusHours(24));

        teamRepository.save(team);
        log.info("Invitation link generated for team {} by user {}. Expires: {}",
            teamID, actingUser.getUsername(), team.getInvitationTokenExpiry());

        return new InviteLinkResponseDTO(baseUrl + "/api/team/join?token=" + token,
            team.getInvitationTokenExpiry());
    }

    @Transactional
    public TeamMemberResponseDTO acceptTeamInvitationLink(String token, User actingUser) {
        log.info("User {} attempting to accept team invitation with token: {}", actingUser.getUsername(), token);

        if (token == null || token.isEmpty()) {
            log.warn("Attempt to accept invitation with null or empty token by user: {}", actingUser.getUsername());
            throw new IllegalArgumentException("Invitation token cannot be null or empty.");
        }

        Team team = findByInvitationTokenLinkOrThrow(token);

        if(team.getInvitationTokenExpiry().isBefore(LocalDateTime.now())) {
            log.warn("User {} attempted to accept expired invitation for team: {}", actingUser.getUsername(), team.getId());
            throw new ResourceNotFoundException("Invitation token has expired.");
        }

        if(teamMemberRepository.findByUserAndTeam(actingUser, team).isPresent()) {
            log.warn("User {} attempted to join team {} but is already a member", actingUser.getUsername(), team.getId());
            throw new ResourceAlreadyExistsException("User is already a member of this team.");
        }

        log.debug("Creating new membership for user {} in team {}", actingUser.getUsername(), team.getId());
        TeamMember newMember = new TeamMember(actingUser, team, ETeamRole.MEMBER);
        newMember.setAcceptedInvite(true);

        TeamMember savedMember = teamMemberRepository.save(newMember);
        log.info("User {} successfully joined team {} via invitation link", actingUser.getUsername(), team.getId());

        return toTeamMemberResponseDTO(savedMember);
    }

    private void validateTeamNameUniqueness(String name, User owner){
        if(teamRepository.existsByNameIgnoreCaseAndOwner(name, owner)) {
            throw new ResourceAlreadyExistsException("Team name already in use by this creator.");
        }
    }

    private Team findTeamByIdOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + teamId));
    }

    private Team findByInvitationTokenLinkOrThrow(String token) {
        return teamRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation."));
    }

    private User findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private User findByEmailIgnoreCaseOrThrow(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email"));
    }

    private TeamMember findMembershipByIdOrThrow(User user, Team team) {
        return teamMemberRepository.findByUserAndTeam(user, team)
                .orElseThrow(() -> new ResourceNotFoundException("User to update not found in this team."));
    }

    private TeamMember findByInvitationTokenEmailORThrow(String token) {
        return teamMemberRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation."));
    }

    private TeamMemberResponseDTO toTeamMemberResponseDTO(TeamMember membership) {
        TeamMemberResponseDTO dto = new TeamMemberResponseDTO();
        dto.setUserId(membership.getUser().getId());
        dto.setUsername(membership.getUser().getUsername());
        dto.setEmail(membership.getUser().getEmail());
        dto.setTeamRole(membership.getTeamRole());
        return dto;
    }

    private UserTeamResponseDTO toUserTeamResponseDTO(Team team, TeamMember membership) {
        UserTeamResponseDTO dto = new UserTeamResponseDTO();
        dto.setId(team.getId());
        dto.setName(team.getName());
        dto.setDescription(team.getDescription());
        dto.setTeamRole(membership.getTeamRole());
        return dto;
    }
}
