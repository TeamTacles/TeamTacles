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
import java.util.UUID;

@Getter
@NoArgsConstructor
@ToString(exclude = {"owner", "members"})
@EqualsAndHashCode(of = "id")
@Entity
@Table(name="teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Size(min=3, max=50)
    @NotBlank(message="The team name cannot be blank")
    private String name;

    @Setter
    @Size(max=250)
    private String description;

    private OffsetDateTime createdAt;

    private String invitationToken;
    private LocalDateTime invitationTokenExpiry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TeamMember> members = new HashSet<>();

    @PrePersist
    public void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public Team(String name, String description, User owner) {
        this.name = name;
        this.description = (description == null) ? "" : description;
        this.owner = owner;
    }

    public void addMember(TeamMember member) {
        this.members.add(member);
        member.setTeam(this);
    }

    public void removeMember(TeamMember member) {
        this.members.remove(member);
        member.setTeam(null);
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
}
