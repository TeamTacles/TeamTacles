package br.com.teamtacles.task.repository;

import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.task.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findByProject(Pageable pageable, Project project);
}
