package br.com.teamtacles.team.model;

import br.com.teamtacles.team.enumeration.ETeamRole;
import br.com.teamtacles.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@ToString(exclude = {"user", "team"})
@EqualsAndHashCode(of = "id")
@Entity
@Table(name="team_members")
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Setter(AccessLevel.PACKAGE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "joined_at")
    private OffsetDateTime joinedAt;

    @Column(name = "accepted_invite")
    private boolean acceptedInvite = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_role", nullable = false)
    private ETeamRole teamRole;

    @Column(name = "invitation_token")
    private String invitationToken;

    @Column(name = "invitation_token_expiry")
    private LocalDateTime invitationTokenExpiry;

    @PrePersist
    public void OnInvite() {
        if(this.joinedAt == null){
            this.joinedAt = OffsetDateTime.now();
        }
    }

    public TeamMember (User user, Team team, ETeamRole teamRole) {
        this.user = user;
        this.team = team;
        this.teamRole = teamRole;
    }

    public String generateInvitation() {
        String token = UUID.randomUUID().toString();

        this.invitationToken = token;
        this.invitationTokenExpiry = LocalDateTime.now().plusHours(24);

        return token;
    }

    public void acceptedInvitation() {
        this.acceptedInvite = true;
        this.invitationToken = null;
        this.invitationTokenExpiry = null;
    }

    public void changeRole(ETeamRole role) {
        this.teamRole = role;
    }
}
