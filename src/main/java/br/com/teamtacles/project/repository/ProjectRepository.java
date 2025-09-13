package br.com.teamtacles.project.repository;

import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    boolean existsByTitleIgnoreCaseAndOwner(String title, User owner);
    Optional<Project> findByInvitationToken(String token);
}
