package br.com.teamtacles.project.model;

import br.com.teamtacles.project.enumeration.EProjectRole;
import br.com.teamtacles.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@ToString(exclude = {"user", "project"})
@EqualsAndHashCode(of = "id")
@Entity
@Table(name="project_members")
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter(AccessLevel.PACKAGE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private OffsetDateTime joinedAt;

    @Column(name = "accepted_invite")
    private boolean acceptedInvite = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_role", nullable = false)
    private EProjectRole projectRole;

    @Column(name = "invitation_token")
    private String invitationToken;

    @Column(name = "invitation_token_expiry")
    private OffsetDateTime invitationTokenExpiry;

    public ProjectMember(User user, Project project, EProjectRole projectRole) {
        this.user = user;
        this.project = project;
        this.projectRole = projectRole;
    }

    public String generateInvitation() {
        String token = UUID.randomUUID().toString();

        this.invitationToken = token;
        this.invitationTokenExpiry = OffsetDateTime.now().plusHours(24);

        return token;
    }

    public void acceptedInvitation() {
        this.acceptedInvite = true;
        this.invitationToken = null;
        this.invitationTokenExpiry = null;
    }

    public void changeRole(EProjectRole role) {
        this.projectRole = role;
    }
}
