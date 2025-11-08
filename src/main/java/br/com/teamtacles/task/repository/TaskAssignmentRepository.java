package br.com.teamtacles.task.repository;

import br.com.teamtacles.task.model.Task;
import br.com.teamtacles.task.model.TaskAssignment;
import br.com.teamtacles.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {
    Optional<TaskAssignment> findByTaskAndUser(Task task, User user);
    List<TaskAssignment> findAllByUser(User user);

    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.task.id = :taskId AND ta.user.id IN :userIds")
    Set<TaskAssignment> findAllByTaskIdAndUserIds(@Param("taskId") Long taskId, @Param("userIds") Set<Long> userIds);
    List<TaskAssignment> findAllByTaskId(Long taskId);
}


