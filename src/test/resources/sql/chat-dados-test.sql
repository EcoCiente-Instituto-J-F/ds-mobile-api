CREATE TABLE IF NOT EXISTS tb_conversas (
    id_conversa INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    condominio_id INTEGER NOT NULL,
    cooperativa_id INTEGER NOT NULL,
    criada_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    ultima_mensagem_em TIMESTAMPTZ,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_conversas_condominio_id
        FOREIGN KEY (condominio_id)
        REFERENCES tb_condominios (id_condominio),

    CONSTRAINT fk_conversas_cooperativa_id
        FOREIGN KEY (cooperativa_id)
        REFERENCES tb_cooperativas (id_cooperativa),

    CONSTRAINT uq_conversas_condominio_cooperativa
        UNIQUE (condominio_id, cooperativa_id)
);

CREATE TABLE IF NOT EXISTS tb_mensagens_conversas (
    id_mensagem BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conversa_id INTEGER NOT NULL,
    remetente_usuario_id INTEGER NOT NULL,
    remetente_tipo VARCHAR(20) NOT NULL,
    conteudo TEXT NOT NULL,
    enviada_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    lida_em TIMESTAMPTZ,

    CONSTRAINT fk_mensagens_conversas_conversa_id
        FOREIGN KEY (conversa_id)
        REFERENCES tb_conversas (id_conversa),

    CONSTRAINT fk_mensagens_conversas_remetente_usuario_id
        FOREIGN KEY (remetente_usuario_id)
        REFERENCES tb_usuarios (id_usuario),

    CONSTRAINT ck_mensagens_conversas_remetente_tipo
        CHECK (
            remetente_tipo IN ('COOPERATIVA', 'CONDOMINIO')
        ),

    CONSTRAINT ck_mensagens_conversas_conteudo
        CHECK (
            length(btrim(conteudo)) BETWEEN 1 AND 2000
        ),

    CONSTRAINT ck_mensagens_conversas_lida_em
        CHECK (
            lida_em IS NULL OR lida_em >= enviada_em
        )
);

INSERT INTO tb_lkp_tipos_usuarios (
    id_tipo_usuario,
    nome_tipo
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Cooperativa'),
    (2, 'Síndico'),
    (3, 'Morador');

INSERT INTO tb_lkp_tipos_condominios (
    id_tipo_condominio,
    nome_tipo
)
OVERRIDING SYSTEM VALUE
VALUES (1, 'Residencial');

INSERT INTO tb_enderecos (
    id_endereco,
    cep,
    estado,
    cidade,
    logradouro,
    numero
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, '01001000', 'SP', 'São Paulo', 'Rua Um', '10'),
    (2, '01001000', 'SP', 'São Paulo', 'Rua Dois', '20'),
    (3, '01001000', 'SP', 'São Paulo', 'Rua Três', '30');

INSERT INTO tb_usuarios (
    id_usuario,
    nome_usuario,
    email_usuario,
    senha_hash,
    ativo,
    tipo_usuario_id,
    endereco_id
)
OVERRIDING SYSTEM VALUE
VALUES
    (
        10, 'Cooperativa Um', 'coop1@teste.com',
        'hash', TRUE, 1, 1
    ),
    (
        20, 'Cooperativa Dois', 'coop2@teste.com',
        'hash', TRUE, 1, 2
    ),
    (
        30, 'Síndico Um', 'sindico1@teste.com',
        'hash', TRUE, 2, 1
    ),
    (
        40, 'Síndico Dois', 'sindico2@teste.com',
        'hash', TRUE, 2, 2
    ),
    (
        50, 'Morador', 'morador@teste.com',
        'hash', TRUE, 3, 1
    ),
    (
        60, 'Responsável Inativo', 'inativo@teste.com',
        'hash', FALSE, 1, 3
    );
INSERT INTO tb_sindicos (
    id_sindico,
    usuario_id
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 30),
    (2, 40);

INSERT INTO tb_condominios (
    id_condominio,
    nome_condominio,
    ativo,
    tipo_condominio_id,
    sindico_id
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Condomínio Um', TRUE, 1, 1),
    (2, 'Condomínio Dois', TRUE, 1, 2),
    (3, 'Condomínio Inativo', FALSE, 1, 1);

INSERT INTO tb_cooperativas (
    id_cooperativa,
    cnpj_cooperativa,
    nome_cooperativa,
    usuario_id
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, '34238864000168', 'Cooperativa Um', 10),
    (2, '11222333000181', 'Cooperativa Dois', 20),
    (3, '22333444000181', 'Cooperativa Inativa', 60);