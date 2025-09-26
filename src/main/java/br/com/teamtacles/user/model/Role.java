package br.com.teamtacles.user.model;

import br.com.teamtacles.user.enumeration.ERole;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Entity
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false, unique = true, name="role_name")
    private ERole roleName;
}
