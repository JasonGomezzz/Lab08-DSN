package com.techstore.inventario.autenticacion;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Segundo factor pendiente: nace con la contraseña correcta, vive 5 minutos y admite 3 intentos. */
@Entity
@Table(name = "desafio_mfa")
@Getter
@Setter
public class DesafioMfa {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    private String id;
    @Column(name = "usuario_id")
    private Long usuarioId;
    private int intentos;
    @Column(name = "expira_en")
    private Instant expiraEn;
    private boolean consumido;
    @Column(name = "creado_en")
    private Instant creadoEn;
}
