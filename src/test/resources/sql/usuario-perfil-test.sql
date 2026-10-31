-- PREPARACAO_COMPARTILHADA_TESTES
SET search_path TO public;

INSERT INTO public.tb_lkp_tipos_eventos_auditados (nome_evento)
SELECT evento
FROM (
    VALUES
        ('usuario_condominio'),
        ('agendamento_coleta'),
        ('postagem')
) AS eventos(evento)
WHERE NOT EXISTS (
    SELECT 1
    FROM public.tb_lkp_tipos_eventos_auditados existente
    WHERE existente.nome_evento = eventos.evento
);

INSERT INTO public.tb_lkp_tipos_operacoes_auditoria (nome_operacao)
SELECT operacao
FROM (
    VALUES ('INSERT'), ('UPDATE'), ('DELETE')
) AS operacoes(operacao)
WHERE NOT EXISTS (
    SELECT 1
    FROM public.tb_lkp_tipos_operacoes_auditoria existente
    WHERE existente.nome_operacao = operacoes.operacao
);

DROP TABLE IF EXISTS tb_rel_usuarios_condominios;
DROP TABLE IF EXISTS tb_moradores;
DROP TABLE IF EXISTS tb_torres;
DROP TABLE IF EXISTS tb_cooperativas;
DROP TABLE IF EXISTS tb_condominios;
DROP TABLE IF EXISTS tb_sindicos;
DROP TABLE IF EXISTS tb_telefones;
DROP TABLE IF EXISTS tb_usuarios;
DROP TABLE IF EXISTS tb_enderecos;
DROP TABLE IF EXISTS tb_lkp_niveis_confianca;
DROP TABLE IF EXISTS tb_lkp_tipos_condominios;
DROP TABLE IF EXISTS tb_lkp_tipos_usuarios;

CREATE TABLE tb_lkp_tipos_usuarios (
    id_tipo_usuario INTEGER PRIMARY KEY,
    nome_tipo VARCHAR(50) NOT NULL
);

CREATE TABLE tb_lkp_tipos_condominios (
    id_tipo_condominio INTEGER PRIMARY KEY,
    nome_tipo VARCHAR(50) NOT NULL
);

CREATE TABLE tb_lkp_niveis_confianca (
    id_nivel_confianca INTEGER PRIMARY KEY,
    nome_nivel VARCHAR(50) NOT NULL,
    peso_voto INTEGER NOT NULL CHECK (peso_voto > 0)
);

CREATE TABLE tb_enderecos (
    id_endereco INTEGER PRIMARY KEY,
    cep VARCHAR(10),
    estado CHAR(2),
    cidade VARCHAR(100),
    logradouro VARCHAR(150),
    numero VARCHAR(20),
    complemento VARCHAR(100)
);

CREATE TABLE tb_usuarios (
    id_usuario INTEGER PRIMARY KEY,
    nome_usuario VARCHAR(100) NOT NULL,
    email_usuario VARCHAR(255) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    data_nascimento DATE,
    cpf VARCHAR(11) UNIQUE,
    url_avatar VARCHAR(500),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    registro_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    tipo_usuario_id INTEGER NOT NULL
        REFERENCES tb_lkp_tipos_usuarios(id_tipo_usuario),
    endereco_id INTEGER NOT NULL
        REFERENCES tb_enderecos(id_endereco)
);

CREATE TABLE tb_sindicos (
    id_sindico INTEGER PRIMARY KEY,
    usuario_id INTEGER NOT NULL UNIQUE
        REFERENCES tb_usuarios(id_usuario)
);

CREATE TABLE tb_telefones (
    id_telefone INTEGER PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES tb_usuarios(id_usuario),
    numero_contato VARCHAR(20) NOT NULL,
    tipo_telefone VARCHAR(20),
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE tb_condominios (
    id_condominio INTEGER PRIMARY KEY,
    nome_condominio VARCHAR(255) NOT NULL,
    cnpj VARCHAR(18),
    codigo_acesso VARCHAR(20),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    tipo_condominio_id INTEGER NOT NULL
        REFERENCES tb_lkp_tipos_condominios(id_tipo_condominio),
    sindico_id INTEGER REFERENCES tb_sindicos(id_sindico),
    endereco_id INTEGER UNIQUE REFERENCES tb_enderecos(id_endereco),
    url_foto VARCHAR(500)
);

CREATE TABLE tb_torres (
    id_torre INTEGER PRIMARY KEY,
    nome_torre VARCHAR(100) NOT NULL,
    condominio_id INTEGER NOT NULL REFERENCES tb_condominios(id_condominio),
    UNIQUE (condominio_id, nome_torre)
);

CREATE TABLE tb_moradores (
    id_morador INTEGER PRIMARY KEY,
    condominio_id INTEGER NOT NULL REFERENCES tb_condominios(id_condominio),
    usuario_id INTEGER NOT NULL UNIQUE REFERENCES tb_usuarios(id_usuario),
    torre_id INTEGER REFERENCES tb_torres(id_torre)
);

CREATE TABLE tb_cooperativas (
    id_cooperativa INTEGER PRIMARY KEY,
    cnpj_cooperativa VARCHAR(18) NOT NULL,
    nome_cooperativa VARCHAR(255) NOT NULL,
    email_cooperativa VARCHAR(255),
    telefone_cooperativa VARCHAR(20),
    data_cadastro TIMESTAMPTZ NOT NULL DEFAULT now(),
    usuario_id INTEGER UNIQUE REFERENCES tb_usuarios(id_usuario),
    endereco_id INTEGER UNIQUE REFERENCES tb_enderecos(id_endereco)
);

CREATE TABLE tb_rel_usuarios_condominios (
    id_usuario_condominio INTEGER PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES tb_usuarios(id_usuario),
    condominio_id INTEGER NOT NULL REFERENCES tb_condominios(id_condominio),
    data_entrada TIMESTAMPTZ NOT NULL DEFAULT now(),
    data_saida TIMESTAMPTZ,
    aprovado BOOLEAN NOT NULL DEFAULT FALSE,
    aprovado_por_usuario_id INTEGER REFERENCES tb_usuarios(id_usuario),
    nivel_confianca_id INTEGER NOT NULL DEFAULT 1
        REFERENCES tb_lkp_niveis_confianca(id_nivel_confianca),
    trust_score DECIMAL(6,2) NOT NULL,
    postagens_validadas_sem_contestacao INTEGER NOT NULL,
    denuncias_realizadas INTEGER NOT NULL,
    denuncias_procedentes INTEGER NOT NULL,
    taxa_acerto_denuncias DECIMAL(5,2),
    UNIQUE (usuario_id, condominio_id)
);

INSERT INTO tb_lkp_tipos_usuarios VALUES
    (1, 'Usuário comum'),
    (2, 'Morador'),
    (3, 'Síndico'),
    (4, 'Cooperativa');

INSERT INTO tb_lkp_tipos_condominios VALUES
    (1, 'Residencial');

INSERT INTO tb_lkp_niveis_confianca VALUES
    (1, 'morador_comum', 1),
    (2, 'pessoa_confiavel', 3);

INSERT INTO tb_enderecos (
    id_endereco, cep, estado, cidade, logradouro, numero, complemento
)
SELECT
    id,
    '01001000',
    'SP',
    'São Paulo',
    'Rua ' || id,
    id::text,
    NULL
FROM generate_series(1, 9) AS id;

INSERT INTO tb_usuarios (
    id_usuario, nome_usuario, email_usuario, senha_hash,
    data_nascimento, cpf, url_avatar, ativo, registro_em,
    tipo_usuario_id, endereco_id
) VALUES
    (
        1, 'Moradora Teste', 'moradora@teste.com', 'hash-nao-expor',
        '2000-02-03', '12345678901', 'https://teste.com/avatar.png',
        TRUE, '2026-01-01T12:00:00Z', 2, 1
    ),
    (
        2, 'Síndico Teste', 'sindico@teste.com', 'hash-sindico',
        '1980-01-01', '12345678902', NULL,
        TRUE, '2026-01-01T12:00:00Z', 3, 2
    ),
    (
        3, 'Cooperativa Teste', 'coop-usuario@teste.com', 'hash-coop',
        NULL, NULL, NULL,
        TRUE, '2026-01-01T12:00:00Z', 4, 3
    ),
    (
        4, 'Usuário Sem Vínculos', 'comum@teste.com', 'hash-comum',
        NULL, NULL, NULL,
        TRUE, '2026-01-01T12:00:00Z', 1, 4
    ),
    (
        5, 'Usuário Inativo', 'inativo@teste.com', 'hash-inativo',
        NULL, NULL, NULL,
        FALSE, '2026-01-01T12:00:00Z', 1, 5
    ),
    (
        6, 'Morador Sem Torre', 'sem-torre@teste.com', 'hash-sem-torre',
        NULL, NULL, NULL,
        TRUE, '2026-01-01T12:00:00Z', 2, 9
    );

INSERT INTO tb_sindicos VALUES (20, 2);

INSERT INTO tb_condominios (
    id_condominio, nome_condominio, cnpj, codigo_acesso,
    ativo, tipo_condominio_id, sindico_id, endereco_id
) VALUES
    (100, 'Alameda', '11111111000111', 'SEGREDO100', TRUE, 1, 20, 6),
    (200, 'Bosque', '22222222000122', 'SEGREDO200', TRUE, 1, 20, 7),
    (300, 'Colinas', NULL, 'SEGREDO300', FALSE, 1, NULL, NULL);

INSERT INTO tb_torres VALUES
    (1002, 'Torre B', 100),
    (1001, 'Torre A', 100),
    (2001, 'Torre Única', 200);

INSERT INTO tb_moradores VALUES
    (10, 100, 1, 1001),
    (11, 200, 6, NULL);

INSERT INTO tb_cooperativas (
    id_cooperativa, cnpj_cooperativa, nome_cooperativa,
    email_cooperativa, telefone_cooperativa,
    data_cadastro, usuario_id, endereco_id
) VALUES (
    30, '34238864000168', 'EcoCoop',
    'empresa@teste.com', '1133334444',
    '2026-01-02T12:00:00Z', 3, 8
);

INSERT INTO tb_telefones VALUES
    (1, 1, '11911111111', 'Celular', TRUE),
    (2, 1, '1133333333', 'Fixo', TRUE),
    (3, 1, '11900000000', 'Antigo', FALSE),
    (4, 2, '11922222222', 'Celular', TRUE);

INSERT INTO tb_rel_usuarios_condominios (
    id_usuario_condominio, usuario_id, condominio_id,
    data_entrada, data_saida, aprovado, aprovado_por_usuario_id,
    nivel_confianca_id, trust_score,
    postagens_validadas_sem_contestacao,
    denuncias_realizadas, denuncias_procedentes, taxa_acerto_denuncias
) VALUES
    (
        1, 1, 100, '2026-01-01T12:00:00Z', NULL,
        TRUE, 2, 2, 85.50, 12, 4, 3, 75.00
    ),
    (
        2, 1, 200, '2026-02-01T12:00:00Z', NULL,
        FALSE, NULL, 1, 0, 0, 0, 0, NULL
    ),
    (
        3, 1, 300, '2026-01-01T12:00:00Z',
        '2026-03-01T12:00:00Z',
        TRUE, 2, 1, 20, 2, 0, 0, NULL
    );