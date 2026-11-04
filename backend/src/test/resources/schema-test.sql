CREATE TABLE IF NOT EXISTS usuario (
    id_usuario INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    tipo_usuario VARCHAR(20) NOT NULL CHECK (tipo_usuario IN ('cliente', 'prestador')),
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);
