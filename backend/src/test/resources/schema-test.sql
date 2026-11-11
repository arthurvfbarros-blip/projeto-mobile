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

CREATE TABLE IF NOT EXISTS categoria_servico (
    id_categoria INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    CONSTRAINT uq_categoria_servico_nome UNIQUE (nome)
);

CREATE TABLE IF NOT EXISTS prestador (
    id_prestador INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario INTEGER NOT NULL,
    nome VARCHAR(150) NOT NULL,
    cpf VARCHAR(11) NOT NULL,
    telefone VARCHAR(20),
    descricao TEXT,
    avaliacao_media NUMERIC(3, 2) DEFAULT 0.00,
    status_aprovacao VARCHAR(20) NOT NULL DEFAULT 'pendente',
    cidade VARCHAR(100),
    uf VARCHAR(2),
    CONSTRAINT uq_prestador_usuario UNIQUE (id_usuario),
    CONSTRAINT uq_prestador_cpf UNIQUE (cpf),
    CONSTRAINT fk_prestador_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    CONSTRAINT ck_prestador_aprovacao CHECK (status_aprovacao IN ('pendente', 'aprovado', 'reprovado')),
    CONSTRAINT ck_prestador_area CHECK (
        (cidade IS NULL AND uf IS NULL)
        OR (cidade IS NOT NULL AND TRIM(cidade) <> '' AND uf IS NOT NULL AND uf IN (
            'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MG', 'MS', 'MT', 'PA',
            'PB', 'PE', 'PI', 'PR', 'RJ', 'RN', 'RO', 'RR', 'RS', 'SC', 'SE', 'SP', 'TO'
        ))
    )
);

CREATE TABLE IF NOT EXISTS empresa (
    id_empresa INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_prestador INTEGER NOT NULL,
    razao_social VARCHAR(200) NOT NULL,
    nome_fantasia VARCHAR(200),
    cnpj VARCHAR(14) NOT NULL,
    cep VARCHAR(8),
    telefone VARCHAR(20),
    CONSTRAINT uq_empresa_prestador UNIQUE (id_prestador),
    CONSTRAINT uq_empresa_cnpj UNIQUE (cnpj),
    CONSTRAINT fk_empresa_prestador FOREIGN KEY (id_prestador) REFERENCES prestador (id_prestador) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS prestador_categoria (
    id_prestador INTEGER NOT NULL,
    id_categoria INTEGER NOT NULL,
    PRIMARY KEY (id_prestador, id_categoria),
    FOREIGN KEY (id_prestador) REFERENCES prestador (id_prestador) ON DELETE CASCADE,
    FOREIGN KEY (id_categoria) REFERENCES categoria_servico (id_categoria) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS prestador_bairro (
    id_prestador INTEGER NOT NULL REFERENCES prestador (id_prestador) ON DELETE CASCADE,
    bairro VARCHAR(100) NOT NULL CHECK (TRIM(bairro) <> ''),
    PRIMARY KEY (id_prestador, bairro)
);

CREATE TABLE IF NOT EXISTS pedido_servico (
    id_pedido INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cliente INTEGER NOT NULL,
    id_categoria INTEGER NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    descricao TEXT NOT NULL,
    status VARCHAR(25) NOT NULL DEFAULT 'aberto',
    id_endereco INTEGER,
    data_desejada DATE,
    criado_em TIMESTAMP WITH TIME ZONE,
    endereco_cep VARCHAR(8),
    endereco_logradouro VARCHAR(150),
    endereco_numero VARCHAR(20),
    endereco_complemento VARCHAR(100),
    endereco_bairro VARCHAR(100),
    endereco_cidade VARCHAR(100),
    endereco_uf VARCHAR(2),
    CONSTRAINT fk_pedido_servico_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
    CONSTRAINT fk_pedido_servico_categoria FOREIGN KEY (id_categoria) REFERENCES categoria_servico (id_categoria),
    CONSTRAINT fk_pedido_servico_endereco FOREIGN KEY (id_endereco) REFERENCES endereco_cliente (id_endereco) ON DELETE SET NULL,
    CONSTRAINT ck_pedido_servico_status CHECK (status IN ('aberto', 'em_orcamento', 'aguardando_confirmacao', 'agendado', 'concluido', 'cancelado'))
);

CREATE INDEX IF NOT EXISTS idx_pedido_servico_cliente ON pedido_servico (id_cliente, id_pedido);

CREATE TABLE IF NOT EXISTS orcamento (
    id_orcamento INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_pedido INTEGER NOT NULL REFERENCES pedido_servico(id_pedido) ON DELETE CASCADE,
    id_prestador INTEGER NOT NULL REFERENCES prestador(id_prestador),
    valor NUMERIC(10,2) NOT NULL CHECK (valor > 0),
    prazo_dias INTEGER CHECK (prazo_dias > 0),
    descricao TEXT,
    status VARCHAR(15) NOT NULL DEFAULT 'enviado' CHECK (status IN ('enviado', 'aceito', 'recusado', 'expirado')),
    CONSTRAINT uq_orcamento_pedido_prestador UNIQUE (id_pedido, id_prestador)
);
