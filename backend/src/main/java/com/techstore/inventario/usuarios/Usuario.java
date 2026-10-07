package com.techstore.inventario.usuarios;

import java.time.Instant;
import com.techstore.inventario.tiendas.Tienda;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Getter
@Setter
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    @Column(name = "password_hash")
    private String passwordHash;
    @Column(name = "nombre_completo")
    private String nombreCompleto;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tienda_id")
    private Tienda tienda;
    @Enumerated(EnumType.STRING)
    private Rol rol;
    @Enumerated(EnumType.STRING)
    private ProveedorIdentidad proveedor;
    @Column(name = "proveedor_id")
    private String proveedorId;
    @Column(name = "intentos_fallidos")
    private int intentosFallidos;
    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;
    @Column(name = "mfa_secreto")
    private String mfaSecreto;
    @Column(name = "mfa_habilitado")
    private boolean mfaHabilitado;
    @Column(name = "mfa_ultimo_paso")
    private Long mfaUltimoPaso;
    @Column(name = "creado_en")
    private Instant creadoEn;

    public boolean estaBloqueado(Instant ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }
}
