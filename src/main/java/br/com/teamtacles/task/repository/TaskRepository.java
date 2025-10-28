package br.com.teamtacles.task.repository;

import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.task.dto.request.TaskFilterReportDTO;
import br.com.teamtacles.task.model.Task;
import br.com.teamtacles.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import br.com.teamtacles.task.enumeration.ETaskStatus;


public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findByProject(Pageable pageable, Project project);
    long countByProject(Project project);
    List<Task> findAllByOwner(User owner);

    @Query("SELECT DISTINCT t FROM Task t JOIN t.assignments a " +
            "LEFT JOIN FETCH t.project p " +
            "WHERE a.user.id = :userId " +
            "AND ( COALESCE(:#{#filter.title}, '') = '' OR LOWER(t.title) LIKE LOWER(CONCAT('%', :#{#filter.title}, '%')) ) " +
            "AND ( COALESCE(:#{#filter.status}, CAST(NULL AS br.com.teamtacles.task.enumeration.ETaskStatus)) IS NULL OR t.status = :#{#filter.status} ) " +
            "AND ( COALESCE(:#{#filter.isOverdue}, CAST(NULL AS java.lang.Boolean)) IS NULL OR :#{#filter.isOverdue} = false " +
            "OR  ( :#{#filter.isOverdue} = true AND t.status != br.com.teamtacles.task.enumeration.ETaskStatus.DONE AND t.dueDate IS NOT NULL AND t.dueDate < CURRENT_TIMESTAMP) ) " +
            "AND ( COALESCE(:#{#filter.dueDateAfter}, CAST(NULL AS date)) IS NULL OR CAST(t.dueDate AS date) >= :#{#filter.dueDateAfter} ) " +
            "AND ( COALESCE(:#{#filter.dueDateBefore}, CAST(NULL AS date)) IS NULL OR CAST(t.dueDate AS date) <= :#{#filter.dueDateBefore} ) " +
            "AND ( COALESCE(:#{#filter.conclusionDateAfter}, CAST(NULL AS date)) IS NULL OR CAST(t.completedAt AS date) >= :#{#filter.conclusionDateAfter} ) " +
            "AND ( COALESCE(:#{#filter.conclusionDateBefore}, CAST(NULL AS date)) IS NULL OR CAST(t.completedAt AS date) <= :#{#filter.conclusionDateBefore} ) " +
            "AND ( COALESCE(:#{#filter.createdAtAfter}, CAST(NULL AS date)) IS NULL OR CAST(t.createdAt AS date) >= :#{#filter.createdAtAfter} ) " +
            "AND ( COALESCE(:#{#filter.createdAtBefore}, CAST(NULL AS date)) IS NULL OR CAST(t.createdAt AS date) <= :#{#filter.createdAtBefore} )")
    Page<Task> findTasksByUserWithFilters(@Param("userId") Long userId, @Param("filter") TaskFilterReportDTO filter, Pageable pageable);

    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN t.assignments a WHERE t.project.id = :projectId " +
            "AND ( COALESCE(:#{#filter.title}, '') = '' OR LOWER(t.title) LIKE LOWER(CONCAT('%', :#{#filter.title}, '%')) ) " +
            "AND ( COALESCE(:#{#filter.status}, CAST(NULL AS br.com.teamtacles.task.enumeration.ETaskStatus)) IS NULL OR t.status = :#{#filter.status} ) " +
            "AND ( COALESCE(:#{#filter.assignedUserId}, CAST(NULL AS java.lang.Long)) IS NULL OR a.user.id = :#{#filter.assignedUserId} ) " +
            "AND ( COALESCE(:#{#filter.isOverdue}, CAST(NULL AS java.lang.Boolean)) IS NULL OR :#{#filter.isOverdue} = false " +
            "OR  ( :#{#filter.isOverdue} = true AND t.status != br.com.teamtacles.task.enumeration.ETaskStatus.DONE AND t.dueDate IS NOT NULL AND t.dueDate < CURRENT_TIMESTAMP) ) " +
            "AND ( COALESCE(:#{#filter.dueDateAfter}, CAST(NULL AS date)) IS NULL OR CAST(t.dueDate AS date) >= :#{#filter.dueDateAfter} ) " +
            "AND ( COALESCE(:#{#filter.dueDateBefore}, CAST(NULL AS date)) IS NULL OR CAST(t.dueDate AS date) <= :#{#filter.dueDateBefore} ) " +
            "AND ( COALESCE(:#{#filter.conclusionDateAfter}, CAST(NULL AS date)) IS NULL OR CAST(t.completedAt AS date) >= :#{#filter.conclusionDateAfter} ) " +
            "AND ( COALESCE(:#{#filter.conclusionDateBefore}, CAST(NULL AS date)) IS NULL OR CAST(t.completedAt AS date) <= :#{#filter.conclusionDateBefore} ) " +
            "AND ( COALESCE(:#{#filter.createdAtAfter}, CAST(NULL AS date)) IS NULL OR CAST(t.createdAt AS date) >= :#{#filter.createdAtAfter} ) " +
            "AND ( COALESCE(:#{#filter.createdAtBefore}, CAST(NULL AS date)) IS NULL OR CAST(t.createdAt AS date) <= :#{#filter.createdAtBefore} )")
    Page<Task> findTasksByProjectWithFilters(@Param("projectId") Long projectId, @Param("filter") TaskFilterReportDTO filter, Pageable pageable);

    @Query("SELECT DISTINCT t FROM Task t " +
            "LEFT JOIN FETCH t.assignments a " +
            "LEFT JOIN FETCH a.user u " +
            "WHERE t.project.id = :projectId " +
            "AND (COALESCE(:#{#filter.status}, CAST(NULL AS br.com.teamtacles.task.enumeration.ETaskStatus)) IS NULL OR t.status = :#{#filter.status}) " +
            "AND ( COALESCE(:#{#filter.isOverdue}, CAST(NULL AS java.lang.Boolean)) IS NULL OR :#{#filter.isOverdue} = false " +
            "OR  ( :#{#filter.isOverdue} = true AND t.status != br.com.teamtacles.task.enumeration.ETaskStatus.DONE AND t.dueDate IS NOT NULL AND t.dueDate < CURRENT_TIMESTAMP) ) " +
            "AND (COALESCE(:#{#filter.assignedUserId}, CAST(NULL AS java.lang.Long)) IS NULL OR EXISTS (SELECT 1 FROM TaskAssignment ta WHERE ta.task = t AND ta.user.id = :#{#filter.assignedUserId})) " +
            "AND (COALESCE(:#{#filter.updatedAtAfter}, CAST(NULL AS date)) IS NULL OR CAST(t.updatedAt AS date) >= :#{#filter.updatedAtAfter}) " +
            "AND (COALESCE(:#{#filter.updatedAtBefore}, CAST(NULL AS date)) IS NULL OR CAST(t.updatedAt AS date) <= :#{#filter.updatedAtBefore})")
    Set<Task> findTasksByProjectWithFiltersForReport(@Param("projectId") Long projectId, @Param("filter") TaskFilterReportDTO filter);

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.assignments WHERE t.project.id = :projectId AND t.status = :status")
    List<Task> findAllByProjectIdAndStatusWithAssignments(@Param("projectId") Long projectId, @Param("status") ETaskStatus status);
}

