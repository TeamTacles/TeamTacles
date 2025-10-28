package br.com.teamtacles.project.repository;

import br.com.teamtacles.project.dto.request.ProjectFilterDTO;
import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Optional;


@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    boolean existsByTitleIgnoreCaseAndOwner(String title, User owner);
    Optional<Project> findByInvitationToken(String token);

    List<Project> findAllByOwner(User owner);

    @Query("SELECT DISTINCT p FROM Project p " +
            "JOIN FETCH p.members m " +
            "WHERE m.user = :user AND m.acceptedInvite = true " +
            "AND ( COALESCE(:#{#filter.title}, '') = '' OR LOWER(p.title) LIKE LOWER(CONCAT('%', :#{#filter.title}, '%')) ) " +
            "AND ( COALESCE(:#{#filter.createdAtAfter}, CAST(NULL AS date)) IS NULL OR CAST(p.createdAt AS date) >= :#{#filter.createdAtAfter} ) " +
            "AND ( COALESCE(:#{#filter.createdAtBefore}, CAST(NULL AS date)) IS NULL OR CAST(p.createdAt AS date) <= :#{#filter.createdAtBefore} )")
    Page<Project> findProjectsByUserWithFilters(@Param("user") User user, @Param("filter") ProjectFilterDTO filter, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p " +
            "LEFT JOIN FETCH p.members mem " +
            "LEFT JOIN FETCH mem.user u " +
            "WHERE p.id = :projectId " +
            "AND (:userId IS NULL OR mem.user.id = :userId)")
    Optional<Project> findProjectByIdForReport(@Param("projectId") Long projectId, @Param("userId") Long userId);


    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.members LEFT JOIN FETCH p.tasks WHERE p.id = :projectId")
    Optional<Project> findByIdWithMembersAndTasks(@Param("projectId") Long projectId);

}
