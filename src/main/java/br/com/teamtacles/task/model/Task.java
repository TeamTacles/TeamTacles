package br.com.teamtacles.task.model;

import br.com.teamtacles.project.model.Project;
import br.com.teamtacles.task.enumeration.ETaskStatus;
import br.com.teamtacles.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"project", "owner", "assignments"})
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Setter
    @Column(nullable = false, length = 100)
    private String title;

    @Setter
    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ETaskStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Setter
    @Column(name = "due_date")
    private OffsetDateTime dueDate;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "completion_comment", length = 300)
    private String completionComment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TaskAssignment> assignments = new HashSet<>();

    public Task(Project project, String title, String description, User owner, OffsetDateTime dueDate) {
        this.project = project;
        this.title = title;
        this.description = description == null ? "" : description;
        this.owner = owner;
        this.dueDate = dueDate;
        this.status = ETaskStatus.TO_DO;
    }

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

    public void updateStatus(ETaskStatus status) {
        this.status = status;
    }

    public void completedTask(String completionComment) {
        this.status = ETaskStatus.DONE;
        this.completedAt = OffsetDateTime.now();
        this.completionComment = completionComment;
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

    public ETaskStatus getEffectiveStatus() {
        if(getStatus().equals(ETaskStatus.DONE)) {
            return ETaskStatus.DONE;
        }

        if(isOverdue()) {
            return ETaskStatus.OVERDUE;
        }

        return getStatus();
    }

    public void transferOwnership(User newOwner) {
        this.owner = newOwner;
    }

    public Set<TaskAssignment> getAssignments() {
        return Collections.unmodifiableSet(assignments);
    }
}