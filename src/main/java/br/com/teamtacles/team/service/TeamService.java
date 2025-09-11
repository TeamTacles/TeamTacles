package br.com.teamtacles.team.service;

import br.com.teamtacles.common.service.EmailService;
import br.com.teamtacles.team.dto.request.InvitedMemberRequestDTO;
import br.com.teamtacles.team.dto.request.TeamRequestRegisterDTO;
import br.com.teamtacles.team.dto.request.TeamRequestUpdateDTO;
import br.com.teamtacles.team.dto.request.UpdateMemberRoleRequestDTO;
import br.com.teamtacles.common.dto.page.PagedResponse;
import br.com.teamtacles.team.dto.response.InviteLinkResponseDTO;
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

@Service
public class TeamService {

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
        validateTeamNameUniqueness(dto.getName(), actingUser);

        Team newTeam = modelMapper.map(dto, Team.class);
        newTeam.setOwner(actingUser);

        TeamMember creatorMembership = new TeamMember(actingUser, newTeam, ETeamRole.OWNER);
        creatorMembership.setAcceptedInvite(true);
        newTeam.getMembers().add(creatorMembership);

        Team savedTeam = teamRepository.save(newTeam);
        return modelMapper.map(savedTeam, TeamResponseDTO.class);
    }

    @Transactional
    public void inviteMember(Long teamId, InvitedMemberRequestDTO dto, User actingUser) {
        Team team = findTeamByIdOrThrow(teamId);

        if(dto.getRole().equals(ETeamRole.OWNER)) {
            throw new IllegalArgumentException("Cannot invite a user to be OWNER.");
        }

        teamAuthorizationService.checkTeamAdmin(actingUser, team);

        User userToInvite = findByEmailIgnoreCaseOrThrow(dto.getEmail());

        if (teamMemberRepository.findByUserAndTeam(userToInvite, team).isPresent()) {
            throw new ResourceAlreadyExistsException("User is already a member of this team.");
        }

        String token = UUID.randomUUID().toString();
        TeamMember newMember = new TeamMember(userToInvite, team, dto.getRole());
        newMember.setInvitationToken(token);
        newMember.setInvitationTokenExpiry(LocalDateTime.now().plusHours(24));

        teamMemberRepository.save(newMember);

        emailService.sendTeamInvitationEmail(userToInvite.getEmail(), team.getName(), token);
    }

    @Transactional
    public void acceptInvitation(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Invitation token cannot be null or empty.");
        }

        TeamMember membership = findByInvitationTokenORThrow(token);

        if(membership.getInvitationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ResourceNotFoundException("Invitation token has expired.");
        }

        membership.setAcceptedInvite(true);
        membership.setInvitationToken(null);
        membership.setInvitationTokenExpiry(null);

        teamMemberRepository.save(membership);
    }

    @Transactional
    public TeamResponseDTO updateTeam(Long teamId, TeamRequestUpdateDTO dto, User actingUser) {
        Team team = findTeamByIdOrThrow(teamId);
        teamAuthorizationService.checkTeamOwner(actingUser, team);

        if(!team.getName().equalsIgnoreCase(dto.getName())){
            validateTeamNameUniqueness(dto.getName(), team.getOwner());
        }

        if(dto.getName() != null && !dto.getName().isEmpty()){
            team.setName(dto.getName());
        }

        if(dto.getDescription() != null && !dto.getDescription().isEmpty()){
            team.setDescription(dto.getDescription());
        }

        Team updatedTeam = teamRepository.save(team);
        return modelMapper.map(updatedTeam, TeamResponseDTO.class);
    }

    @Transactional
    public TeamMemberResponseDTO updateMemberRole(Long teamId, Long userIdToUpdate, UpdateMemberRoleRequestDTO dto, User actingUser) {
        Team team = findTeamByIdOrThrow(teamId);
        teamAuthorizationService.checkTeamAdmin(actingUser, team);

        User userToUpdate = findUserByIdOrThrow(userIdToUpdate);
        TeamMember membershipToUpdate = findMembershipByIdOrThrow(userToUpdate, team);

        if (membershipToUpdate.getTeamRole().equals(ETeamRole.OWNER)) {
            throw new AccessDeniedException("The team OWNER's role cannot be changed.");
        }

        if (membershipToUpdate.getTeamRole().equals(ETeamRole.ADMIN)) {
            teamAuthorizationService.checkTeamOwner(actingUser, team);
        }

        if (dto.getNewRole().equals(ETeamRole.OWNER)) {
            throw new IllegalArgumentException("Cannot promote a user to OWNER.");
        }

        membershipToUpdate.setTeamRole(dto.getNewRole());
        TeamMember updatedMembership = teamMemberRepository.save(membershipToUpdate);

        return toTeamMemberResponseDTO(updatedMembership);
    }

    public TeamResponseDTO getTeamById(Long teamId, User actingUser) {
        Team team = findTeamByIdOrThrow(teamId);
        teamAuthorizationService.checkTeamMembership(actingUser, team);
        return modelMapper.map(team, TeamResponseDTO.class);
    }

    public PagedResponse<UserTeamResponseDTO> getAllTeamsByUser(Pageable pageable, User actingUser) {
        Page<TeamMember> userTeamsPage = teamMemberRepository.findByUserAndAcceptedInviteTrue(actingUser, pageable);

        Page<UserTeamResponseDTO> userTeamResponseDTOPage = userTeamsPage.map(membership -> {
            return toUserTeamResponseDTO(membership.getTeam(), membership);
        });

        return pagedResponseMapper.toPagedResponse(userTeamResponseDTOPage, UserTeamResponseDTO.class);
    }

    public PagedResponse<TeamMemberResponseDTO> getAllMembersFromTeam(Pageable pageable, Long teamId, User actingUser) {
        Team team = findTeamByIdOrThrow(teamId);
        teamAuthorizationService.checkTeamMembership(actingUser, team);

        Page<TeamMember> teamsMemberPage = teamMemberRepository.findByTeamAndAcceptedInviteTrue(team, pageable);

        Page<TeamMemberResponseDTO> teamMemberResponseDTOPage = teamsMemberPage.map(this::toTeamMemberResponseDTO);

        return pagedResponseMapper.toPagedResponse(teamMemberResponseDTOPage, TeamMemberResponseDTO.class);
    }

    @Transactional
    public void deleteTeam(Long teamId, User actingUser) {
        Team team = findTeamByIdOrThrow(teamId);
        teamAuthorizationService.checkTeamOwner(actingUser, team);
        teamRepository.delete(team);
    }

    @Transactional
    public void deleteMembershipFromTeam(Long teamId, Long userIdToDelete, User actingUser) {
        Team team = findTeamByIdOrThrow(teamId);
        teamAuthorizationService.checkTeamAdmin(actingUser, team);

        User userToDelete = findUserByIdOrThrow(userIdToDelete);
        TeamMember membershipToDelete = findMembershipByIdOrThrow(userToDelete, team);
        TeamMember actingMembership = findMembershipByIdOrThrow(actingUser, team);

        boolean actingUserIsOwner = actingMembership.getTeamRole().equals(ETeamRole.OWNER);
        boolean targetIsPrivileged = membershipToDelete.getTeamRole().isPrivileged();

        if (actingUserIsOwner && membershipToDelete.equals(actingMembership)) {
            throw new AccessDeniedException("OWNER cannot remove themselves.");
        }

        if (!actingUserIsOwner && targetIsPrivileged) {
            throw new AccessDeniedException("You cannot remove a member with role OWNER or ADMIN.");
        }

        teamMemberRepository.delete(membershipToDelete);
    }

    @Transactional
    public InviteLinkResponseDTO generateTeamInviteToken(Long teamID, User actingUser) {
        Team team = findTeamByIdOrThrow(teamID);
        teamAuthorizationService.checkTeamAdmin(actingUser, team);

        String token = UUID.randomUUID().toString();
        team.setInvitationToken(token);
        team.setInvitationTokenExpiry(LocalDateTime.now().plusHours(24));

        teamRepository.save(team);

        return new InviteLinkResponseDTO(baseUrl + "/api/team/join?token=" + token, team.getInvitationTokenExpiry());
    }

    @Transactional
    public TeamMemberResponseDTO acceptTeamInvitationLink(String token, User actingUser) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Invitation token cannot be null or empty.");
        }

        Team team = findByInvitationTokenOrThrow(token);

        if(team.getInvitationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ResourceNotFoundException("Invitation token has expired.");
        }

        if(teamMemberRepository.findByUserAndTeam(actingUser, team).isPresent()) {
            throw new ResourceAlreadyExistsException("User is already a member of this team.");
        }

        TeamMember newMember = new TeamMember(actingUser, team, ETeamRole.MEMBER);
        newMember.setAcceptedInvite(true);

        return toTeamMemberResponseDTO(teamMemberRepository.save(newMember));
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

    private Team findByInvitationTokenOrThrow(String token) {
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

    private TeamMember findMembershipByIdOrThrow(User userToUpdate, Team team) {
        return teamMemberRepository.findByUserAndTeam(userToUpdate, team)
                .orElseThrow(() -> new ResourceNotFoundException("User to update not found in this team."));
    }

    private TeamMember findByInvitationTokenORThrow(String token) {
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