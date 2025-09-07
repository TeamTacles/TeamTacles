package br.com.teamtacles.user.model;

import br.com.teamtacles.team.model.TeamMember;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Entity
@Table(name="users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Size(min = 3, max = 50)
    @NotBlank(message="The username  cannot be blank")
    private String username;

    @Email(message = "The email must be valid")
    @Size(min = 8, max = 50)
    private String email;

    @Size(min = 5, max = 100)
    @NotBlank(message="The password cannot be blank")
    private String password;

    @Setter(AccessLevel.NONE)
    private OffsetDateTime createdAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TeamMember> teamMemberships = new HashSet<>();

    @PrePersist
    public void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }
}


