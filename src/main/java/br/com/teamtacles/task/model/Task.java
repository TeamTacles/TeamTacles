package br.com.teamtacles.task.model;

import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.task.enumeration.ETaskStatus;
import br.com.teamtacles.user.model.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"project", "owner", "assignments"})
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ETaskStatus status;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime dueDate;

    private OffsetDateTime completedAt;

    @Column(length = 300)
    private String completionComment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TaskAssignment> assignments = new HashSet<>();

    @PrePersist
    public void onCreate() {
        this.createdAt = OffsetDateTime.now();
        if (this.status == null) {
            this.status = ETaskStatus.TO_DO;
        }
    }

    public void addAssigment(TaskAssignment assignment) {
        this.assignments.add(assignment);
        assignment.setTask(this);
    }

    public void removeAssigment(TaskAssignment assignment) {
        this.assignments.remove(assignment);
        assignment.setTask(null);
    }

    public boolean isToDo() {
        return this.status == ETaskStatus.TO_DO;
    }

    public boolean isInProgress() {
        return this.status == ETaskStatus.IN_PROGRESS;
    }

    public boolean isCompleted() {
        return this.status == ETaskStatus.DONE;
    }

    public boolean isOverdue() {
        return this.dueDate != null && OffsetDateTime.now().isAfter(this.dueDate) && !isCompleted();
    }
}