-- Apenas no PostgreSQL isolado dos testes. Replica as tabelas informadas pelo usuário.
CREATE TABLE tb_conversas (
  id_conversa INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  condominio_id INTEGER NOT NULL REFERENCES tb_condominios(id_condominio),
  cooperativa_id INTEGER NOT NULL REFERENCES tb_cooperativas(id_cooperativa),
  criada_em TIMESTAMPTZ NOT NULL DEFAULT now(),
  ultima_mensagem_em TIMESTAMPTZ,
  ativa BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uq_conversas_condominio_cooperativa UNIQUE(condominio_id, cooperativa_id)
);
CREATE TABLE tb_mensagens_conversas (
  id_mensagem BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  conversa_id INTEGER NOT NULL REFERENCES tb_conversas(id_conversa),
  remetente_usuario_id INTEGER NOT NULL REFERENCES tb_usuarios(id_usuario),
  remetente_tipo VARCHAR(20) NOT NULL CHECK(remetente_tipo IN ('COOPERATIVA','CONDOMINIO')),
  conteudo TEXT NOT NULL CHECK(length(btrim(conteudo)) BETWEEN 1 AND 2000),
  enviada_em TIMESTAMPTZ NOT NULL DEFAULT now(),
  lida_em TIMESTAMPTZ CHECK(lida_em IS NULL OR lida_em >= enviada_em)
);
INSERT INTO tb_lkp_tipos_usuarios(id_tipo_usuario,nome_tipo)
OVERRIDING SYSTEM VALUE VALUES (3,'Morador');
INSERT INTO tb_usuarios(id_usuario,nome_usuario,email_usuario,senha_hash,ativo,tipo_usuario_id)
OVERRIDING SYSTEM VALUE VALUES
(60,'Outro síndico','sindico2@teste.com','hash',TRUE,2),
(70,'Morador teste','morador@teste.com','hash',TRUE,3);
INSERT INTO tb_sindicos(id_sindico,usuario_id) OVERRIDING SYSTEM VALUE VALUES (2,60);
INSERT INTO tb_condominios(id_condominio,nome_condominio,tipo_condominio_id,sindico_id)
OVERRIDING SYSTEM VALUE VALUES (3,'Condomínio Três',1,2);
