package com.techstore.inventario.productos;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    @EntityGraph(attributePaths = "tienda")
    List<Producto> findAllByOrderByNombreAsc();

    @EntityGraph(attributePaths = "tienda")
    List<Producto> findByTiendaIdOrderByNombreAsc(Long tiendaId);

    @EntityGraph(attributePaths = "tienda")
    Optional<Producto> findWithTiendaById(Long id);

    boolean existsByTiendaIdAndSku(Long tiendaId, String sku);

    /**
     * Ajuste atómico de stock: la condición evita que dos ventas simultáneas dejen el stock en
     * negativo o pisen la cuenta una de la otra, cosa que sí pasaría leyendo y luego guardando.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Producto p set p.stock = p.stock + :ajuste, p.actualizadoEn = :ahora "
        + "where p.id = :id and p.stock + :ajuste >= 0")
    int ajustarStock(@Param("id") Long id, @Param("ajuste") int ajuste, @Param("ahora") Instant ahora);

    /** Una fila por tienda: id, código, nombre, productos, unidades, valor del inventario y productos con poco stock. */
    @Query("select t.id, t.codigo, t.nombre, count(p), coalesce(sum(p.stock), 0), coalesce(sum(p.precio * p.stock), 0), "
        + "coalesce(sum(case when p.stock < :umbral then 1 else 0 end), 0) "
        + "from Tienda t left join Producto p on p.tienda = t "
        + "where (:tiendaId is null or t.id = :tiendaId) "
        + "group by t.id, t.codigo, t.nombre order by t.nombre")
    List<Object[]> resumenPorTienda(@Param("tiendaId") Long tiendaId, @Param("umbral") int umbral);
}
