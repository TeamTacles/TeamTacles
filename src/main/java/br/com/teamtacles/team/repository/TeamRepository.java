package br.com.teamtacles.team.repository;

import br.com.teamtacles.team.model.Team;
import br.com.teamtacles.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    boolean existsByNameIgnoreCaseAndOwner(String name, User owner);
    Optional<Team> findByInvitationToken(String token);
}
