package com.techstore.inventario.autenticacion;

import java.time.Instant;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DesafioMfaRepository extends JpaRepository<DesafioMfa, String> {
    /** Bloquea la fila para que dos verificaciones simultáneas no se salten el límite de intentos. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DesafioMfa d where d.id = :id")
    Optional<DesafioMfa> buscarParaActualizar(@Param("id") String id);

    @Modifying
    @Query("update DesafioMfa d set d.consumido = true where d.usuarioId = :usuarioId and d.consumido = false")
    int consumirPendientes(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("delete from DesafioMfa d where d.expiraEn < :limite")
    int eliminarExpirados(@Param("limite") Instant limite);
}
