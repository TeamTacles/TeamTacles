package br.com.teamtacles.team.validator;

import br.com.teamtacles.common.exception.ResourceAlreadyExistsException;
import br.com.teamtacles.team.repository.TeamRepository;
import br.com.teamtacles.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class TeamNameUniquenessValidator {
     private final TeamRepository teamRepository;

     public TeamNameUniquenessValidator(TeamRepository teamRepository) {
         this.teamRepository = teamRepository;
     }

     public void validate(String teamName, User owner) {
         if(teamRepository.existsByNameIgnoreCaseAndOwner(teamName, owner)) {
             throw new ResourceAlreadyExistsException("Team name already in use by this creator.");
         }
     }
}
