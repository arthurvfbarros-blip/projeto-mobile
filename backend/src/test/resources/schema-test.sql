CREATE TABLE IF NOT EXISTS usuario (
    id_usuario INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email VARCHAR(150) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    tipo_usuario VARCHAR(20) NOT NULL CHECK (tipo_usuario IN ('cliente', 'prestador')),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_usuario_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS cliente (
    id_cliente INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario INTEGER NOT NULL,
    nome VARCHAR(150) NOT NULL,
    cpf VARCHAR(11) NOT NULL,
    telefone VARCHAR(20),
    CONSTRAINT uq_cliente_usuario UNIQUE (id_usuario),
    CONSTRAINT uq_cliente_cpf UNIQUE (cpf),
    CONSTRAINT fk_cliente_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS endereco_cliente (
    id_endereco INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cliente INTEGER NOT NULL,
    cep VARCHAR(8) NOT NULL CHECK (REGEXP_LIKE(cep, '^[0-9]{8}$')),
    logradouro VARCHAR(150) NOT NULL CHECK (TRIM(logradouro) <> ''),
    numero VARCHAR(20) NOT NULL CHECK (TRIM(numero) <> ''),
    complemento VARCHAR(100),
    bairro VARCHAR(100) NOT NULL CHECK (TRIM(bairro) <> ''),
    cidade VARCHAR(100) NOT NULL CHECK (TRIM(cidade) <> ''),
    uf VARCHAR(2) NOT NULL CHECK (uf IN (
        'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG',
        'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'
    )),
    CONSTRAINT fk_endereco_cliente_cliente FOREIGN KEY (id_cliente)
        REFERENCES cliente (id_cliente) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_endereco_cliente_cliente ON endereco_cliente (id_cliente, id_endereco);
