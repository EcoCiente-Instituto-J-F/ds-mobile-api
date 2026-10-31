BEGIN;

ALTER TABLE tb_notificacoes
  ADD COLUMN IF NOT EXISTS chave_evento VARCHAR(240),
  ADD COLUMN IF NOT EXISTS destino VARCHAR(40),
  ADD COLUMN IF NOT EXISTS acao VARCHAR(100),
  ADD COLUMN IF NOT EXISTS lida_em TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS enviar_apos TIMESTAMPTZ DEFAULT now();

CREATE UNIQUE INDEX IF NOT EXISTS uq_notificacoes_evento_usuario
  ON tb_notificacoes (usuario_id, chave_evento)
  WHERE chave_evento IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_notificacoes_historico
  ON tb_notificacoes (usuario_id, data_envio DESC);

CREATE TABLE IF NOT EXISTS tb_push_dispositivos (
  instalacao_id VARCHAR(80) PRIMARY KEY,
  usuario_id INTEGER NOT NULL REFERENCES tb_usuarios(id_usuario),
  token_fcm TEXT NOT NULL,
  preferencias JSONB NOT NULL DEFAULT '{}'::jsonb,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  ultimo_acesso_em TIMESTAMPTZ NOT NULL DEFAULT now(),
  atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_push_dispositivos_usuarios
  ON tb_push_dispositivos (usuario_id) WHERE ativo = TRUE;

CREATE TABLE IF NOT EXISTS tb_push_entregas (
  id_entrega BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  notificacao_id INTEGER NOT NULL REFERENCES tb_notificacoes(id_notificacao) ON DELETE CASCADE,
  instalacao_id VARCHAR(80) NOT NULL REFERENCES tb_push_dispositivos(instalacao_id) ON DELETE CASCADE,
  estado VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
  tentativas INTEGER NOT NULL DEFAULT 0,
  proxima_tentativa TIMESTAMPTZ NOT NULL DEFAULT now(),
  bloqueado_em TIMESTAMPTZ,
  enviado_em TIMESTAMPTZ,
  ultimo_erro TEXT,
  UNIQUE (notificacao_id, instalacao_id),
  CONSTRAINT ck_push_estado CHECK (estado IN ('PENDENTE','PROCESSANDO','ERRO','ENVIADO','DESCARTADO'))
);

CREATE INDEX IF NOT EXISTS idx_push_entregas_pendentes
  ON tb_push_entregas (proxima_tentativa, id_entrega)
  WHERE estado IN ('PENDENTE','ERRO','PROCESSANDO');

COMMIT;
