package es.danieljr.peliculas.rest.users.repositories;

import es.danieljr.peliculas.rest.users.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsernameEqualsIgnoreCaseOrEmailEqualsIgnoreCase(String username, String email);
    @Modifying // Para indicar que es una consulta de actualización
    @Query("UPDATE User p SET p.isDeleted = true WHERE p.id = :id")
        // Consulta de actualización
    void updateIsDeletedToTrueById(Long id);

    List<User> findAllByIsDeletedFalse();

    Optional<User> findByUsername(String username);
}
