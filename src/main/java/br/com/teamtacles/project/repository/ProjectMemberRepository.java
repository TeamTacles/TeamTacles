package br.com.teamtacles.project.repository;

import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.project.model.ProjectMember;
import br.com.teamtacles.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    Optional<ProjectMember> findByUserAndProject(User user, Project project);
    Page<ProjectMember> findByUserAndAcceptedInviteTrue(User user, Pageable pageable);
    Page<ProjectMember> findByProjectAndAcceptedInviteTrue(Project project, Pageable pageable);
    Optional<ProjectMember> findByInvitationToken(String token);
    long countByProjectAndAcceptedInviteTrue(Project project);

    @Query("SELECT pm.user FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.user.id IN :userIds AND pm.acceptedInvite = true")
    Set<User> findProjectMembersAsUsers(@Param("projectId") Long projectId, @Param("userIds") List<Long> userIds);

    /**
     * Verifica de forma eficiente se uma associação de membro existe
     * para um usuário e projeto específicos.
     * Isso é muito mais performático do que carregar a coleção 'project.getMembers()'.
     *
     * @param user O Usuário a ser verificado.
     * @param project O Projeto a ser verificado.
     * @return true se o membro já existe, false caso contrário.
     */
    boolean existsByUserAndProject(User user, Project project);

}