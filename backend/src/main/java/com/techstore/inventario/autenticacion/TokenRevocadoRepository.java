package com.techstore.inventario.autenticacion;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenRevocadoRepository extends JpaRepository<TokenRevocado, String> {
    @Modifying
    @Query("delete from TokenRevocado t where t.expiraEn < :limite")
    int eliminarExpirados(@Param("limite") Instant limite);
}
