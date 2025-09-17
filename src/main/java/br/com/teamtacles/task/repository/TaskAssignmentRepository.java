package br.com.teamtacles.task.repository;

import br.com.teamtacles.task.model.Task;
import br.com.teamtacles.task.model.TaskAssignment;
import br.com.teamtacles.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {
    Optional<TaskAssignment> findByTaskAndUser(Task task, User user);
}