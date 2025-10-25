package br.com.teamtacles.team.repository;

import br.com.teamtacles.team.model.Team;
import br.com.teamtacles.team.model.TeamMember;
import br.com.teamtacles.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    Optional<TeamMember> findByUserAndTeam(User user, Team team);
    Page<TeamMember> findByUserAndAcceptedInviteTrue(User user, Pageable pageable);
    Page<TeamMember> findByTeamAndAcceptedInviteTrue(Team team, Pageable pageable);
    Page<TeamMember> findByUser(User user, Pageable pageable);
    Optional<TeamMember> findByInvitationToken(String token);
    long countByTeamAndAcceptedInviteTrue(Team team);

    /**
     * Busca explicitamente todos os membros de um time pelo ID do time,
     * e também carrega (FETCH) a entidade 'User' associada a cada membro
     * para evitar LazyInitializationException no serviço.
     *
     * @param teamId O ID do time
     * @return Uma lista de TeamMember com seus respectivos Users pré-carregados.
     */
    @Query("SELECT tm FROM TeamMember tm JOIN FETCH tm.user WHERE tm.team.id = :teamId")
    List<TeamMember> findByTeamIdWithUser(@Param("teamId") Long teamId);

}