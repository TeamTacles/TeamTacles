package br.com.teamtacles.team.model;

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
@ToString(exclude = {"owner", "members"})
@EqualsAndHashCode(of = "id")
@Entity
@Table(name="teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Setter
    @Column(length = 250)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "invitation_token")
    private String invitationToken;

    @Column(name = "invitation_token_expiry")
    private LocalDateTime invitationTokenExpiry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TeamMember> members = new HashSet<>();

    public Team(String name, String description, User owner) {
        this.name = name;
        this.description = description == null ? "" : description;
        this.owner = owner;
    }

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

    public void transferOwnership(User newOwner) {
        this.owner = newOwner;
    }

    public Set<TeamMember> getMembers() {
        return Collections.unmodifiableSet(members);
    }
}
