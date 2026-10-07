package com.techstore.inventario.tiendas;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TiendaRepository extends JpaRepository<Tienda, Long> {
    List<Tienda> findAllByOrderByNombreAsc();

    Optional<Tienda> findByCodigo(String codigo);
}
