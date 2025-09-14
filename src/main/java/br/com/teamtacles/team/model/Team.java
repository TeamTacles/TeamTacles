package br.com.teamtacles.team.model;

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
@ToString(exclude = {"owner", "members"})
@EqualsAndHashCode(of = "id")
@Entity
@Table(name="teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(min=3, max=50)
    @NotBlank(message="The team name cannot be blank")
    private String name;

    @Size(max=250)
    private String description;

    @Setter(AccessLevel.NONE)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TeamMember> members = new HashSet<>();

    private String invitationToken;

    private LocalDateTime invitationTokenExpiry;

    @PrePersist
    public void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public void addMember(TeamMember member) {
        this.members.add(member);
        member.setTeam(this);
    }

    public void removeMember(TeamMember member) {
        this.members.remove(member);
        member.setTeam(null);
    }
}
