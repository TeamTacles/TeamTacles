package br.com.teamtacles.repository;
import java.util.Optional;

import br.com.teamtacles.enumeration.ERole;
import br.com.teamtacles.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(ERole roleName);
}
