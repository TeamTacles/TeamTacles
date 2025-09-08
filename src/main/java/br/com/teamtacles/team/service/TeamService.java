package br.com.teamtacles.team.service;

import br.com.teamtacles.common.service.EmailService;
import br.com.teamtacles.team.dto.request.InvitedMemberRequestDTO;
import br.com.teamtacles.team.dto.request.TeamRequestRegisterDTO;
import br.com.teamtacles.team.dto.request.TeamRequestUpdateDTO;
import br.com.teamtacles.team.dto.request.UpdateMemberRoleRequestDTO;
import br.com.teamtacles.common.dto.page.PagedResponse;
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
    public TeamResponseDTO createTeam(TeamRequestRegisterDTO dto, User creator) {
        validateProjectNameUniqueness(dto.getName(), creator);

        Team newTeam = modelMapper.map(dto, Team.class);
        newTeam.setOwner(creator);

        TeamMember creatorMembership = new TeamMember(creator, newTeam, ETeamRole.OWNER);
        creatorMembership.setAcceptedInvite(true);
        newTeam.getMembers().add(creatorMembership);

        Team savedTeam = teamRepository.save(newTeam);
        return modelMapper.map(savedTeam, TeamResponseDTO.class);
    }

    @Transactional
    public void inviteMember(Long teamId, InvitedMemberRequestDTO dto, User invitingUser) {
        Team team = findTeamByIdOrThrow(teamId);

        if(dto.getRole().equals(ETeamRole.OWNER)) {
            throw new IllegalArgumentException("Cannot invite a user to be OWNER.");
        }

        teamAuthorizationService.checkTeamAdmin(invitingUser, team);

        User userToInvite = userRepository.findByEmailIgnoreCase(dto.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email"));

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
        TeamMember membership = teamMemberRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation token."));

        if(membership.getInvitationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ResourceNotFoundException("Invitation token has expired.");
        }

        membership.setAcceptedInvite(true);
        membership.setInvitationToken(null);
        membership.setInvitationTokenExpiry(null);

        teamMemberRepository.save(membership);
    }

    @Transactional
    public TeamResponseDTO updateTeam(Long teamId, TeamRequestUpdateDTO dto, User user) {
        Team team = findTeamByIdOrThrow(teamId);
        teamAuthorizationService.checkTeamOwner(user, team);

        if(!team.getName().equalsIgnoreCase(dto.getName())){
            validateProjectNameUniqueness(dto.getName(), team.getOwner());
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
        return modelMapper.map(teamMemberRepository.save(membershipToUpdate), TeamMemberResponseDTO.class);
    }

    public TeamResponseDTO getTeamById(Long teamId, User user) {
        Team team = findTeamByIdOrThrow(teamId);
        teamAuthorizationService.checkTeamMembership(user, team);
        return modelMapper.map(team, TeamResponseDTO.class);
    }

    public PagedResponse<UserTeamResponseDTO> getAllTeamsByUser(Pageable pageable, User user) {
        Page<TeamMember> teamsMemberPage = teamMemberRepository.findByUserAndAcceptedInviteTrue(user, pageable);

        Page<UserTeamResponseDTO> userTeamResponseDTOPage = teamsMemberPage.map(membership -> {
            UserTeamResponseDTO dto = new UserTeamResponseDTO();
            Team team = membership.getTeam();

            dto.setId(team.getId());
            dto.setName(team.getName());
            dto.setDescription(team.getDescription());
            dto.setTeamRole(membership.getTeamRole());

            return dto;
        });

        return pagedResponseMapper.toPagedResponse(userTeamResponseDTOPage, UserTeamResponseDTO.class);
    }

    @Transactional
    public void deleteTeam(Long teamId, User user) {
        Team team = findTeamByIdOrThrow(teamId);
        teamAuthorizationService.checkTeamOwner(user, team);
        teamRepository.delete(team);
    }

    private void validateProjectNameUniqueness(String name, User creator){
        teamRepository.findByNameIgnoreCaseAndOwner(name, creator).ifPresent(t -> {
            throw new ResourceAlreadyExistsException("Team name already in use by this creator.");
        });
    }

    private Team findTeamByIdOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + teamId));
    }

    private User findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private TeamMember findMembershipByIdOrThrow(User userToUpdate, Team team) {
        return teamMemberRepository.findByUserAndTeam(userToUpdate, team)
                .orElseThrow(() -> new ResourceNotFoundException("User to update not found in this team."));
    }
}