package com.techstore.inventario.tiendas;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TiendaRepository extends JpaRepository<Tienda, Long> {
    List<Tienda> findAllByOrderByNombreAsc();
}
