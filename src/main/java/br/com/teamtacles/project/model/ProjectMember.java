package br.com.teamtacles.project.model;

import br.com.teamtacles.project.enumeration.EProjectRole;
import br.com.teamtacles.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"user", "project"})
@EqualsAndHashCode(of = "id")
@Entity
@Table(name="project_members")
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "joined_at")
    private OffsetDateTime joinedAt;

    @Column(name = "accepted_invite")
    private boolean acceptedInvite = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_role", nullable = false)
    private EProjectRole projectRole;

    @Column(name = "invitation_token")
    private String invitationToken;

    @Column(name = "invitation_token_expiry")
    private LocalDateTime invitationTokenExpiry;

    public ProjectMember(User user, Project project, EProjectRole projectRole) {
        this.user = user;
        this.project = project;
        this.projectRole = projectRole;
    }
}
