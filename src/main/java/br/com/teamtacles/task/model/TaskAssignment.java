package br.com.teamtacles.task.model;

import br.com.teamtacles.task.enumeration.ETaskRole;
import br.com.teamtacles.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"task", "user"})
@ToString(exclude = {"task", "user"})
@Entity
@Table(name = "task_assignments")
public class TaskAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    @Setter(AccessLevel.PACKAGE)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_role", nullable = false)
    private ETaskRole taskRole;

    @Column(name = "assigned_at", nullable = false, updatable = false)
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

    public void changeRole(ETaskRole newRole) {
        this.taskRole = newRole;
    }
}