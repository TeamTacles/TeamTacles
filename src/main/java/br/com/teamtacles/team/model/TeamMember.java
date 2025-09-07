package br.com.teamtacles.team.model;

import br.com.teamtacles.team.enumeration.ETeamRole;
import br.com.teamtacles.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
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
}
