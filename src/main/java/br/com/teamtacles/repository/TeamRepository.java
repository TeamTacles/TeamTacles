package br.com.teamtacles.repository;

import br.com.teamtacles.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
}
