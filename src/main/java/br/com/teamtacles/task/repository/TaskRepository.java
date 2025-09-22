package br.com.teamtacles.task.repository;

import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.task.dto.request.TaskFilterDTO;
import br.com.teamtacles.task.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findByProject(Pageable pageable, Project project);

    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId " +
            "AND ( :#{#filter.title} IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :#{#filter.title}, '%')) ) " +
            "AND ( :#{#filter.status} IS NULL OR t.status = :#{#filter.status} ) " +
            "AND ( :#{#filter.dueDateAfter} IS NULL OR CAST(t.dueDate AS date) >= :#{#filter.dueDateAfter} ) " +
            "AND ( :#{#filter.dueDateBefore} IS NULL OR CAST(t.dueDate AS date) <= :#{#filter.dueDateBefore} ) " +
            "AND ( :#{#filter.conclusionDateAfter} IS NULL OR CAST(t.completedAt AS date) >= :#{#filter.conclusionDateAfter} ) " +
            "AND ( :#{#filter.conclusionDateBefore} IS NULL OR CAST(t.completedAt AS date) <= :#{#filter.conclusionDateBefore} ) " +
            "AND ( :#{#filter.createdAtAfter} IS NULL OR CAST(t.createdAt AS date) >= :#{#filter.createdAtAfter} ) " +
            "AND ( :#{#filter.createdAtBefore} IS NULL OR CAST(t.createdAt AS date) <= :#{#filter.createdAtBefore} )")
    Page<Task> findTasksByProjectWithFilters(@Param("projectId") Long projectId, @Param("filter") TaskFilterDTO filter, Pageable pageable);

    @Query("SELECT DISTINCT t FROM Task t " +
            "LEFT JOIN FETCH t.assignments a " +
            "LEFT JOIN FETCH a.user u " +
            "WHERE t.project.id = :projectId " +
            "AND (:#{#filter.status} IS NULL OR t.status = :#{#filter.status}) " +
            "AND (:#{#filter.assignedUserId} IS NULL OR a.user.id = :#{#filter.assignedUserId}) " +
            "AND (:#{#filter.updatedAtAfter} IS NULL OR CAST(t.updatedAt AS date) >= :#{#filter.updatedAtAfter}) " +
            "AND (:#{#filter.updatedAtBefore} IS NULL OR CAST(t.updatedAt AS date) <= :#{#filter.updatedAtBefore})")
    List<Task> findTasksByProjectWithFiltersForReport(@Param("projectId") Long projectId, @Param("filter") TaskFilterDTO filter);
}

