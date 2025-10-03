package br.com.teamtacles.user.model;

import br.com.teamtacles.project.model.ProjectMember;
import br.com.teamtacles.task.model.TaskAssignment;
import br.com.teamtacles.team.model.TeamMember;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@ToString(exclude = {"password", "roles", "teamMemberships", "projectMemberships", "taskAssignments"})
@EqualsAndHashCode(of = "id")
@Entity
@Table(name="users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Setter
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Setter(AccessLevel.PRIVATE)
    @Column(nullable = false, length = 100)
    private String password;

    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    @Column(name = "reset_password_token_expiry")
    private LocalDateTime resetPasswordTokenExpiry;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "verification_token_expiry")
    private LocalDateTime verificationTokenExpiry;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<TeamMember> teamMemberships = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<ProjectMember> projectMemberships = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<TaskAssignment> taskAssignments = new HashSet<>();

    @PrePersist
    public void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public void definePassword(String password) {
        this.setPassword(password);
    }

    public void updatePassword(String newPassword) {
        this.setPassword(newPassword);
        this.resetPasswordToken = null;
        this.resetPasswordTokenExpiry = null;
    }

    public void confirmAccountVerification() {
        if(this.enabled){
            return;
        }

        this.enabled = true;
        this.verificationToken = null;
        this.verificationTokenExpiry = null;
    }

    public void disableAccount() {
        this.enabled = false;
    }

    public void assignVerificationToken(String token, LocalDateTime expiryDate) {
        this.verificationToken = token;
        this.verificationTokenExpiry = expiryDate;
    }

    public void assignPasswordResetToken(String token, LocalDateTime expiryDate) {
        this.resetPasswordToken = token;
        this.resetPasswordTokenExpiry = expiryDate;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    public Set<TeamMember> getTeamMemberships() {
        return Collections.unmodifiableSet(teamMemberships);
    }

    public Set<ProjectMember> getProjectMemberships() {
        return Collections.unmodifiableSet(projectMemberships);
    }

    public Set<TaskAssignment> getTaskAssignments() {
        return Collections.unmodifiableSet(taskAssignments);
    }
}


