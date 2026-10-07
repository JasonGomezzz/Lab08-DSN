CREATE TABLE tienda (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    codigo VARCHAR(20) NOT NULL,
    nombre VARCHAR(80) NOT NULL,
    ciudad VARCHAR(80) NOT NULL,
    CONSTRAINT uq_tienda_codigo UNIQUE (codigo)
);

-- password_hash es nulo solo en cuentas creadas con Google o GitHub.
CREATE TABLE usuario (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(160) NOT NULL,
    password_hash VARCHAR(100) NULL,
    nombre_completo VARCHAR(120) NOT NULL,
    tienda_id BIGINT NULL,
    rol VARCHAR(20) NOT NULL,
    proveedor VARCHAR(10) NOT NULL DEFAULT 'LOCAL',
    proveedor_id VARCHAR(100) NULL,
    intentos_fallidos INT NOT NULL DEFAULT 0,
    bloqueado_hasta DATETIME(6) NULL,
    mfa_secreto VARCHAR(200) NULL,
    mfa_habilitado BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_ultimo_paso BIGINT NULL,
    creado_en DATETIME(6) NOT NULL,
    CONSTRAINT uq_usuario_email UNIQUE (email),
    CONSTRAINT uq_usuario_proveedor UNIQUE (proveedor, proveedor_id),
    CONSTRAINT fk_usuario_tienda FOREIGN KEY (tienda_id) REFERENCES tienda(id),
    CONSTRAINT chk_usuario_rol CHECK (rol IN ('ADMINISTRADOR', 'GERENTE_TIENDA', 'EMPLEADO_VENTAS', 'AUDITOR')),
    CONSTRAINT chk_usuario_proveedor CHECK (proveedor IN ('LOCAL', 'GOOGLE', 'GITHUB')),
    CONSTRAINT chk_usuario_password CHECK (proveedor <> 'LOCAL' OR password_hash IS NOT NULL),
    CONSTRAINT chk_usuario_intentos CHECK (intentos_fallidos >= 0)
);

-- Un desafío MFA nace con la contraseña (o el login social) correcta y vive 5 minutos.
CREATE TABLE desafio_mfa (
    id CHAR(36) PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    intentos INT NOT NULL DEFAULT 0,
    expira_en DATETIME(6) NOT NULL,
    consumido BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en DATETIME(6) NOT NULL,
    CONSTRAINT fk_desafio_mfa_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
);

CREATE INDEX idx_desafio_mfa_expira ON desafio_mfa (expira_en);

CREATE TABLE token_revocado (
    jti CHAR(36) PRIMARY KEY,
    expira_en DATETIME(6) NOT NULL
);

CREATE INDEX idx_token_revocado_expira ON token_revocado (expira_en);

CREATE TABLE producto (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sku VARCHAR(40) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    categoria VARCHAR(60) NOT NULL,
    precio DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    tienda_id BIGINT NOT NULL,
    creado_en DATETIME(6) NOT NULL,
    actualizado_en DATETIME(6) NOT NULL,
    CONSTRAINT uq_producto_tienda_sku UNIQUE (tienda_id, sku),
    CONSTRAINT fk_producto_tienda FOREIGN KEY (tienda_id) REFERENCES tienda(id),
    CONSTRAINT chk_producto_precio CHECK (precio >= 0),
    CONSTRAINT chk_producto_stock CHECK (stock >= 0)
);
