package com.techstore.inventario.usuarios;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @EntityGraph(attributePaths = "tienda")
    Optional<Usuario> findByEmail(String email);

    @EntityGraph(attributePaths = "tienda")
    Optional<Usuario> findWithTiendaById(Long id);

    @EntityGraph(attributePaths = "tienda")
    Optional<Usuario> findByProveedorAndProveedorId(ProveedorIdentidad proveedor, String proveedorId);

    @EntityGraph(attributePaths = "tienda")
    List<Usuario> findAllByOrderByNombreCompletoAsc();

    boolean existsByEmail(String email);

    /** Bloquea la fila para que el conteo de intentos fallidos sea exacto bajo concurrencia. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.email = :email")
    Optional<Usuario> buscarParaAutenticar(@Param("email") String email);
}
