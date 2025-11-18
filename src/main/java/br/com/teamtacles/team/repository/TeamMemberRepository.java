package br.com.teamtacles.team.repository;

import br.com.teamtacles.team.model.Team;
import br.com.teamtacles.team.model.TeamMember;
import br.com.teamtacles.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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

    @Query("SELECT tm FROM TeamMember tm JOIN FETCH tm.user WHERE tm.team.id = :teamId AND tm.acceptedInvite = true")
    List<TeamMember> findAcceptedByTeamIdWithUser(@Param("teamId") Long teamId);

}