package br.com.teamtacles.team.service;

import br.com.teamtacles.team.enumeration.ETeamRole;
import br.com.teamtacles.team.model.Team;
import br.com.teamtacles.team.model.TeamMember;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.team.repository.TeamMemberRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class TeamAuthorizationService  {

    private final TeamMemberRepository teamMemberRepository;

    public TeamAuthorizationService(TeamMemberRepository teamMemberRepository) {
        this.teamMemberRepository = teamMemberRepository;
    }

    public boolean isMember(User user, Team team) {
        return teamMemberRepository.findByUserAndTeam(user, team).isPresent();
    }

    public boolean isAdmin(User user, Team team) {
        return teamMemberRepository.findByUserAndTeam(user, team)
                .map(member -> member.getTeamRole() == ETeamRole.ADMIN || member.getTeamRole() == ETeamRole.OWNER)
                .orElse(false);
    }

    public boolean isOwner(User user, Team team) {
        return team.getOwner().equals(user);
    }

    public void checkTeamMembership(User user, Team team) {
        if (!isMember(user, team)) {
            throw new AccessDeniedException("Access denied. You are not a member of this team.");
        }
    }

    public void checkTeamAdmin(User user, Team team) {
        if (!isAdmin(user, team)) {
            throw new AccessDeniedException("Permission denied. Action requires ADMIN or OWNER role.");
        }
    }

    public void checkTeamOwner(User user, Team team) {
        if (!isOwner(user, team)) {
            throw new AccessDeniedException("Permission denied. Action requires OWNER role.");
        }
    }

    private TeamMember findMembershipOrThrow(User user, Team team) {
        return teamMemberRepository.findByUserAndTeam(user, team)
                .orElseThrow(() -> new AccessDeniedException("Access denied. User is not a member of this team."));
    }
}