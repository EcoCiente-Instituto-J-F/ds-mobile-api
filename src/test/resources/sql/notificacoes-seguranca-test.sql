DROP TABLE IF EXISTS tb_push_entregas;
DROP TABLE IF EXISTS tb_push_dispositivos;
DROP TABLE IF EXISTS tb_notificacoes;
DROP TABLE IF EXISTS tb_sindicos;
DROP TABLE IF EXISTS tb_usuarios;

CREATE TABLE tb_usuarios (
    id_usuario INTEGER PRIMARY KEY,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE tb_sindicos (
    id_sindico INTEGER PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES tb_usuarios(id_usuario)
);
CREATE TABLE tb_notificacoes (
    id_notificacao INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES tb_usuarios(id_usuario),
    titulo_mensagem VARCHAR(255) NOT NULL,
    corpo_mensagem TEXT,
    tipo_notificacao VARCHAR(50),
    chave_evento VARCHAR(240),
    destino VARCHAR(40),
    acao VARCHAR(100),
    data_envio TIMESTAMPTZ NOT NULL DEFAULT now(),
    lida_em TIMESTAMPTZ,
    enviar_apos TIMESTAMPTZ DEFAULT now()
);
CREATE UNIQUE INDEX uq_notificacoes_evento_usuario
    ON tb_notificacoes (usuario_id, chave_evento)
    WHERE chave_evento IS NOT NULL;
CREATE TABLE tb_push_dispositivos (
    instalacao_id VARCHAR(80) PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES tb_usuarios(id_usuario),
    token_fcm TEXT NOT NULL,
    preferencias JSONB NOT NULL DEFAULT '{}'::jsonb,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    ultimo_acesso_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_push_token_ativo
    ON tb_push_dispositivos (token_fcm) WHERE ativo = TRUE;
CREATE TABLE tb_push_entregas (
    id_entrega BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    notificacao_id INTEGER NOT NULL REFERENCES tb_notificacoes(id_notificacao),
    instalacao_id VARCHAR(80) NOT NULL REFERENCES tb_push_dispositivos(instalacao_id),
    estado VARCHAR(20) DEFAULT 'PENDENTE',
    tentativas INTEGER DEFAULT 0,
    proxima_tentativa TIMESTAMPTZ DEFAULT now(),
    bloqueado_em TIMESTAMPTZ,
    enviado_em TIMESTAMPTZ,
    ultimo_erro TEXT,
    UNIQUE (notificacao_id, instalacao_id)
);
INSERT INTO tb_usuarios VALUES (10, TRUE), (20, TRUE), (30, FALSE);
INSERT INTO tb_sindicos VALUES (1, 10);
