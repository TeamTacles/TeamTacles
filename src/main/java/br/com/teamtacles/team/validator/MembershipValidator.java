package br.com.teamtacles.team.validator;

import br.com.teamtacles.common.exception.ResourceAlreadyExistsException;
import br.com.teamtacles.team.model.Team;
import br.com.teamtacles.team.repository.TeamMemberRepository;
import br.com.teamtacles.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class MembershipValidator {
    private final TeamMemberRepository teamMemberRepository;

    public MembershipValidator(TeamMemberRepository teamMemberRepository) {
        this.teamMemberRepository = teamMemberRepository;
    }

    public void validateNewMember(User user, Team team) {
        if (teamMemberRepository.findByUserAndTeam(user, team).isPresent()) {
            throw new ResourceAlreadyExistsException("User is already a member of this team.");
        }
    }
}
