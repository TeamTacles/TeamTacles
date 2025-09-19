package br.com.teamtacles.task.model;

import br.com.teamtacles.task.enumeration.ETaskRole;
import br.com.teamtacles.user.model.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"task", "user"})
@Entity
@Table(name = "task_assignments")
public class TaskAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_role", nullable = false)
    private ETaskRole taskRole;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime assignedAt;

    public TaskAssignment(Task task, User user, ETaskRole taskRole) {
        this.task = task;
        this.user = user;
        this.taskRole = taskRole;
    }

    @PrePersist
    public void onAssign() {
        this.assignedAt = OffsetDateTime.now();
    }
}