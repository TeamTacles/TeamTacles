package br.com.teamtacles.team.service;

import br.com.teamtacles.config.aop.BusinessActivityLog;
import br.com.teamtacles.infrastructure.email.EmailService;
import br.com.teamtacles.team.dto.request.*;
import br.com.teamtacles.common.dto.response.page.PagedResponse;
import br.com.teamtacles.common.dto.response.InviteLinkResponseDTO;
import br.com.teamtacles.team.dto.response.TeamMemberResponseDTO;
import br.com.teamtacles.team.dto.response.TeamResponseDTO;
import br.com.teamtacles.team.dto.response.UserTeamResponseDTO;
import br.com.teamtacles.team.enumeration.ETeamRole;
import br.com.teamtacles.common.exception.ResourceNotFoundException;
import br.com.teamtacles.common.mapper.PagedResponseMapper;
import br.com.teamtacles.team.model.Team;
import br.com.teamtacles.team.model.TeamMember;
import br.com.teamtacles.team.validator.*;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.team.repository.TeamMemberRepository;
import br.com.teamtacles.team.repository.TeamRepository;
import br.com.teamtacles.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TeamService {

    @Value("${app.base-url}")
    private String baseUrl;

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamAuthorizationService teamAuthorizationService;
    private final TeamNameUniquenessValidator teamNameUniquenessValidator;
    private final MembershipValidator membershipValidator;
    private final TeamTokenValidator teamTokenValidator;
    private final TeamMembershipActionValidator teamMembershipActionValidator;
    private final TeamInvitationValidator teamInvitationValidator;

    private final UserService userService;
    private final EmailService emailService;

    private final ModelMapper modelMapper;
    private final PagedResponseMapper pagedResponseMapper;

    public TeamService(TeamRepository teamRepository, TeamMemberRepository teamMemberRepository,
                       UserService userService, ModelMapper modelMapper,
                       TeamAuthorizationService teamAuthorizationService,
                       PagedResponseMapper pagedResponseMapper, EmailService emailService,
                       TeamNameUniquenessValidator teamNameUniquenessValidator,
                       MembershipValidator membershipValidator,
                       TeamTokenValidator teamTokenValidator,
                       TeamMembershipActionValidator teamMembershipActionValidator,
                       TeamInvitationValidator teamInvitationValidator) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userService = userService;
        this.modelMapper = modelMapper;
        this.teamAuthorizationService = teamAuthorizationService;
        this.pagedResponseMapper = pagedResponseMapper;
        this.emailService = emailService;
        this.teamNameUniquenessValidator = teamNameUniquenessValidator;
        this.membershipValidator = membershipValidator;
        this.teamTokenValidator = teamTokenValidator;
        this.teamMembershipActionValidator = teamMembershipActionValidator;
        this.teamInvitationValidator = teamInvitationValidator;
    }

    @BusinessActivityLog(action = "Create Team")
    @Transactional
    public TeamResponseDTO createTeam(TeamRequestRegisterDTO dto, User actingUser) {
        teamNameUniquenessValidator.validate(dto.getName(), actingUser);

        Team newTeam = new Team(dto.getName(), dto.getDescription(), actingUser);

        TeamMember creatorMembership = new TeamMember(actingUser, newTeam, ETeamRole.OWNER);
        creatorMembership.acceptedInvitation();
        newTeam.addMember(creatorMembership);

        Team savedTeam = teamRepository.save(newTeam);
        return modelMapper.map(savedTeam, TeamResponseDTO.class);
    }

    @BusinessActivityLog(action = "Update Team")
    @Transactional
    public TeamResponseDTO updateTeam(Long teamId, TeamRequestUpdateDTO dto, User actingUser) {
        Team team = findTeamByIdOrThrow(teamId);
        teamAuthorizationService.checkTeamOwner(actingUser, team);

        if(dto.getName() != null && !team.getName().equalsIgnoreCase(dto.getName())){
            teamNameUniquenessValidator.validate(dto.getName(), team.getOwner());
        }

        if(dto.getName() != null && !dto.getName().isEmpty()){
            team.setName(dto.getName());
        }

        if(dto.getDescription() != null){
            team.setDescription(dto.getDescription());
        }

        Team updatedTeam = teamRepository.save(team);
        return modelMapper.map(updatedTeam, TeamResponseDTO.class);
    }

    @BusinessActivityLog(action = "Update Team Member Role")
    @Transactional
    public TeamMemberResponseDTO updateMemberRole(Long teamId, Long userIdToUpdate, UpdateMemberRoleTeamRequestDTO dto, User actingUser) {
        Team team = findTeamByIdOrThrow(teamId);
        teamAuthorizationService.checkTeamAdmin(actingUser, team);

        User userToUpdate = userService.findUserEntityById(userIdToUpdate);
        TeamMember membershipToUpdate = findMembershipByIdOrThrow(userToUpdate, team);
        TeamMember actingMembership = findMembershipByIdOrThrow(actingUser, team);

        teamMembershipActionValidator.validateRoleUpdate(actingMembership, membershipToUpdate, dto.getNewRole());

        membershipToUpdate.changeRole(dto.getNewRole());
        TeamMember updatedMembership = teamMemberRepository.save(membershipToUpdate);

        return toTeamMemberResponseDTO(updatedMembership);
    }

    public TeamResponseDTO getTeamById(Long teamId, User actingUser) {
        Team team = findTeamByIdOrThrow(teamId);
        teamAuthorizationService.checkTeamMembership(actingUser, team);
        return modelMapper.map(team, TeamResponseDTO.class);
    }

    public PagedResponse<UserTeamResponseDTO> getAllTeamsByUser(Pageable pageable, TeamFilterDTO filter, User actingUser) {
        Page<Team> teamsPage = teamRepository.findTeamsByUserWithFilters(actingUser, filter, pageable);

        Page<UserTeamResponseDTO> userTeamResponseDTOPage = teamsPage.map(team -> {
            TeamMember membership = team.getMembers().stream()
                    .filter(m -> m.getUser().getId().equals(actingUser.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Data inconsistency: Team found without corresponding member."));

            return toUserTeamResponseDTO(team, membership);
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

    @BusinessActivityLog(action = "Invite Member to Team")
    @Transactional
    public void inviteMember(Long teamId, InvitedMemberRequestDTO dto, User actingUser) {
        Team team = findTeamByIdOrThrow(teamId);
        teamInvitationValidator.validateRole(dto.getRole());

        teamAuthorizationService.checkTeamAdmin(actingUser, team);
        User userToInvite = userService.findUserEntityByEmail(dto.getEmail());

        membershipValidator.validateNewMember(userToInvite, team);

        String token = UUID.randomUUID().toString();
        TeamMember newMember = new TeamMember(userToInvite, team, dto.getRole());
        newMember.generateInvitation(token, LocalDateTime.now().plusHours(24));

        team.addMember(newMember);
        teamRepository.save(team);
        emailService.sendTeamInvitationEmail(userToInvite.getEmail(), team.getName(), token);
    }

    @BusinessActivityLog(action = "Accept Team Invitation via Email")
    @Transactional
    public void acceptInvitation(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Invitation token cannot be null or empty.");
        }

        TeamMember membership = findByInvitationTokenEmailORThrow(token);
        teamTokenValidator.validateInvitationToken(membership);

        membership.acceptedInvitation();

        teamMemberRepository.save(membership);
    }

    @BusinessActivityLog(action = "Generate Team Invitation Link")
    @Transactional
    public InviteLinkResponseDTO generateTeamInviteToken(Long teamID, User actingUser) {
        Team team = findTeamByIdOrThrow(teamID);
        teamAuthorizationService.checkTeamAdmin(actingUser, team);

        String token = team.generateInviteLinkToken();

        teamRepository.save(team);
        return new InviteLinkResponseDTO(baseUrl + "/api/team/join?token=" + token,
            team.getInvitationTokenExpiry());
    }

    @BusinessActivityLog(action = "Accept Team Invitation via Link")
    @Transactional
    public TeamMemberResponseDTO acceptTeamInvitationLink(String token, User actingUser) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Invitation token cannot be null or empty.");
        }

        Team team = findByInvitationTokenLinkOrThrow(token);

        teamTokenValidator.validateInvitationLinkToken(team);
        membershipValidator.validateNewMember(actingUser, team);

        TeamMember newMember = new TeamMember(actingUser, team, ETeamRole.MEMBER);
        newMember.acceptedInvitation();

        team.addMember(newMember);
        teamRepository.save(team);

        return toTeamMemberResponseDTO(newMember);
    }

    @BusinessActivityLog(action = "Delete Team")
    @Transactional
    public void deleteTeam(Long teamId, User actingUser) {
        Team team = findTeamByIdOrThrow(teamId);
        teamAuthorizationService.checkTeamOwner(actingUser, team);
        teamRepository.delete(team);
    }

    @BusinessActivityLog(action = "Remove Member from Team")
    @Transactional
    public void deleteMembershipFromTeam(Long teamId, Long userIdToDelete, User actingUser) {
        Team team = findTeamByIdOrThrow(teamId);
        teamAuthorizationService.checkTeamAdmin(actingUser, team);

        User userToDelete = userService.findUserEntityById(userIdToDelete);
        TeamMember membershipToDelete = findMembershipByIdOrThrow(userToDelete, team);
        TeamMember actingMembership = findMembershipByIdOrThrow(actingUser, team);

        teamMembershipActionValidator.validateDeletion(actingMembership, membershipToDelete);

        team.removeMember(membershipToDelete);
        teamRepository.save(team);
    }

    public Team findTeamEntityById(Long teamId) {
        return findTeamByIdOrThrow(teamId);
    }

    private Team findTeamByIdOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + teamId));
    }

    private Team findByInvitationTokenLinkOrThrow(String token) {
        return teamRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation."));
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
