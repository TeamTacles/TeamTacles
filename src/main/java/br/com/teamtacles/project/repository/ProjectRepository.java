package br.com.teamtacles.project.repository;

import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    boolean existsByTitleIgnoreCaseAndOwner(String title, User owner);
}
