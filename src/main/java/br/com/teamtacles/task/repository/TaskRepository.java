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
            "AND ( :#{#filter.title} IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :#{#filter.title}, '%')) ) " +
            "AND ( :#{#filter.status} IS NULL OR t.status = :#{#filter.status} ) " +
            "AND ( :#{#filter.isOverdue} IS NULL OR :#{#filter.isOverdue} = false " +
            "OR  ( :#{#filter.isOverdue} = true AND t.status != br.com.teamtacles.task.enumeration.ETaskStatus.DONE AND t.dueDate IS NOT NULL AND t.dueDate < CURRENT_TIMESTAMP) ) " +
            "AND ( :#{#filter.dueDateAfter} IS NULL OR CAST(t.dueDate AS date) >= :#{#filter.dueDateAfter} ) " +
            "AND ( :#{#filter.dueDateBefore} IS NULL OR CAST(t.dueDate AS date) <= :#{#filter.dueDateBefore} ) " +
            "AND ( :#{#filter.conclusionDateAfter} IS NULL OR CAST(t.completedAt AS date) >= :#{#filter.conclusionDateAfter} ) " +
            "AND ( :#{#filter.conclusionDateBefore} IS NULL OR CAST(t.completedAt AS date) <= :#{#filter.conclusionDateBefore} ) " +
            "AND ( :#{#filter.createdAtAfter} IS NULL OR CAST(t.createdAt AS date) >= :#{#filter.createdAtAfter} ) " +
            "AND ( :#{#filter.createdAtBefore} IS NULL OR CAST(t.createdAt AS date) <= :#{#filter.createdAtBefore} )")
    Page<Task> findTasksByUserWithFilters(@Param("userId") Long userId, @Param("filter") TaskFilterReportDTO filter, Pageable pageable);

    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN t.assignments a WHERE t.project.id = :projectId " + // Adicionado DISTINCT e LEFT JOIN
            "AND ( :#{#filter.title} IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :#{#filter.title}, '%')) ) " +
            "AND ( :#{#filter.status} IS NULL OR t.status = :#{#filter.status} ) " +
            "AND ( :#{#filter.assignedUserId} IS NULL OR a.user.id = :#{#filter.assignedUserId} ) " + // <-- LINHA ADICIONADA PARA FILTRAR POR MEMBRO
            "AND ( :#{#filter.isOverdue} IS NULL OR :#{#filter.isOverdue} = false " +
            "OR  ( :#{#filter.isOverdue} = true AND t.status != br.com.teamtacles.task.enumeration.ETaskStatus.DONE AND t.dueDate IS NOT NULL AND t.dueDate < CURRENT_TIMESTAMP) ) " +
            "AND ( :#{#filter.dueDateAfter} IS NULL OR CAST(t.dueDate AS date) >= :#{#filter.dueDateAfter} ) " +
            "AND ( :#{#filter.dueDateBefore} IS NULL OR CAST(t.dueDate AS date) <= :#{#filter.dueDateBefore} ) " +
            "AND ( :#{#filter.conclusionDateAfter} IS NULL OR CAST(t.completedAt AS date) >= :#{#filter.conclusionDateAfter} ) " +
            "AND ( :#{#filter.conclusionDateBefore} IS NULL OR CAST(t.completedAt AS date) <= :#{#filter.conclusionDateBefore} ) " +
            "AND ( :#{#filter.createdAtAfter} IS NULL OR CAST(t.createdAt AS date) >= :#{#filter.createdAtAfter} ) " +
            "AND ( :#{#filter.createdAtBefore} IS NULL OR CAST(t.createdAt AS date) <= :#{#filter.createdAtBefore} )")
    Page<Task> findTasksByProjectWithFilters(@Param("projectId") Long projectId, @Param("filter") TaskFilterReportDTO filter, Pageable pageable);
    
    @Query("SELECT DISTINCT t FROM Task t " +
            "LEFT JOIN FETCH t.assignments a " +
            "LEFT JOIN FETCH a.user u " +
            "WHERE t.project.id = :projectId " +
            "AND (:#{#filter.status} IS NULL OR t.status = :#{#filter.status}) " +
            "AND ( :#{#filter.isOverdue} IS NULL OR :#{#filter.isOverdue} = false " +
            "OR  ( :#{#filter.isOverdue} = true AND t.status != br.com.teamtacles.task.enumeration.ETaskStatus.DONE AND t.dueDate IS NOT NULL AND t.dueDate < CURRENT_TIMESTAMP) ) " +
            "AND (:#{#filter.assignedUserId} IS NULL OR EXISTS (SELECT 1 FROM TaskAssignment ta WHERE ta.task = t AND ta.user.id = :#{#filter.assignedUserId})) " +
            "AND (:#{#filter.updatedAtAfter} IS NULL OR CAST(t.updatedAt AS date) >= :#{#filter.updatedAtAfter}) " +
            "AND (:#{#filter.updatedAtBefore} IS NULL OR CAST(t.updatedAt AS date) <= :#{#filter.updatedAtBefore})")
    Set<Task> findTasksByProjectWithFiltersForReport(@Param("projectId") Long projectId, @Param("filter") TaskFilterReportDTO filter);

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.assignments WHERE t.project.id = :projectId AND t.status = :status")
    List<Task> findAllByProjectIdAndStatusWithAssignments(@Param("projectId") Long projectId, @Param("status") ETaskStatus status);
}

