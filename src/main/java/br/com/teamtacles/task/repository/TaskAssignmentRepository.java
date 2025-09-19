package br.com.teamtacles.task.repository;

import br.com.teamtacles.task.model.Task;
import br.com.teamtacles.task.model.TaskAssignment;
import br.com.teamtacles.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {
    Optional<TaskAssignment> findByTaskAndUser(Task task, User user);

    @Modifying
    @Query("DELETE FROM TaskAssignment ta WHERE ta.task.id = :taskId AND ta.user.id IN :userIds")
    void deleteAllByTaskIdAndUserIds(Long taskId, List<Long> userIds);
}


