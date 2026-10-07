package com.techstore.inventario.autenticacion;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "token_revocado")
@Getter
@Setter
public class TokenRevocado {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    private String jti;
    @Column(name = "expira_en")
    private Instant expiraEn;
}
