package br.com.teamtacles.project.model;

import br.com.teamtacles.task.model.Task;
import br.com.teamtacles.user.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"owner", "members", "tasks"})
@EqualsAndHashCode(of = "id")
@Entity
@Table(name="project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min=3, max=50)
    private String title;

    @Size(max=250)
    private String description;

    @ManyToOne
    @JoinColumn(name="owner_id", nullable = false)
    private User owner;

    @Setter(AccessLevel.NONE)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProjectMember> members = new HashSet<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Task> tasks = new HashSet<>();

    private String invitationToken;

    private LocalDateTime invitationTokenExpiry;

    @PrePersist
    public void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public void addMember(ProjectMember member) {
        this.members.add(member);
        member.setProject(this);

    }

    public void removeMember(ProjectMember member) {
        this.members.remove(member);
        member.setProject(null);
    }
}
