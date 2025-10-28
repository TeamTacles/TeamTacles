package br.com.teamtacles.team.repository;

import br.com.teamtacles.team.dto.request.TeamFilterDTO;
import br.com.teamtacles.team.model.Team;
import br.com.teamtacles.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    boolean existsByNameIgnoreCaseAndOwner(String name, User owner);
    Optional<Team> findByInvitationToken(String token);
    List<Team> findAllByOwner(User owner);

    @Query("SELECT DISTINCT t FROM Team t " +
            "JOIN FETCH t.members m " +
            "WHERE m.user = :user AND m.acceptedInvite = true " +
            "AND ( COALESCE(:#{#filter.name}, '') = '' OR LOWER(t.name) LIKE LOWER(CONCAT('%', :#{#filter.name}, '%')) ) " +
            "AND ( COALESCE(:#{#filter.createdAtAfter}, CAST(NULL AS date)) IS NULL OR CAST(t.createdAt AS date) >= :#{#filter.createdAtAfter} ) " +
            "AND ( COALESCE(:#{#filter.createdAtBefore}, CAST(NULL AS date)) IS NULL OR CAST(t.createdAt AS date) <= :#{#filter.createdAtBefore} )")
    Page<Team> findTeamsByUserWithFilters(@Param("user") User user, @Param("filter") TeamFilterDTO filter, Pageable pageable);
}
