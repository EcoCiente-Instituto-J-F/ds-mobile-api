DROP TABLE IF EXISTS tb_alternativas_quiz;
DROP TABLE IF EXISTS tb_perguntas_quiz;
DROP TABLE IF EXISTS tb_quizzes;
DROP TABLE IF EXISTS tb_aulas;
DROP TABLE IF EXISTS tb_cursos;

CREATE TABLE tb_cursos (
    id_curso INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    titulo_curso VARCHAR(255) NOT NULL,
    descricao_curso TEXT,
    esta_ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE tb_aulas (
    id_aula INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    curso_id INTEGER NOT NULL REFERENCES tb_cursos(id_curso),
    titulo_aula VARCHAR(255) NOT NULL,
    conteudo_aula TEXT,
    ordem INTEGER NOT NULL,
    CONSTRAINT aulas_index_4 UNIQUE (curso_id, ordem)
);

CREATE TABLE tb_quizzes (
    id_quiz INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    curso_id INTEGER NOT NULL REFERENCES tb_cursos(id_curso),
    aula_id INTEGER NOT NULL REFERENCES tb_aulas(id_aula),
    titulo_quiz VARCHAR(255) NOT NULL,
    nota_minima_aprovacao DECIMAL(5,2) NOT NULL DEFAULT 70,
    pontos_recompensa INTEGER NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_quizzes_aula_id UNIQUE (aula_id),
    CONSTRAINT ck_quizzes_nota_minima_aprovacao
        CHECK (nota_minima_aprovacao BETWEEN 0 AND 100),
    CONSTRAINT ck_quizzes_pontos_recompensa
        CHECK (pontos_recompensa > 0)
);

CREATE TABLE tb_perguntas_quiz (
    id_pergunta INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    quiz_id INTEGER NOT NULL REFERENCES tb_quizzes(id_quiz),
    enunciado TEXT NOT NULL,
    ordem INTEGER NOT NULL,
    explicacao TEXT,
    CONSTRAINT perguntas_quiz_index_12 UNIQUE (quiz_id, ordem)
);

CREATE TABLE tb_alternativas_quiz (
    id_alternativa INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pergunta_id INTEGER NOT NULL REFERENCES tb_perguntas_quiz(id_pergunta),
    texto_alternativa VARCHAR(500) NOT NULL,
    correta BOOLEAN NOT NULL DEFAULT FALSE
);

INSERT INTO tb_cursos (id_curso, titulo_curso, esta_ativo)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Reciclagem', TRUE),
    (2, 'Compostagem', TRUE),
    (3, 'Curso inativo', FALSE);

INSERT INTO tb_aulas (id_aula, curso_id, titulo_aula, ordem)
OVERRIDING SYSTEM VALUE
VALUES
    (10, 1, 'Introdução', 1),
    (11, 1, 'Aula sem quiz', 2),
    (12, 1, 'Quiz inativo', 3),
    (13, 1, 'Quiz inconsistente', 4),
    (14, 1, 'Quiz sem perguntas', 5),
    (20, 2, 'Compostagem', 1),
    (30, 3, 'Aula de curso inativo', 1);

INSERT INTO tb_quizzes (
    id_quiz, curso_id, aula_id, titulo_quiz,
    nota_minima_aprovacao, pontos_recompensa, ativo
)
OVERRIDING SYSTEM VALUE
VALUES
    (100, 1, 10, 'Quiz de reciclagem', 70, 30, TRUE),
    (120, 1, 12, 'Quiz inativo', 70, 30, FALSE),
    (130, 2, 13, 'Curso divergente da aula', 70, 30, TRUE),
    (140, 1, 14, 'Quiz sem perguntas', 70, 30, TRUE),
    (200, 2, 20, 'Quiz de compostagem', 80, 40, TRUE),
    (300, 3, 30, 'Quiz de curso inativo', 70, 30, TRUE);

INSERT INTO tb_perguntas_quiz (
    id_pergunta, quiz_id, enunciado, ordem
)
OVERRIDING SYSTEM VALUE
VALUES
    (1000, 100, 'Onde descartar papel?', 2),
    (1001, 100, 'Por que separar resíduos?', 1),
    (2000, 200, 'O que pode ser compostado?', 1);

INSERT INTO tb_alternativas_quiz (
    id_alternativa, pergunta_id, texto_alternativa, correta
)
OVERRIDING SYSTEM VALUE
VALUES
    (5002, 1001, 'Misturar todos os resíduos.', FALSE),
    (5001, 1001, 'Facilitar a reciclagem.', TRUE),
    (5004, 1000, 'No coletor de papel.', TRUE),
    (5003, 1000, 'Em qualquer lugar.', FALSE),
    (6001, 2000, 'Restos de frutas.', TRUE),
    (6002, 2000, 'Vidro.', FALSE);

ALTER TABLE tb_cursos ALTER COLUMN id_curso RESTART WITH 1000;
ALTER TABLE tb_aulas ALTER COLUMN id_aula RESTART WITH 1000;
ALTER TABLE tb_quizzes ALTER COLUMN id_quiz RESTART WITH 1000;
ALTER TABLE tb_perguntas_quiz ALTER COLUMN id_pergunta RESTART WITH 10000;
ALTER TABLE tb_alternativas_quiz ALTER COLUMN id_alternativa RESTART WITH 10000;