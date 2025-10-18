package br.com.teamtacles.team.validator;

import br.com.teamtacles.common.exception.ResourceAlreadyExistsException;
import br.com.teamtacles.team.model.Team;
import br.com.teamtacles.team.model.TeamMember;
import br.com.teamtacles.team.repository.TeamMemberRepository;
import br.com.teamtacles.user.model.User;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TeamMembershipValidator {
    private final TeamMemberRepository teamMemberRepository;

    public TeamMembershipValidator(TeamMemberRepository teamMemberRepository) {
        this.teamMemberRepository = teamMemberRepository;
    }

    public void validateNewMember(User user, Team team) {
        Optional<TeamMember> existingMembership = teamMemberRepository.findByUserAndTeam(user, team);

        if (existingMembership.isPresent() && existingMembership.get().isAcceptedInvite()) {
            throw new ResourceAlreadyExistsException("User is already a member of this team.");
        }
    }
}