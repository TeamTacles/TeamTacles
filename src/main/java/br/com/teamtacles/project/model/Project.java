package br.com.teamtacles.project.model;

import br.com.teamtacles.task.model.Task;
import br.com.teamtacles.user.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@NoArgsConstructor
@ToString(exclude = {"owner", "members", "tasks"})
@EqualsAndHashCode(of = "id")
@Entity
@Table(name="project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @NotBlank
    @Size(min=3, max=50)
    private String title;

    @Setter
    @Size(max=250)
    private String description;

    @ManyToOne
    @JoinColumn(name="owner_id", nullable = false)
    private User owner;

    private OffsetDateTime createdAt;

    private String invitationToken;
    private LocalDateTime invitationTokenExpiry;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProjectMember> members = new HashSet<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Task> tasks = new HashSet<>();

    public Project(String title, String description, User owner) {
        this.title = title;
        this.description = description == null ? "" : description;
        this.owner = owner;
    }

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

    public String generateInviteLinkToken() {
        String token = UUID.randomUUID().toString();

        this.invitationToken = token;
        this.invitationTokenExpiry = LocalDateTime.now().plusHours(24);

        return token;
    }

    public void expiryInviteLinkToken() {
        this.invitationToken = null;
        this.invitationTokenExpiry = null;
    }

    public Set<ProjectMember> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public Set<Task> getTasks() {
        return Collections.unmodifiableSet(tasks);
    }
}
