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
    curso_id INTEGER NOT NULL,
    titulo_aula VARCHAR(255) NOT NULL,
    conteudo_aula TEXT,
    ordem INTEGER NOT NULL,
    CONSTRAINT fk_aulas_curso_id
        FOREIGN KEY (curso_id) REFERENCES tb_cursos(id_curso),
    CONSTRAINT aulas_index_4 UNIQUE (curso_id, ordem)
);

INSERT INTO tb_cursos (
    id_curso, titulo_curso, descricao_curso, esta_ativo
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Reciclagem', 'Separação de resíduos.', TRUE),
    (2, 'Compostagem', NULL, TRUE),
    (3, 'Curso inativo', 'Indisponível.', FALSE),
    (4, 'Curso sem aulas', NULL, TRUE);

INSERT INTO tb_aulas (
    id_aula, curso_id, titulo_aula, conteudo_aula, ordem
)
OVERRIDING SYSTEM VALUE
VALUES
    (10, 1, 'Separação', 'Separe os resíduos por categoria.', 2),
    (11, 1, 'Introdução', 'Por que reciclar?', 1),
    (12, 1, 'Revisão', NULL, 3),
    (20, 2, 'Compostagem doméstica', 'Como montar uma composteira.', 1),
    (30, 3, 'Aula indisponível', 'Conteúdo do curso inativo.', 1);

ALTER TABLE tb_cursos ALTER COLUMN id_curso RESTART WITH 100;
ALTER TABLE tb_aulas ALTER COLUMN id_aula RESTART WITH 100;