package br.com.teamtacles.user.repository;
import java.util.Optional;

import br.com.teamtacles.user.enumeration.ERole;
import br.com.teamtacles.user.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(ERole roleName);
}
