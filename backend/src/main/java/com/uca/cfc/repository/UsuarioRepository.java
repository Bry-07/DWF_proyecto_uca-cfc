package com.uca.cfc.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uca.cfc.entity.Usuario;
import com.uca.cfc.entity.enums.EstadoUsuario;

/**
 * Acceso a datos de los usuarios.
 * findByEmailIgnoreCase es el que se usara para el login cuando se implemente
 * la seguridad con JWT en la Fase 3.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = {"rol"})
    Optional<Usuario> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"rol"})
    Optional<Usuario> findWithRolById(Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByRolId(Long rolId);

    @EntityGraph(attributePaths = {"rol"})
    @Query("""
            SELECT u FROM Usuario u
            WHERE (:rolId IS NULL OR u.rol.id = :rolId)
              AND (:estado IS NULL OR u.estado = :estado)
              AND (CAST(:texto AS String) IS NULL OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', CAST(:texto AS String), '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:texto AS String), '%')))
            """)
    Page<Usuario> buscar(@Param("rolId") Long rolId, @Param("estado") EstadoUsuario estado,
            @Param("texto") String texto, Pageable pageable);
}
