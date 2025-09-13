package br.com.teamtacles.project.repository;

import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.project.model.ProjectMember;
import br.com.teamtacles.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    Optional<ProjectMember> findByUserAndProject(User user, Project project);
    Page<ProjectMember> findByUserAndAcceptedInviteTrue(User user, Pageable pageable);
    Page<ProjectMember> findByProjectAndAcceptedInviteTrue(Project project, Pageable pageable);
    Optional<ProjectMember> findByInvitationToken(String token);
}
