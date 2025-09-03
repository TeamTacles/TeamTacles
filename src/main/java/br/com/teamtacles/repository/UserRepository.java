package br.com.teamtacles.repository;

import br.com.teamtacles.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByUsername(String userName);
    boolean existsByEmail(String email);
}
