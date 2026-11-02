--
-- PostgreSQL database dump
--


-- Dumped from database version 18.6 (4e5fd08)
-- Dumped by pg_dump version 18.6

-- Started on 2026-10-29 23:14:23

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 5 (class 2615 OID 2200)
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--



--
-- TOC entry 405 (class 1255 OID 25567)
-- Name: fn_calcular_trust_score(integer, integer, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_calcular_trust_score(p_postagens_validadas integer, p_denuncias_realizadas integer, p_denuncias_procedentes integer) RETURNS numeric
    LANGUAGE plpgsql IMMUTABLE
    AS $$
DECLARE
    v_taxa_acerto DECIMAL(5,2);
    v_score       DECIMAL(6,2);
BEGIN
    IF p_denuncias_realizadas > 0 THEN
        v_taxa_acerto := ROUND(100.0 * p_denuncias_procedentes / p_denuncias_realizadas, 2);
    ELSE
        v_taxa_acerto := 0;
    END IF;

    -- Regra de negocio:
    --   +2 pontos por postagem validada sem contestacao
    --   +5 pontos por denuncia procedente (credibilidade como moderador)
    --   + metade da taxa de acerto de denuncias (0 a 50 pontos)
    --   teto de 1000 pontos
    v_score := LEAST(
        (p_postagens_validadas * 2) + (p_denuncias_procedentes * 5) + (v_taxa_acerto * 0.5),
        1000
    );

    RETURN v_score;
END;
$$;


--
-- TOC entry 404 (class 1255 OID 25566)
-- Name: fn_pontos_disponiveis_postagem(integer, integer, timestamp with time zone, text); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_pontos_disponiveis_postagem(p_usuario_id integer, p_categoria_id integer, p_data_referencia timestamp with time zone DEFAULT now(), p_timezone text DEFAULT 'America/Sao_Paulo'::text) RETURNS integer
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_pontos_base      INTEGER;
    v_limite_diario    INTEGER;
    v_pontos_validos   INTEGER;
    v_pontos_conceder  INTEGER;
    v_data_local       DATE;
    v_inicio_dia       TIMESTAMPTZ;
    v_fim_dia          TIMESTAMPTZ;
BEGIN
    SELECT pontos_base, limite_pontos_diario
      INTO v_pontos_base, v_limite_diario
      FROM tb_lkp_categorias_residuos
     WHERE id_categoria = p_categoria_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Categoria de residuo % nao encontrada', p_categoria_id;
    END IF;

    IF v_limite_diario IS NULL THEN
        RETURN v_pontos_base;
    END IF;

    -- Converte a referencia para o dia civil do timezone configurado e cria
    -- limites TIMESTAMPTZ para permitir filtro eficiente em data_postagem.
    v_data_local := (p_data_referencia AT TIME ZONE p_timezone)::DATE;
    v_inicio_dia := (v_data_local::TIMESTAMP AT TIME ZONE p_timezone);
    v_fim_dia    := ((v_data_local + 1)::TIMESTAMP AT TIME ZONE p_timezone);

    SELECT COALESCE(SUM(
        CASE mp.tipo_movimentacao
            WHEN 'CREDITO'     THEN mp.pontos
            WHEN 'RESTAURACAO' THEN mp.pontos
            WHEN 'ESTORNO'     THEN -mp.pontos
            ELSE 0
        END
    ), 0)::INTEGER
      INTO v_pontos_validos
      FROM tb_movimentacoes_pontos mp
      JOIN tb_postagens p ON p.id_postagem = mp.postagem_id
     WHERE mp.origem_tipo = 'POSTAGEM'
       AND mp.usuario_id = p_usuario_id
       AND mp.categoria_id = p_categoria_id
       AND p.data_postagem >= v_inicio_dia
       AND p.data_postagem <  v_fim_dia;

    IF v_pontos_validos < 0 THEN
        RAISE EXCEPTION 'Inconsistencia no ledger: saldo diario negativo para usuario %, categoria %',
            p_usuario_id, p_categoria_id;
    END IF;

    v_pontos_conceder := GREATEST(
        LEAST(v_pontos_base, v_limite_diario - v_pontos_validos),
        0
    );

    RETURN v_pontos_conceder;
END;
$$;


--
-- TOC entry 411 (class 1255 OID 25574)
-- Name: fn_trg_auditoria_agendamentos_coletas(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_trg_auditoria_agendamentos_coletas() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_auditoria_id      BIGINT;
    v_tipo_evento_id    INTEGER;
    v_tipo_operacao_id  INTEGER;
BEGIN
    SELECT id_tipo_evento_auditado INTO v_tipo_evento_id
      FROM tb_lkp_tipos_eventos_auditados WHERE nome_evento = 'agendamento_coleta';

    SELECT id_tipo_operacao INTO v_tipo_operacao_id
      FROM tb_lkp_tipos_operacoes_auditoria WHERE nome_operacao = TG_OP;

    INSERT INTO tb_log_auditoria (tipo_evento_auditado_id, tipo_operacao_id, executado_por, executado_em)
    VALUES (v_tipo_evento_id, v_tipo_operacao_id, CURRENT_USER, now())
    RETURNING id_auditoria INTO v_auditoria_id;

    IF TG_OP = 'INSERT' THEN
        INSERT INTO tb_log_auditoria_agendamentos_coletas (
            auditoria_id, agendamento_coleta_id,
            status_anterior_id, status_novo_id,
            data_inicio_anterior, data_inicio_novo,
            data_fim_anterior, data_fim_novo
        ) VALUES (
            v_auditoria_id, NEW.id_agendamento_coleta,
            NULL, NEW.status_agendamento_id,
            NULL, NEW.data_inicio,
            NULL, NEW.data_fim
        );
        RETURN NEW;

    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO tb_log_auditoria_agendamentos_coletas (
            auditoria_id, agendamento_coleta_id,
            status_anterior_id, status_novo_id,
            data_inicio_anterior, data_inicio_novo,
            data_fim_anterior, data_fim_novo
        ) VALUES (
            v_auditoria_id, NEW.id_agendamento_coleta,
            OLD.status_agendamento_id, NEW.status_agendamento_id,
            OLD.data_inicio, NEW.data_inicio,
            OLD.data_fim, NEW.data_fim
        );
        RETURN NEW;

    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO tb_log_auditoria_agendamentos_coletas (
            auditoria_id, agendamento_coleta_id,
            status_anterior_id, status_novo_id,
            data_inicio_anterior, data_inicio_novo,
            data_fim_anterior, data_fim_novo
        ) VALUES (
            v_auditoria_id, OLD.id_agendamento_coleta,
            OLD.status_agendamento_id, NULL,
            OLD.data_inicio, NULL,
            OLD.data_fim, NULL
        );
        RETURN OLD;
    END IF;

    RETURN NULL;
END;
$$;


--
-- TOC entry 410 (class 1255 OID 25572)
-- Name: fn_trg_auditoria_postagens(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_trg_auditoria_postagens() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_auditoria_id      BIGINT;
    v_tipo_evento_id    INTEGER;
    v_tipo_operacao_id  INTEGER;
BEGIN
    SELECT id_tipo_evento_auditado INTO v_tipo_evento_id
      FROM tb_lkp_tipos_eventos_auditados WHERE nome_evento = 'postagem';

    SELECT id_tipo_operacao INTO v_tipo_operacao_id
      FROM tb_lkp_tipos_operacoes_auditoria WHERE nome_operacao = TG_OP;

    INSERT INTO tb_log_auditoria (tipo_evento_auditado_id, tipo_operacao_id, executado_por, executado_em)
    VALUES (v_tipo_evento_id, v_tipo_operacao_id, CURRENT_USER, now())
    RETURNING id_auditoria INTO v_auditoria_id;

    IF TG_OP = 'INSERT' THEN
        INSERT INTO tb_log_auditoria_postagens (
            auditoria_id, postagem_id,
            status_validacao_anterior_id, status_validacao_novo_id,
            saldo_confianca_anterior, saldo_confianca_novo
        ) VALUES (
            v_auditoria_id, NEW.id_postagem,
            NULL, NEW.status_validacao_id,
            NULL, NEW.saldo_confianca
        );
        RETURN NEW;

    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO tb_log_auditoria_postagens (
            auditoria_id, postagem_id,
            status_validacao_anterior_id, status_validacao_novo_id,
            saldo_confianca_anterior, saldo_confianca_novo
        ) VALUES (
            v_auditoria_id, NEW.id_postagem,
            OLD.status_validacao_id, NEW.status_validacao_id,
            OLD.saldo_confianca, NEW.saldo_confianca
        );
        RETURN NEW;

    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO tb_log_auditoria_postagens (
            auditoria_id, postagem_id,
            status_validacao_anterior_id, status_validacao_novo_id,
            saldo_confianca_anterior, saldo_confianca_novo
        ) VALUES (
            v_auditoria_id, OLD.id_postagem,
            OLD.status_validacao_id, NULL,
            OLD.saldo_confianca, NULL
        );
        RETURN OLD;
    END IF;

    RETURN NULL;
END;
$$;


--
-- TOC entry 412 (class 1255 OID 25576)
-- Name: fn_trg_auditoria_usuarios_condominios(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_trg_auditoria_usuarios_condominios() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_auditoria_id      BIGINT;
    v_tipo_evento_id    INTEGER;
    v_tipo_operacao_id  INTEGER;
BEGIN
    SELECT id_tipo_evento_auditado INTO v_tipo_evento_id
      FROM tb_lkp_tipos_eventos_auditados WHERE nome_evento = 'usuario_condominio';

    SELECT id_tipo_operacao INTO v_tipo_operacao_id
      FROM tb_lkp_tipos_operacoes_auditoria WHERE nome_operacao = TG_OP;

    INSERT INTO tb_log_auditoria (tipo_evento_auditado_id, tipo_operacao_id, executado_por, executado_em)
    VALUES (v_tipo_evento_id, v_tipo_operacao_id, CURRENT_USER, now())
    RETURNING id_auditoria INTO v_auditoria_id;

    IF TG_OP = 'INSERT' THEN
        INSERT INTO tb_log_auditoria_usuarios_condominios (
            auditoria_id, usuario_condominio_id,
            aprovado_anterior, aprovado_novo,
            data_saida_anterior, data_saida_novo
        ) VALUES (
            v_auditoria_id, NEW.id_usuario_condominio,
            NULL, NEW.aprovado,
            NULL, NEW.data_saida
        );
        RETURN NEW;

    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO tb_log_auditoria_usuarios_condominios (
            auditoria_id, usuario_condominio_id,
            aprovado_anterior, aprovado_novo,
            data_saida_anterior, data_saida_novo
        ) VALUES (
            v_auditoria_id, NEW.id_usuario_condominio,
            OLD.aprovado, NEW.aprovado,
            OLD.data_saida, NEW.data_saida
        );
        RETURN NEW;

    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO tb_log_auditoria_usuarios_condominios (
            auditoria_id, usuario_condominio_id,
            aprovado_anterior, aprovado_novo,
            data_saida_anterior, data_saida_novo
        ) VALUES (
            v_auditoria_id, OLD.id_usuario_condominio,
            OLD.aprovado, NULL,
            OLD.data_saida, NULL
        );
        RETURN OLD;
    END IF;

    RETURN NULL;
END;
$$;


--
-- TOC entry 418 (class 1255 OID 25800)
-- Name: fn_trg_validar_mensagem_conversa(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_trg_validar_mensagem_conversa() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_condominio_id  INTEGER;
    v_cooperativa_id INTEGER;
    v_ativa          BOOLEAN;
BEGIN
    -- Trava a conversa para serializar a checagem de "primeira mensagem".
    SELECT condominio_id, cooperativa_id, ativa
      INTO v_condominio_id, v_cooperativa_id, v_ativa
      FROM tb_conversas
     WHERE id_conversa = NEW.conversa_id
     FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Conversa % nao encontrada', NEW.conversa_id;
    END IF;

    IF NOT v_ativa THEN
        RAISE EXCEPTION 'Conversa % esta inativa e nao aceita novas mensagens', NEW.conversa_id;
    END IF;

    IF NOT fn_usuario_representa_lado(NEW.remetente_usuario_id, NEW.remetente_tipo,
                                      v_condominio_id, v_cooperativa_id) THEN
        RAISE EXCEPTION 'Usuario % nao pode enviar mensagem como % nesta conversa',
            NEW.remetente_usuario_id, NEW.remetente_tipo;
    END IF;

    IF NEW.remetente_tipo = 'CONDOMINIO'
       AND NOT EXISTS (SELECT 1 FROM tb_mensagens_conversas WHERE conversa_id = NEW.conversa_id) THEN
        RAISE EXCEPTION 'A conversa % deve ser iniciada pela cooperativa', NEW.conversa_id;
    END IF;

    UPDATE tb_conversas
       -- GREATEST: mensagem com data antiga (carga historica) nao volta o relogio
       SET ultima_mensagem_em = GREATEST(ultima_mensagem_em, NEW.enviada_em)
     WHERE id_conversa = NEW.conversa_id;

    RETURN NEW;
END;
$$;


--
-- TOC entry 417 (class 1255 OID 25741)
-- Name: fn_trg_validar_partes_agendamento(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_trg_validar_partes_agendamento() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_lado_resposta VARCHAR(20);
BEGIN
    -- NULL so existe em linhas legadas nao migradas
    IF NEW.solicitado_por_usuario_id IS NOT NULL
       AND NOT fn_usuario_representa_lado(NEW.solicitado_por_usuario_id, NEW.origem_solicitacao,
                                          NEW.condominio_id, NEW.cooperativa_id) THEN
        RAISE EXCEPTION 'Usuario % nao representa o lado % deste agendamento',
            NEW.solicitado_por_usuario_id, NEW.origem_solicitacao;
    END IF;

    IF NEW.respondido_por_usuario_id IS NOT NULL THEN
        v_lado_resposta := CASE NEW.origem_solicitacao
                               WHEN 'CONDOMINIO' THEN 'COOPERATIVA'
                               ELSE 'CONDOMINIO'
                           END;

        IF NOT fn_usuario_representa_lado(NEW.respondido_por_usuario_id, v_lado_resposta,
                                          NEW.condominio_id, NEW.cooperativa_id) THEN
            RAISE EXCEPTION 'Somente o lado % pode aceitar/recusar esta solicitacao', v_lado_resposta;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;


--
-- TOC entry 414 (class 1255 OID 25740)
-- Name: fn_usuario_representa_lado(integer, character varying, integer, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_usuario_representa_lado(p_usuario_id integer, p_lado character varying, p_condominio_id integer, p_cooperativa_id integer) RETURNS boolean
    LANGUAGE sql STABLE
    AS $$
    SELECT CASE p_lado
        WHEN 'CONDOMINIO' THEN EXISTS (
            SELECT 1
              FROM tb_condominios c
              JOIN tb_sindicos s ON s.id_sindico = c.sindico_id
             WHERE c.id_condominio = p_condominio_id
               AND s.usuario_id    = p_usuario_id)
        WHEN 'COOPERATIVA' THEN EXISTS (
            SELECT 1
              FROM tb_cooperativas coop
             WHERE coop.id_cooperativa = p_cooperativa_id
               AND coop.usuario_id     = p_usuario_id)
        ELSE FALSE
    END;
$$;


--
-- TOC entry 392 (class 1255 OID 25458)
-- Name: fn_validar_referencia_movimentacao_pontos(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_validar_referencia_movimentacao_pontos() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_ref tb_movimentacoes_pontos%ROWTYPE;
BEGIN
    IF NEW.tipo_movimentacao IN ('ESTORNO', 'RESTAURACAO') THEN
        SELECT *
          INTO v_ref
          FROM tb_movimentacoes_pontos
         WHERE id_movimentacao = NEW.movimentacao_referencia_id;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'Movimentacao de referencia % nao encontrada', NEW.movimentacao_referencia_id;
        END IF;

        IF v_ref.tipo_movimentacao <> 'CREDITO' THEN
            RAISE EXCEPTION 'ESTORNO/RESTAURACAO deve referenciar um CREDITO original';
        END IF;

        IF v_ref.usuario_id <> NEW.usuario_id
           OR v_ref.origem_tipo <> NEW.origem_tipo
           OR v_ref.pontos <> NEW.pontos
           OR v_ref.postagem_id IS DISTINCT FROM NEW.postagem_id
           OR v_ref.tentativa_quiz_id IS DISTINCT FROM NEW.tentativa_quiz_id THEN
            RAISE EXCEPTION 'Movimentacao de referencia nao corresponde a mesma origem/usuario/valor';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;


--
-- TOC entry 409 (class 1255 OID 25571)
-- Name: sp_atualizar_trust_score(integer, integer); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_atualizar_trust_score(IN p_usuario_id integer, IN p_condominio_id integer)
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_postagens_validadas  INTEGER;
    v_denuncias_realizadas INTEGER;
    v_denuncias_procedentes INTEGER;
    v_taxa_acerto          DECIMAL(5,2);
    v_novo_score           DECIMAL(6,2);
    v_nivel_confiavel_id   INTEGER;
    v_limite_promocao      CONSTANT DECIMAL := 200.00;
BEGIN
    -- Postagens aprovadas do usuario nesse condominio que nunca receberam denuncia
    SELECT COUNT(*) INTO v_postagens_validadas
      FROM tb_postagens p
      JOIN tb_lkp_status_validacoes_postagens svp ON svp.id_status_validacao = p.status_validacao_id
     WHERE p.usuario_id    = p_usuario_id
       AND p.condominio_id = p_condominio_id
       AND svp.nome_status = 'aprovada'
       AND p.resolvido_em IS NOT NULL
       AND NOT EXISTS (
            SELECT 1
              FROM tb_rel_votos_postagens vp
              JOIN tb_lkp_tipos_votos_postagens tvp ON tvp.id_tipo_voto = vp.tipo_voto_id
             WHERE vp.postagem_id = p.id_postagem
               AND tvp.nome_tipo  = 'denunciar'
       );

    -- Total de denuncias feitas pelo usuario (como denunciante)
    SELECT COUNT(*) FILTER (WHERE tvp.nome_tipo = 'denunciar')
      INTO v_denuncias_realizadas
      FROM tb_rel_votos_postagens vp
      JOIN tb_lkp_tipos_votos_postagens tvp ON tvp.id_tipo_voto = vp.tipo_voto_id
     WHERE vp.usuario_id = p_usuario_id;

    -- Denuncias do usuario cuja postagem denunciada acabou reprovada (procedentes)
    SELECT COUNT(*) FILTER (WHERE tvp.nome_tipo = 'denunciar' AND svp.nome_status = 'reprovada')
      INTO v_denuncias_procedentes
      FROM tb_rel_votos_postagens vp
      JOIN tb_lkp_tipos_votos_postagens tvp        ON tvp.id_tipo_voto = vp.tipo_voto_id
      JOIN tb_postagens p                      ON p.id_postagem = vp.postagem_id
      JOIN tb_lkp_status_validacoes_postagens svp  ON svp.id_status_validacao = p.status_validacao_id
     WHERE vp.usuario_id = p_usuario_id
       AND p.resolvido_em IS NOT NULL;

    IF v_denuncias_realizadas > 0 THEN
        v_taxa_acerto := ROUND(100.0 * v_denuncias_procedentes / v_denuncias_realizadas, 2);
    ELSE
        v_taxa_acerto := NULL;
    END IF;

    v_novo_score := fn_calcular_trust_score(v_postagens_validadas, v_denuncias_realizadas, v_denuncias_procedentes);

    UPDATE tb_rel_usuarios_condominios
       SET postagens_validadas_sem_contestacao = v_postagens_validadas,
           denuncias_realizadas                = v_denuncias_realizadas,
           denuncias_procedentes                = v_denuncias_procedentes,
           taxa_acerto_denuncias                = v_taxa_acerto,
           trust_score                          = v_novo_score
     WHERE usuario_id    = p_usuario_id
       AND condominio_id = p_condominio_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Vinculo usuario % / condominio % nao encontrado', p_usuario_id, p_condominio_id;
    END IF;

    -- Promocao automatica a pessoa_confiavel ao ultrapassar o limiar
    IF v_novo_score >= v_limite_promocao THEN
        SELECT id_nivel_confianca INTO v_nivel_confiavel_id
          FROM tb_lkp_niveis_confianca
         WHERE nome_nivel = 'pessoa_confiavel';

        IF v_nivel_confiavel_id IS NOT NULL THEN
            UPDATE tb_rel_usuarios_condominios
               SET nivel_confianca_id = v_nivel_confiavel_id
             WHERE usuario_id         = p_usuario_id
               AND condominio_id      = p_condominio_id
               AND nivel_confianca_id <> v_nivel_confiavel_id;
        END IF;
    END IF;
END;
$$;


--
-- TOC entry 391 (class 1255 OID 74117)
-- Name: sp_carga_datamart(); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_carga_datamart()
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_data_min DATE;
    v_data_max DATE;

    v_qtd_dim_tempo INTEGER;
    v_qtd_dim_usuario INTEGER;
    v_qtd_dim_condominio INTEGER;
    v_qtd_dim_cooperativa INTEGER;
    v_qtd_dim_categoria INTEGER;
    v_qtd_dim_status_postagem INTEGER;
    v_qtd_dim_status_agendamento INTEGER;
    v_qtd_fato_postagens INTEGER;
    v_qtd_fato_coletas INTEGER;
BEGIN
    -- ------------------------------------------------------------------------
    -- 4.1 Limpeza das tabelas do Data Mart
    -- ------------------------------------------------------------------------

    TRUNCATE TABLE
        fato_postagens,
        fato_coletas,
        dim_usuario,
        dim_condominio,
        dim_cooperativa,
        dim_categoria_residuo,
        dim_status_postagem,
        dim_status_agendamento
    RESTART IDENTITY CASCADE;

    DELETE FROM dim_tempo;

    -- ------------------------------------------------------------------------
    -- 4.2 DIM_TEMPO
    -- ------------------------------------------------------------------------

    SELECT
        LEAST(
            COALESCE(
                (SELECT MIN(data_postagem)::DATE FROM tb_postagens),
                DATE '2999-12-31'
            ),
            COALESCE(
                (SELECT MIN(data_visita)::DATE FROM tb_visitas_coletas),
                DATE '2999-12-31'
            )
        ),
        GREATEST(
            COALESCE(
                (SELECT MAX(data_postagem)::DATE FROM tb_postagens),
                DATE '1900-01-01'
            ),
            COALESCE(
                (SELECT MAX(data_visita)::DATE FROM tb_visitas_coletas),
                DATE '1900-01-01'
            )
        )
    INTO v_data_min, v_data_max;

    IF v_data_min = DATE '2999-12-31'
       OR v_data_max = DATE '1900-01-01' THEN
        v_data_min := CURRENT_DATE;
        v_data_max := CURRENT_DATE;
    END IF;

    -- Margem de 30 dias para cada lado do período encontrado.
    v_data_min := v_data_min - 30;
    v_data_max := v_data_max + 30;

    INSERT INTO dim_tempo (
        sk_tempo,
        data_completa,
        ano,
        trimestre,
        mes,
        nome_mes,
        dia,
        dia_semana_numero,
        nome_dia_semana,
        is_fim_de_semana
    )
    SELECT
        TO_CHAR(d, 'YYYYMMDD')::INTEGER,
        d::DATE,
        EXTRACT(YEAR FROM d)::INTEGER,
        EXTRACT(QUARTER FROM d)::INTEGER,
        EXTRACT(MONTH FROM d)::INTEGER,
        TRIM(TO_CHAR(d, 'TMMonth')),
        EXTRACT(DAY FROM d)::INTEGER,
        EXTRACT(DOW FROM d)::INTEGER,
        TRIM(TO_CHAR(d, 'TMDay')),
        EXTRACT(DOW FROM d) IN (0, 6)
    FROM generate_series(
        v_data_min,
        v_data_max,
        INTERVAL '1 day'
    ) AS d;

    -- ------------------------------------------------------------------------
    -- 4.3 DIM_USUARIO
    -- ------------------------------------------------------------------------

    INSERT INTO dim_usuario (
        id_usuario,
        nome_usuario,
        email_usuario,
        tipo_usuario,
        ativo
    )
    SELECT
        u.id_usuario,
        u.nome_usuario,
        u.email_usuario,
        tu.nome_tipo,
        u.ativo
    FROM tb_usuarios u
    LEFT JOIN tb_lkp_tipos_usuarios tu
        ON tu.id_tipo_usuario = u.tipo_usuario_id;

    -- ------------------------------------------------------------------------
    -- 4.4 DIM_CONDOMINIO
    -- ------------------------------------------------------------------------

    INSERT INTO dim_condominio (
        id_condominio,
        nome_condominio,
        tipo_condominio,
        cidade,
        estado,
        ativo
    )
    SELECT
        c.id_condominio,
        c.nome_condominio,
        tc.nome_tipo,
        e.cidade,
        e.estado,
        c.ativo
    FROM tb_condominios c
    LEFT JOIN tb_lkp_tipos_condominios tc
        ON tc.id_tipo_condominio = c.tipo_condominio_id
    LEFT JOIN tb_enderecos e
        ON e.id_endereco = c.endereco_id;

    -- ------------------------------------------------------------------------
    -- 4.5 DIM_COOPERATIVA
    -- ------------------------------------------------------------------------

    INSERT INTO dim_cooperativa (
        id_cooperativa,
        nome_cooperativa,
        cidade,
        estado
    )
    SELECT
        co.id_cooperativa,
        co.nome_cooperativa,
        e.cidade,
        e.estado
    FROM tb_cooperativas co
    LEFT JOIN tb_enderecos e
        ON e.id_endereco = co.endereco_id;

    -- ------------------------------------------------------------------------
    -- 4.6 DIM_CATEGORIA_RESIDUO
    -- Correção: a coluna da origem é limite_pontos_diario.
    -- ------------------------------------------------------------------------

    INSERT INTO dim_categoria_residuo (
        id_categoria,
        nome_categoria,
        permite_reciclagem,
        pontos_base,
        limite_pontos_ciclo
    )
    SELECT
        id_categoria,
        nome_categoria,
        permite_reciclagem,
        pontos_base,
        limite_pontos_diario
    FROM tb_lkp_categorias_residuos;

    -- ------------------------------------------------------------------------
    -- 4.7 DIM_STATUS_POSTAGEM
    -- ------------------------------------------------------------------------

    INSERT INTO dim_status_postagem (
        id_status_validacao,
        codigo,
        descricao
    )
    SELECT
        id_status_validacao,
        nome_status,
        descricao
    FROM tb_lkp_status_validacoes_postagens;

    -- ------------------------------------------------------------------------
    -- 4.8 DIM_STATUS_AGENDAMENTO
    -- ------------------------------------------------------------------------

    INSERT INTO dim_status_agendamento (
        id_status,
        nome_status
    )
    SELECT
        id_status,
        nome_status
    FROM tb_lkp_status_agendamentos;

    -- ------------------------------------------------------------------------
    -- 4.9 FATO_POSTAGENS
    -- ------------------------------------------------------------------------

    INSERT INTO fato_postagens (
        id_postagem,
        sk_tempo,
        sk_usuario,
        sk_condominio,
        sk_categoria,
        sk_status_postagem,
        qtd_postagens,
        foi_aprovada,
        pontos_base_categoria
    )
    SELECT
        p.id_postagem,
        TO_CHAR(p.data_postagem, 'YYYYMMDD')::INTEGER,
        du.sk_usuario,
        dc.sk_condominio,
        dcat.sk_categoria,
        dsp.sk_status_postagem,
        1,
        LOWER(dsp.codigo) = 'aprovada',
        COALESCE(dcat.pontos_base, 0)
    FROM tb_postagens p
    JOIN dim_usuario du
        ON du.id_usuario = p.usuario_id
    JOIN dim_condominio dc
        ON dc.id_condominio = p.condominio_id
    JOIN dim_categoria_residuo dcat
        ON dcat.id_categoria = p.categoria_id
    JOIN dim_status_postagem dsp
        ON dsp.id_status_validacao = p.status_validacao_id;

    -- ------------------------------------------------------------------------
    -- 4.10 FATO_COLETAS
    -- ------------------------------------------------------------------------

    INSERT INTO fato_coletas (
        id_visita_coleta,
        sk_tempo,
        sk_condominio,
        sk_cooperativa,
        sk_status_agendamento,
        foi_realizada,
        houve_confirmacao,
        qtd_visitas
    )
    SELECT
        vc.id_visita_coleta,
        TO_CHAR(vc.data_visita, 'YYYYMMDD')::INTEGER,
        dc.sk_condominio,
        dcoop.sk_cooperativa,
        dsa.sk_status_agendamento,
        vc.foi_realizada,
        vc.houve_confirmacao,
        1
    FROM tb_visitas_coletas vc
    JOIN tb_agendamentos_coletas ac
        ON ac.id_agendamento_coleta = vc.agendamento_coleta_id
    JOIN dim_condominio dc
        ON dc.id_condominio = ac.condominio_id
    JOIN dim_cooperativa dcoop
        ON dcoop.id_cooperativa = ac.cooperativa_id
    JOIN dim_status_agendamento dsa
        ON dsa.id_status = ac.status_agendamento_id;

    -- ------------------------------------------------------------------------
    -- 4.11 Contagens para validação
    -- ------------------------------------------------------------------------

    SELECT COUNT(*) INTO v_qtd_dim_tempo
    FROM dim_tempo;

    SELECT COUNT(*) INTO v_qtd_dim_usuario
    FROM dim_usuario;

    SELECT COUNT(*) INTO v_qtd_dim_condominio
    FROM dim_condominio;

    SELECT COUNT(*) INTO v_qtd_dim_cooperativa
    FROM dim_cooperativa;

    SELECT COUNT(*) INTO v_qtd_dim_categoria
    FROM dim_categoria_residuo;

    SELECT COUNT(*) INTO v_qtd_dim_status_postagem
    FROM dim_status_postagem;

    SELECT COUNT(*) INTO v_qtd_dim_status_agendamento
    FROM dim_status_agendamento;

    SELECT COUNT(*) INTO v_qtd_fato_postagens
    FROM fato_postagens;

    SELECT COUNT(*) INTO v_qtd_fato_coletas
    FROM fato_coletas;

    -- Atualiza estatísticas utilizadas pelo otimizador.
    ANALYZE dim_tempo;
    ANALYZE dim_usuario;
    ANALYZE dim_condominio;
    ANALYZE dim_cooperativa;
    ANALYZE dim_categoria_residuo;
    ANALYZE dim_status_postagem;
    ANALYZE dim_status_agendamento;
    ANALYZE fato_postagens;
    ANALYZE fato_coletas;

    -- ------------------------------------------------------------------------
    -- 4.12 Validações básicas
    -- ------------------------------------------------------------------------

    IF v_qtd_dim_tempo = 0 THEN
        RAISE EXCEPTION
            'ETL interrompido: dim_tempo ficou vazia.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM fato_postagens
        WHERE sk_tempo IS NULL
           OR sk_usuario IS NULL
           OR sk_condominio IS NULL
           OR sk_categoria IS NULL
           OR sk_status_postagem IS NULL
    ) THEN
        RAISE EXCEPTION
            'ETL interrompido: existem chaves dimensionais nulas em fato_postagens.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM fato_coletas
        WHERE sk_tempo IS NULL
           OR sk_condominio IS NULL
           OR sk_cooperativa IS NULL
           OR sk_status_agendamento IS NULL
    ) THEN
        RAISE EXCEPTION
            'ETL interrompido: existem chaves dimensionais nulas em fato_coletas.';
    END IF;

    -- ------------------------------------------------------------------------
    -- 4.13 Resultado da carga
    -- ------------------------------------------------------------------------

    RAISE NOTICE
        'Data Mart carregado | dim_tempo=% | dim_usuario=% | dim_condominio=% | dim_cooperativa=% | dim_categoria=% | dim_status_postagem=% | dim_status_agendamento=% | fato_postagens=% | fato_coletas=%',
        v_qtd_dim_tempo,
        v_qtd_dim_usuario,
        v_qtd_dim_condominio,
        v_qtd_dim_cooperativa,
        v_qtd_dim_categoria,
        v_qtd_dim_status_postagem,
        v_qtd_dim_status_agendamento,
        v_qtd_fato_postagens,
        v_qtd_fato_coletas;
END;
$$;


--
-- TOC entry 413 (class 1255 OID 25716)
-- Name: sp_consolidar_metricas_dau(date, date); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_consolidar_metricas_dau(IN p_inicio date, IN p_fim date DEFAULT CURRENT_DATE)
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF p_fim < p_inicio THEN
        RAISE EXCEPTION 'Intervalo invalido: % > %', p_inicio, p_fim;
    END IF;

    INSERT INTO tb_metricas_dau (
        data_referencia, dau, wau, mau, novos_usuarios, usuarios_retornantes,
        sessoes_total, minutos_medios, stickiness, calculado_em
    )
    SELECT
        g.dia,
        j.dau,
        j.wau,
        j.mau,
        n.novos,
        j.dau - n.novos,
        j.sessoes,
        j.minutos_medios,
        ROUND(100.0 * j.dau / NULLIF(j.mau, 0), 2),
        now()
    FROM generate_series(p_inicio, p_fim, INTERVAL '1 day') AS g0(ts)
    CROSS JOIN LATERAL (SELECT g0.ts::DATE AS dia) g
    CROSS JOIN LATERAL (
        SELECT
            COUNT(DISTINCT a.usuario_id) FILTER (WHERE a.data_atividade = g.dia)::INTEGER     AS dau,
            COUNT(DISTINCT a.usuario_id) FILTER (WHERE a.data_atividade > g.dia - 7)::INTEGER AS wau,
            COUNT(DISTINCT a.usuario_id)::INTEGER                                              AS mau,
            COALESCE(SUM(a.qtd_sessoes) FILTER (WHERE a.data_atividade = g.dia), 0)::INTEGER  AS sessoes,
            ROUND(AVG(a.minutos_ativos) FILTER (WHERE a.data_atividade = g.dia), 2)           AS minutos_medios
          FROM tb_atividades_diarias_usuarios a
         WHERE a.data_atividade BETWEEN g.dia - 29 AND g.dia
    ) j
    CROSS JOIN LATERAL (
        SELECT COUNT(*)::INTEGER AS novos
          FROM tb_atividades_diarias_usuarios h
         WHERE h.data_atividade = g.dia
           AND NOT EXISTS (
                SELECT 1
                  FROM tb_atividades_diarias_usuarios x
                 WHERE x.usuario_id = h.usuario_id
                   AND x.data_atividade < g.dia
           )
    ) n
    ON CONFLICT (data_referencia) DO UPDATE
       SET dau                  = EXCLUDED.dau,
           wau                  = EXCLUDED.wau,
           mau                  = EXCLUDED.mau,
           novos_usuarios       = EXCLUDED.novos_usuarios,
           usuarios_retornantes = EXCLUDED.usuarios_retornantes,
           sessoes_total        = EXCLUDED.sessoes_total,
           minutos_medios       = EXCLUDED.minutos_medios,
           stickiness           = EXCLUDED.stickiness,
           calculado_em         = EXCLUDED.calculado_em;
END;
$$;


--
-- TOC entry 408 (class 1255 OID 25570)
-- Name: sp_decidir_postagem_analise(integer, boolean); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_decidir_postagem_analise(IN p_postagem_id integer, IN p_aprovar boolean)
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_status_atual_nome       VARCHAR(30);
    v_status_destino_nome     VARCHAR(30);
    v_status_destino_id       INTEGER;
    v_pontuacao_ativa         BOOLEAN;
    v_nova_pontuacao_ativa    BOOLEAN;
    v_mudou_estado            BOOLEAN;
BEGIN
    SELECT sv.nome_status, p.pontuacao_ativa
      INTO v_status_atual_nome, v_pontuacao_ativa
      FROM tb_postagens p
      JOIN tb_lkp_status_validacoes_postagens sv
        ON sv.id_status_validacao = p.status_validacao_id
     WHERE p.id_postagem = p_postagem_id
     FOR UPDATE OF p;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Postagem % nao encontrada', p_postagem_id;
    END IF;

    IF v_status_atual_nome <> 'em_analise' THEN
        RAISE EXCEPTION 'Postagem % nao esta em analise; status atual: %', p_postagem_id, v_status_atual_nome;
    END IF;

    v_status_destino_nome  := CASE WHEN p_aprovar THEN 'aprovada' ELSE 'reprovada' END;
    v_nova_pontuacao_ativa := p_aprovar;
    v_mudou_estado := (v_nova_pontuacao_ativa IS DISTINCT FROM v_pontuacao_ativa);

    SELECT id_status_validacao
      INTO v_status_destino_id
      FROM tb_lkp_status_validacoes_postagens
     WHERE nome_status = v_status_destino_nome;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Status de validacao % nao cadastrado', v_status_destino_nome;
    END IF;

    UPDATE tb_postagens
       SET status_validacao_id = v_status_destino_id,
           resolvido_em = now(),
           pontuacao_ativa = v_nova_pontuacao_ativa,
           pontuacao_reconciliacao_pendente =
               pontuacao_reconciliacao_pendente OR v_mudou_estado
     WHERE id_postagem = p_postagem_id;
END;
$$;


--
-- TOC entry 407 (class 1255 OID 25569)
-- Name: sp_encerrar_janela_postagem(integer); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_encerrar_janela_postagem(IN p_postagem_id integer)
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_saldo                  INTEGER;
    v_data_limite            TIMESTAMPTZ;
    v_resolvido_em           TIMESTAMPTZ;
    v_pontuacao_ativa        BOOLEAN;
    v_nova_pontuacao_ativa   BOOLEAN;
    v_status_destino_id      INTEGER;
    v_status_destino_nome    VARCHAR(30);
    v_mudou_estado           BOOLEAN := FALSE;
BEGIN
    SELECT saldo_confianca,
           data_limite_analise,
           resolvido_em,
           pontuacao_ativa
      INTO v_saldo,
           v_data_limite,
           v_resolvido_em,
           v_pontuacao_ativa
      FROM tb_postagens
     WHERE id_postagem = p_postagem_id
     FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Postagem % nao encontrada', p_postagem_id;
    END IF;

    IF v_resolvido_em IS NOT NULL THEN
        RETURN; -- idempotente para jobs de fechamento
    END IF;

    IF now() < v_data_limite THEN
        RAISE EXCEPTION 'Janela comunitaria da postagem % ainda esta aberta ate %', p_postagem_id, v_data_limite;
    END IF;

    IF v_saldo >= 0 THEN
        v_status_destino_nome := 'aprovada';
    ELSIF v_saldo <= -5 THEN
        v_status_destino_nome := 'reprovada';
    ELSE
        v_status_destino_nome := 'em_analise';
    END IF;

    SELECT id_status_validacao
      INTO v_status_destino_id
      FROM tb_lkp_status_validacoes_postagens
     WHERE nome_status = v_status_destino_nome;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Status de validacao % nao cadastrado', v_status_destino_nome;
    END IF;

    v_nova_pontuacao_ativa := v_pontuacao_ativa;
    IF v_status_destino_nome = 'aprovada' THEN
        v_nova_pontuacao_ativa := TRUE;
    ELSIF v_status_destino_nome = 'reprovada' THEN
        v_nova_pontuacao_ativa := FALSE;
    END IF;

    v_mudou_estado := (v_nova_pontuacao_ativa IS DISTINCT FROM v_pontuacao_ativa);

    UPDATE tb_postagens
       SET status_validacao_id = v_status_destino_id,
           resolvido_em = now(),
           pontuacao_ativa = v_nova_pontuacao_ativa,
           pontuacao_reconciliacao_pendente =
               pontuacao_reconciliacao_pendente OR v_mudou_estado
     WHERE id_postagem = p_postagem_id;
END;
$$;


--
-- TOC entry 406 (class 1255 OID 25568)
-- Name: sp_processar_voto_postagem(integer, integer, character varying, integer, character varying); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_processar_voto_postagem(IN p_postagem_id integer, IN p_usuario_id integer, IN p_tipo_voto character varying, IN p_motivo_denuncia_id integer DEFAULT NULL::integer, IN p_comentario character varying DEFAULT NULL::character varying)
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_condominio_id       INTEGER;
    v_resolvido_em        TIMESTAMPTZ;
    v_data_limite         TIMESTAMPTZ;
    v_peso                INTEGER;
    v_tipo_voto_id        INTEGER;
    v_saldo_atual         INTEGER;
    v_novo_saldo          INTEGER;
    v_pontuacao_ativa     BOOLEAN;
    v_nova_pontuacao_ativa BOOLEAN;
    v_mudou_estado        BOOLEAN := FALSE;
BEGIN
    SELECT id_tipo_voto
      INTO v_tipo_voto_id
      FROM tb_lkp_tipos_votos_postagens
     WHERE nome_tipo = p_tipo_voto;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Tipo de voto invalido: %', p_tipo_voto;
    END IF;

    IF p_tipo_voto = 'denunciar' AND p_motivo_denuncia_id IS NULL THEN
        RAISE EXCEPTION 'motivo_denuncia_id e obrigatorio para votos de denuncia';
    END IF;

    IF p_tipo_voto = 'aprovar' AND p_motivo_denuncia_id IS NOT NULL THEN
        RAISE EXCEPTION 'motivo_denuncia_id deve ser nulo para votos de aprovacao';
    END IF;

    -- Trava a postagem para serializar votos concorrentes.
    SELECT condominio_id,
           saldo_confianca,
           resolvido_em,
           data_limite_analise,
           pontuacao_ativa
      INTO v_condominio_id,
           v_saldo_atual,
           v_resolvido_em,
           v_data_limite,
           v_pontuacao_ativa
      FROM tb_postagens
     WHERE id_postagem = p_postagem_id
     FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Postagem % nao encontrada', p_postagem_id;
    END IF;

    IF v_resolvido_em IS NOT NULL THEN
        RAISE EXCEPTION 'Postagem % ja possui decisao de encerramento e nao aceita novos votos', p_postagem_id;
    END IF;

    IF now() >= v_data_limite THEN
        RAISE EXCEPTION 'Janela comunitaria da postagem % encerrou em %', p_postagem_id, v_data_limite;
    END IF;

    SELECT nc.peso_voto
      INTO v_peso
      FROM tb_rel_usuarios_condominios uco
      JOIN tb_lkp_niveis_confianca nc
        ON nc.id_nivel_confianca = uco.nivel_confianca_id
     WHERE uco.usuario_id    = p_usuario_id
       AND uco.condominio_id = v_condominio_id
       AND uco.aprovado      = TRUE
       AND uco.data_saida IS NULL;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Usuario % nao possui vinculo aprovado e ativo com o condominio da postagem', p_usuario_id;
    END IF;

    INSERT INTO tb_rel_votos_postagens (
        postagem_id,
        usuario_id,
        tipo_voto_id,
        motivo_denuncia_id,
        peso_aplicado,
        comentario
    ) VALUES (
        p_postagem_id,
        p_usuario_id,
        v_tipo_voto_id,
        p_motivo_denuncia_id,
        v_peso,
        p_comentario
    );

    v_novo_saldo := v_saldo_atual
        + CASE WHEN p_tipo_voto = 'aprovar' THEN v_peso ELSE -v_peso END;

    -- Histerese da pontuacao:
    --   ativa  -> inativa ao atingir <= -5;
    --   inativa -> ativa somente quando recuperar >= 0.
    v_nova_pontuacao_ativa := v_pontuacao_ativa;

    IF v_pontuacao_ativa = TRUE AND v_novo_saldo <= -5 THEN
        v_nova_pontuacao_ativa := FALSE;
        v_mudou_estado := TRUE;
    ELSIF v_pontuacao_ativa = FALSE AND v_novo_saldo >= 0 THEN
        v_nova_pontuacao_ativa := TRUE;
        v_mudou_estado := TRUE;
    END IF;

    UPDATE tb_postagens
       SET saldo_confianca = v_novo_saldo,
           pontuacao_ativa = v_nova_pontuacao_ativa,
           pontuacao_reconciliacao_pendente =
               pontuacao_reconciliacao_pendente OR v_mudou_estado
     WHERE id_postagem = p_postagem_id;

    -- Nao altera status_validacao_id nem resolvido_em aqui.
    -- A API deve reconciliar o ledger quando a flag pendente for TRUE.
END;
$$;


SET default_table_access_method = heap;

--
-- TOC entry 347 (class 1259 OID 73985)
-- Name: dim_categoria_residuo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dim_categoria_residuo (
    sk_categoria integer NOT NULL,
    id_categoria integer NOT NULL,
    nome_categoria character varying(100),
    permite_reciclagem boolean,
    pontos_base integer,
    limite_pontos_ciclo integer,
    carregado_em timestamp with time zone DEFAULT now() NOT NULL
);


--
-- TOC entry 346 (class 1259 OID 73984)
-- Name: dim_categoria_residuo_sk_categoria_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dim_categoria_residuo_sk_categoria_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4293 (class 0 OID 0)
-- Dependencies: 346
-- Name: dim_categoria_residuo_sk_categoria_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dim_categoria_residuo_sk_categoria_seq OWNED BY public.dim_categoria_residuo.sk_categoria;


--
-- TOC entry 343 (class 1259 OID 73959)
-- Name: dim_condominio; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dim_condominio (
    sk_condominio integer NOT NULL,
    id_condominio integer NOT NULL,
    nome_condominio character varying(255),
    tipo_condominio character varying(50),
    cidade character varying(100),
    estado character(2),
    ativo boolean,
    carregado_em timestamp with time zone DEFAULT now() NOT NULL
);


--
-- TOC entry 342 (class 1259 OID 73958)
-- Name: dim_condominio_sk_condominio_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dim_condominio_sk_condominio_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4294 (class 0 OID 0)
-- Dependencies: 342
-- Name: dim_condominio_sk_condominio_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dim_condominio_sk_condominio_seq OWNED BY public.dim_condominio.sk_condominio;


--
-- TOC entry 345 (class 1259 OID 73972)
-- Name: dim_cooperativa; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dim_cooperativa (
    sk_cooperativa integer NOT NULL,
    id_cooperativa integer NOT NULL,
    nome_cooperativa character varying(255),
    cidade character varying(100),
    estado character(2),
    carregado_em timestamp with time zone DEFAULT now() NOT NULL
);


--
-- TOC entry 344 (class 1259 OID 73971)
-- Name: dim_cooperativa_sk_cooperativa_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dim_cooperativa_sk_cooperativa_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4295 (class 0 OID 0)
-- Dependencies: 344
-- Name: dim_cooperativa_sk_cooperativa_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dim_cooperativa_sk_cooperativa_seq OWNED BY public.dim_cooperativa.sk_cooperativa;


--
-- TOC entry 351 (class 1259 OID 74012)
-- Name: dim_status_agendamento; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dim_status_agendamento (
    sk_status_agendamento integer NOT NULL,
    id_status integer NOT NULL,
    nome_status character varying(50)
);


--
-- TOC entry 350 (class 1259 OID 74011)
-- Name: dim_status_agendamento_sk_status_agendamento_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dim_status_agendamento_sk_status_agendamento_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4296 (class 0 OID 0)
-- Dependencies: 350
-- Name: dim_status_agendamento_sk_status_agendamento_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dim_status_agendamento_sk_status_agendamento_seq OWNED BY public.dim_status_agendamento.sk_status_agendamento;


--
-- TOC entry 349 (class 1259 OID 73998)
-- Name: dim_status_postagem; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dim_status_postagem (
    sk_status_postagem integer NOT NULL,
    id_status_validacao integer NOT NULL,
    codigo character varying(30) NOT NULL,
    descricao character varying(255)
);


--
-- TOC entry 348 (class 1259 OID 73997)
-- Name: dim_status_postagem_sk_status_postagem_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dim_status_postagem_sk_status_postagem_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4297 (class 0 OID 0)
-- Dependencies: 348
-- Name: dim_status_postagem_sk_status_postagem_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dim_status_postagem_sk_status_postagem_seq OWNED BY public.dim_status_postagem.sk_status_postagem;


--
-- TOC entry 339 (class 1259 OID 73928)
-- Name: dim_tempo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dim_tempo (
    sk_tempo integer NOT NULL,
    data_completa date NOT NULL,
    ano integer NOT NULL,
    trimestre integer NOT NULL,
    mes integer NOT NULL,
    nome_mes character varying(20) NOT NULL,
    dia integer NOT NULL,
    dia_semana_numero integer NOT NULL,
    nome_dia_semana character varying(20) NOT NULL,
    is_fim_de_semana boolean NOT NULL
);


--
-- TOC entry 341 (class 1259 OID 73946)
-- Name: dim_usuario; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dim_usuario (
    sk_usuario integer NOT NULL,
    id_usuario integer NOT NULL,
    nome_usuario character varying(100),
    email_usuario character varying(255),
    tipo_usuario character varying(50),
    ativo boolean,
    carregado_em timestamp with time zone DEFAULT now() NOT NULL
);


--
-- TOC entry 340 (class 1259 OID 73945)
-- Name: dim_usuario_sk_usuario_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dim_usuario_sk_usuario_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4298 (class 0 OID 0)
-- Dependencies: 340
-- Name: dim_usuario_sk_usuario_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dim_usuario_sk_usuario_seq OWNED BY public.dim_usuario.sk_usuario;


--
-- TOC entry 355 (class 1259 OID 74074)
-- Name: fato_coletas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fato_coletas (
    sk_fato_coleta bigint NOT NULL,
    id_visita_coleta integer NOT NULL,
    sk_tempo integer NOT NULL,
    sk_condominio integer NOT NULL,
    sk_cooperativa integer NOT NULL,
    sk_status_agendamento integer NOT NULL,
    foi_realizada boolean DEFAULT false NOT NULL,
    houve_confirmacao boolean DEFAULT false NOT NULL,
    qtd_visitas integer DEFAULT 1 NOT NULL
);


--
-- TOC entry 354 (class 1259 OID 74073)
-- Name: fato_coletas_sk_fato_coleta_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.fato_coletas_sk_fato_coleta_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4299 (class 0 OID 0)
-- Dependencies: 354
-- Name: fato_coletas_sk_fato_coleta_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.fato_coletas_sk_fato_coleta_seq OWNED BY public.fato_coletas.sk_fato_coleta;


--
-- TOC entry 353 (class 1259 OID 74023)
-- Name: fato_postagens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fato_postagens (
    sk_fato_postagem bigint NOT NULL,
    id_postagem integer NOT NULL,
    sk_tempo integer NOT NULL,
    sk_usuario integer NOT NULL,
    sk_condominio integer NOT NULL,
    sk_categoria integer NOT NULL,
    sk_status_postagem integer NOT NULL,
    qtd_postagens integer DEFAULT 1 NOT NULL,
    foi_aprovada boolean DEFAULT false NOT NULL,
    pontos_base_categoria integer DEFAULT 0 NOT NULL
);


--
-- TOC entry 352 (class 1259 OID 74022)
-- Name: fato_postagens_sk_fato_postagem_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.fato_postagens_sk_fato_postagem_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4300 (class 0 OID 0)
-- Dependencies: 352
-- Name: fato_postagens_sk_fato_postagem_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.fato_postagens_sk_fato_postagem_seq OWNED BY public.fato_postagens.sk_fato_postagem;


--
-- TOC entry 275 (class 1259 OID 25062)
-- Name: tb_agendamentos_coletas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_agendamentos_coletas (
    id_agendamento_coleta integer NOT NULL,
    condominio_id integer NOT NULL,
    cooperativa_id integer NOT NULL,
    status_agendamento_id integer NOT NULL,
    data_inicio timestamp with time zone NOT NULL,
    data_fim timestamp with time zone,
    possui_recorrencia boolean DEFAULT false NOT NULL,
    solicitado_por_usuario_id integer NOT NULL,
    origem_solicitacao character varying(20) NOT NULL,
    respondido_por_usuario_id integer,
    respondido_em timestamp with time zone,
    CONSTRAINT ck_agendamentos_coletas_datas CHECK (((data_fim IS NULL) OR (data_fim >= data_inicio))),
    CONSTRAINT ck_agendamentos_coletas_origem_solicitacao CHECK (((origem_solicitacao)::text = ANY ((ARRAY['CONDOMINIO'::character varying, 'COOPERATIVA'::character varying])::text[]))),
    CONSTRAINT ck_agendamentos_coletas_resposta CHECK (((respondido_por_usuario_id IS NULL) = (respondido_em IS NULL)))
);


--
-- TOC entry 274 (class 1259 OID 25061)
-- Name: tb_agendamentos_coletas_id_agendamento_coleta_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_agendamentos_coletas ALTER COLUMN id_agendamento_coleta ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_agendamentos_coletas_id_agendamento_coleta_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 291 (class 1259 OID 25254)
-- Name: tb_alternativas_quiz; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_alternativas_quiz (
    id_alternativa integer NOT NULL,
    pergunta_id integer NOT NULL,
    texto_alternativa character varying(500) NOT NULL,
    correta boolean DEFAULT false NOT NULL
);


--
-- TOC entry 290 (class 1259 OID 25253)
-- Name: tb_alternativas_quiz_id_alternativa_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_alternativas_quiz ALTER COLUMN id_alternativa ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_alternativas_quiz_id_alternativa_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 327 (class 1259 OID 25669)
-- Name: tb_atividades_diarias_usuarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_atividades_diarias_usuarios (
    id_atividade_diaria bigint NOT NULL,
    usuario_id integer NOT NULL,
    data_atividade date NOT NULL,
    plataforma character varying(10) NOT NULL,
    qtd_sessoes smallint DEFAULT 1 NOT NULL,
    minutos_ativos smallint DEFAULT 0 NOT NULL,
    qtd_acoes integer DEFAULT 0 NOT NULL,
    primeira_atividade_em timestamp with time zone NOT NULL,
    ultima_atividade_em timestamp with time zone NOT NULL,
    CONSTRAINT ck_atividades_diarias_intervalo CHECK ((ultima_atividade_em >= primeira_atividade_em)),
    CONSTRAINT ck_atividades_diarias_minutos CHECK ((minutos_ativos >= 0)),
    CONSTRAINT ck_atividades_diarias_plataforma CHECK (((plataforma)::text = ANY ((ARRAY['android'::character varying, 'ios'::character varying, 'web'::character varying])::text[]))),
    CONSTRAINT ck_atividades_diarias_qtd_acoes CHECK ((qtd_acoes >= 0)),
    CONSTRAINT ck_atividades_diarias_qtd_sessoes CHECK ((qtd_sessoes > 0))
);


--
-- TOC entry 326 (class 1259 OID 25668)
-- Name: tb_atividades_diarias_usuarios_id_atividade_diaria_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_atividades_diarias_usuarios ALTER COLUMN id_atividade_diaria ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_atividades_diarias_usuarios_id_atividade_diaria_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 285 (class 1259 OID 25185)
-- Name: tb_aulas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_aulas (
    id_aula integer NOT NULL,
    curso_id integer NOT NULL,
    titulo_aula character varying(255) NOT NULL,
    conteudo_aula text,
    ordem integer NOT NULL
);


--
-- TOC entry 284 (class 1259 OID 25184)
-- Name: tb_aulas_id_aula_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_aulas ALTER COLUMN id_aula ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_aulas_id_aula_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 299 (class 1259 OID 25359)
-- Name: tb_autenticacoes_api; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_autenticacoes_api (
    id_autenticao_api integer NOT NULL,
    usuario_id integer NOT NULL,
    token text NOT NULL,
    tipo_token character varying(20) DEFAULT 'Bearer'::character varying NOT NULL,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    expira_em integer NOT NULL,
    CONSTRAINT ck_autenticao_api_expira_em CHECK ((expira_em > 0)),
    CONSTRAINT ck_autenticao_api_token_type CHECK (((tipo_token)::text = 'Bearer'::text))
);


--
-- TOC entry 298 (class 1259 OID 25358)
-- Name: tb_autenticacoes_api_id_autenticao_api_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_autenticacoes_api ALTER COLUMN id_autenticao_api ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_autenticacoes_api_id_autenticao_api_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 279 (class 1259 OID 25125)
-- Name: tb_avaliacoes_visitas_coletas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_avaliacoes_visitas_coletas (
    id_avaliacao integer NOT NULL,
    visita_coleta_id integer NOT NULL,
    usuario_avaliador_id integer NOT NULL,
    nota smallint NOT NULL,
    comentario character varying(500),
    avaliado_em timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_avaliacoes_visitas_coletas_nota CHECK (((nota >= 1) AND (nota <= 5)))
);


--
-- TOC entry 278 (class 1259 OID 25124)
-- Name: tb_avaliacoes_visitas_coletas_id_avaliacao_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_avaliacoes_visitas_coletas ALTER COLUMN id_avaliacao ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_avaliacoes_visitas_coletas_id_avaliacao_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 255 (class 1259 OID 24762)
-- Name: tb_condominios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_condominios (
    id_condominio integer NOT NULL,
    nome_condominio character varying(255) NOT NULL,
    cnpj character varying(18),
    codigo_acesso character varying(20),
    ativo boolean DEFAULT true NOT NULL,
    tipo_condominio_id integer NOT NULL,
    sindico_id integer,
    endereco_id integer,
    url_foto character varying(500)
);


--
-- TOC entry 254 (class 1259 OID 24761)
-- Name: tb_condominios_id_condominio_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_condominios ALTER COLUMN id_condominio ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_condominios_id_condominio_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 333 (class 1259 OID 25744)
-- Name: tb_conversas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_conversas (
    id_conversa integer NOT NULL,
    condominio_id integer NOT NULL,
    cooperativa_id integer NOT NULL,
    criada_em timestamp with time zone DEFAULT now() NOT NULL,
    ultima_mensagem_em timestamp with time zone,
    ativa boolean DEFAULT true NOT NULL
);


--
-- TOC entry 332 (class 1259 OID 25743)
-- Name: tb_conversas_id_conversa_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_conversas ALTER COLUMN id_conversa ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_conversas_id_conversa_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 261 (class 1259 OID 24836)
-- Name: tb_cooperativas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_cooperativas (
    id_cooperativa integer NOT NULL,
    cnpj_cooperativa character varying(18) NOT NULL,
    nome_cooperativa character varying(255) NOT NULL,
    email_cooperativa character varying(255),
    telefone_cooperativa character varying(20),
    data_cadastro timestamp with time zone DEFAULT now() NOT NULL,
    usuario_id integer,
    endereco_id integer
);


--
-- TOC entry 260 (class 1259 OID 24835)
-- Name: tb_cooperativas_id_cooperativa_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_cooperativas ALTER COLUMN id_cooperativa ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_cooperativas_id_cooperativa_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 283 (class 1259 OID 25173)
-- Name: tb_cursos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_cursos (
    id_curso integer NOT NULL,
    titulo_curso character varying(255) NOT NULL,
    descricao_curso text,
    esta_ativo boolean DEFAULT true NOT NULL
);


--
-- TOC entry 282 (class 1259 OID 25172)
-- Name: tb_cursos_id_curso_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_cursos ALTER COLUMN id_curso ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_cursos_id_curso_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 245 (class 1259 OID 24676)
-- Name: tb_enderecos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_enderecos (
    id_endereco integer NOT NULL,
    cep character varying(10),
    estado character(2),
    cidade character varying(100),
    bairro character varying(100),
    logradouro character varying(150),
    numero character varying(20),
    complemento character varying(100)
);


--
-- TOC entry 244 (class 1259 OID 24675)
-- Name: tb_enderecos_id_endereco_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_enderecos ALTER COLUMN id_endereco ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_enderecos_id_endereco_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 338 (class 1259 OID 57345)
-- Name: tb_recuperacoes_senha; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_recuperacoes_senha (
    id_esqueci_senha integer CONSTRAINT tb_esqueci_senha_id_esqueci_senha_not_null NOT NULL,
    usuario_id integer CONSTRAINT tb_esqueci_senha_usuario_id_not_null NOT NULL,
    token_hash character varying(255) CONSTRAINT tb_esqueci_senha_token_hash_not_null NOT NULL,
    expira_em timestamp with time zone CONSTRAINT tb_esqueci_senha_expira_em_not_null NOT NULL,
    utilizado_em timestamp with time zone
);


--
-- TOC entry 337 (class 1259 OID 57344)
-- Name: tb_esqueci_senha_id_esqueci_senha_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_recuperacoes_senha ALTER COLUMN id_esqueci_senha ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_esqueci_senha_id_esqueci_senha_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 231 (class 1259 OID 24612)
-- Name: tb_lkp_categorias_residuos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_lkp_categorias_residuos (
    id_categoria integer NOT NULL,
    nome_categoria character varying(100) NOT NULL,
    descricao_material character varying(255),
    permite_reciclagem boolean DEFAULT true NOT NULL,
    cor_identificacao character varying(10),
    pontos_base integer DEFAULT 10 NOT NULL,
    limite_pontos_diario integer,
    CONSTRAINT ck_categorias_residuos_limite_pontos_diario CHECK (((limite_pontos_diario IS NULL) OR (limite_pontos_diario >= 0))),
    CONSTRAINT ck_categorias_residuos_pontos_base CHECK ((pontos_base > 0))
);


--
-- TOC entry 230 (class 1259 OID 24611)
-- Name: tb_lkp_categorias_residuos_id_categoria_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_lkp_categorias_residuos ALTER COLUMN id_categoria ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_lkp_categorias_residuos_id_categoria_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 229 (class 1259 OID 24604)
-- Name: tb_lkp_dias_semanas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_lkp_dias_semanas (
    id_dia_semana integer NOT NULL,
    nome_dia character varying(20) NOT NULL
);


--
-- TOC entry 228 (class 1259 OID 24603)
-- Name: tb_lkp_dias_semanas_id_dia_semana_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_lkp_dias_semanas ALTER COLUMN id_dia_semana ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_lkp_dias_semanas_id_dia_semana_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 239 (class 1259 OID 24652)
-- Name: tb_lkp_motivos_denuncia; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_lkp_motivos_denuncia (
    id_motivo_denuncia integer NOT NULL,
    descricao character varying(255) NOT NULL,
    ativo boolean DEFAULT true NOT NULL
);


--
-- TOC entry 238 (class 1259 OID 24651)
-- Name: tb_lkp_motivos_denuncia_id_motivo_denuncia_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_lkp_motivos_denuncia ALTER COLUMN id_motivo_denuncia ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_lkp_motivos_denuncia_id_motivo_denuncia_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 233 (class 1259 OID 24626)
-- Name: tb_lkp_niveis_confianca; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_lkp_niveis_confianca (
    id_nivel_confianca integer NOT NULL,
    nome_nivel character varying(50) NOT NULL,
    peso_voto integer NOT NULL,
    descricao character varying(255),
    CONSTRAINT ck_niveis_confianca_peso_voto CHECK ((peso_voto > 0))
);


--
-- TOC entry 232 (class 1259 OID 24625)
-- Name: tb_lkp_niveis_confianca_id_nivel_confianca_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_lkp_niveis_confianca ALTER COLUMN id_nivel_confianca ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_lkp_niveis_confianca_id_nivel_confianca_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 227 (class 1259 OID 24594)
-- Name: tb_lkp_status_agendamentos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_lkp_status_agendamentos (
    id_status integer NOT NULL,
    nome_status character varying(50) NOT NULL
);


--
-- TOC entry 226 (class 1259 OID 24593)
-- Name: tb_lkp_status_agendamentos_id_status_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_lkp_status_agendamentos ALTER COLUMN id_status ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_lkp_status_agendamentos_id_status_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 235 (class 1259 OID 24636)
-- Name: tb_lkp_status_validacoes_postagens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_lkp_status_validacoes_postagens (
    id_status_validacao integer NOT NULL,
    nome_status character varying(30) NOT NULL,
    descricao character varying(255)
);


--
-- TOC entry 234 (class 1259 OID 24635)
-- Name: tb_lkp_status_validacoes_postagens_id_status_validacao_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_lkp_status_validacoes_postagens ALTER COLUMN id_status_validacao ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_lkp_status_validacoes_postagens_id_status_validacao_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 225 (class 1259 OID 24586)
-- Name: tb_lkp_tipos_condominios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_lkp_tipos_condominios (
    id_tipo_condominio integer NOT NULL,
    nome_tipo character varying(50) NOT NULL,
    descricao character varying(255)
);


--
-- TOC entry 224 (class 1259 OID 24585)
-- Name: tb_lkp_tipos_condominios_id_tipo_condominio_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_lkp_tipos_condominios ALTER COLUMN id_tipo_condominio ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_lkp_tipos_condominios_id_tipo_condominio_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 241 (class 1259 OID 24662)
-- Name: tb_lkp_tipos_eventos_auditados; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_lkp_tipos_eventos_auditados (
    id_tipo_evento_auditado integer NOT NULL,
    nome_evento character varying(50),
    descricao character varying(255)
);


--
-- TOC entry 240 (class 1259 OID 24661)
-- Name: tb_lkp_tipos_eventos_auditados_id_tipo_evento_auditado_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_lkp_tipos_eventos_auditados ALTER COLUMN id_tipo_evento_auditado ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_lkp_tipos_eventos_auditados_id_tipo_evento_auditado_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 243 (class 1259 OID 24669)
-- Name: tb_lkp_tipos_operacoes_auditoria; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_lkp_tipos_operacoes_auditoria (
    id_tipo_operacao integer NOT NULL,
    nome_operacao character varying(50)
);


--
-- TOC entry 242 (class 1259 OID 24668)
-- Name: tb_lkp_tipos_operacoes_auditoria_id_tipo_operacao_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_lkp_tipos_operacoes_auditoria ALTER COLUMN id_tipo_operacao ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_lkp_tipos_operacoes_auditoria_id_tipo_operacao_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 223 (class 1259 OID 24578)
-- Name: tb_lkp_tipos_usuarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_lkp_tipos_usuarios (
    id_tipo_usuario integer NOT NULL,
    nome_tipo character varying(50) NOT NULL,
    descricao character varying(255)
);


--
-- TOC entry 222 (class 1259 OID 24577)
-- Name: tb_lkp_tipos_usuarios_id_tipo_usuario_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_lkp_tipos_usuarios ALTER COLUMN id_tipo_usuario ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_lkp_tipos_usuarios_id_tipo_usuario_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 237 (class 1259 OID 24644)
-- Name: tb_lkp_tipos_votos_postagens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_lkp_tipos_votos_postagens (
    id_tipo_voto integer NOT NULL,
    nome_tipo character varying(30) NOT NULL
);


--
-- TOC entry 236 (class 1259 OID 24643)
-- Name: tb_lkp_tipos_votos_postagens_id_tipo_voto_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_lkp_tipos_votos_postagens ALTER COLUMN id_tipo_voto ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_lkp_tipos_votos_postagens_id_tipo_voto_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 303 (class 1259 OID 25461)
-- Name: tb_log_auditoria; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_log_auditoria (
    id_auditoria bigint NOT NULL,
    tipo_evento_auditado_id integer NOT NULL,
    tipo_operacao_id integer NOT NULL,
    executado_por character varying(100) DEFAULT CURRENT_USER NOT NULL,
    executado_em timestamp with time zone DEFAULT now() NOT NULL
);


--
-- TOC entry 307 (class 1259 OID 25515)
-- Name: tb_log_auditoria_agendamentos_coletas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_log_auditoria_agendamentos_coletas (
    id_auditoria_agendamento integer CONSTRAINT tb_log_auditoria_agendamentos_id_auditoria_agendamento_not_null NOT NULL,
    auditoria_id bigint NOT NULL,
    agendamento_coleta_id integer CONSTRAINT tb_log_auditoria_agendamentos_co_agendamento_coleta_id_not_null NOT NULL,
    status_anterior_id integer,
    status_novo_id integer,
    data_inicio_anterior timestamp with time zone,
    data_inicio_novo timestamp with time zone,
    data_fim_anterior timestamp with time zone,
    data_fim_novo timestamp with time zone
);


--
-- TOC entry 306 (class 1259 OID 25514)
-- Name: tb_log_auditoria_agendamentos_cole_id_auditoria_agendamento_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_log_auditoria_agendamentos_coletas ALTER COLUMN id_auditoria_agendamento ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_log_auditoria_agendamentos_cole_id_auditoria_agendamento_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 302 (class 1259 OID 25460)
-- Name: tb_log_auditoria_id_auditoria_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_log_auditoria ALTER COLUMN id_auditoria ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_log_auditoria_id_auditoria_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 305 (class 1259 OID 25484)
-- Name: tb_log_auditoria_postagens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_log_auditoria_postagens (
    id_auditoria_postagem bigint NOT NULL,
    auditoria_id bigint NOT NULL,
    postagem_id integer NOT NULL,
    status_validacao_anterior_id integer,
    status_validacao_novo_id integer,
    saldo_confianca_anterior integer,
    saldo_confianca_novo integer
);


--
-- TOC entry 304 (class 1259 OID 25483)
-- Name: tb_log_auditoria_postagens_id_auditoria_postagem_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_log_auditoria_postagens ALTER COLUMN id_auditoria_postagem ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_log_auditoria_postagens_id_auditoria_postagem_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 309 (class 1259 OID 25546)
-- Name: tb_log_auditoria_usuarios_condominios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_log_auditoria_usuarios_condominios (
    id_auditoria_usuario_condominio integer CONSTRAINT tb_log_auditoria_usuarios_c_id_auditoria_usuario_condo_not_null NOT NULL,
    auditoria_id bigint NOT NULL,
    usuario_condominio_id integer CONSTRAINT tb_log_auditoria_usuarios_condom_usuario_condominio_id_not_null NOT NULL,
    aprovado_anterior boolean,
    aprovado_novo boolean,
    data_saida_anterior timestamp with time zone,
    data_saida_novo timestamp with time zone
);


--
-- TOC entry 308 (class 1259 OID 25545)
-- Name: tb_log_auditoria_usuarios_con_id_auditoria_usuario_condomin_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_log_auditoria_usuarios_condominios ALTER COLUMN id_auditoria_usuario_condominio ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_log_auditoria_usuarios_con_id_auditoria_usuario_condomin_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 335 (class 1259 OID 25769)
-- Name: tb_mensagens_conversas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_mensagens_conversas (
    id_mensagem bigint NOT NULL,
    conversa_id integer NOT NULL,
    remetente_usuario_id integer NOT NULL,
    remetente_tipo character varying(20) NOT NULL,
    conteudo text NOT NULL,
    enviada_em timestamp with time zone DEFAULT now() NOT NULL,
    lida_em timestamp with time zone,
    CONSTRAINT ck_mensagens_conversas_conteudo CHECK (((length(btrim(conteudo)) >= 1) AND (length(btrim(conteudo)) <= 2000))),
    CONSTRAINT ck_mensagens_conversas_lida_em CHECK (((lida_em IS NULL) OR (lida_em >= enviada_em))),
    CONSTRAINT ck_mensagens_conversas_remetente_tipo CHECK (((remetente_tipo)::text = ANY ((ARRAY['COOPERATIVA'::character varying, 'CONDOMINIO'::character varying])::text[])))
);


--
-- TOC entry 334 (class 1259 OID 25768)
-- Name: tb_mensagens_conversas_id_mensagem_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_mensagens_conversas ALTER COLUMN id_mensagem ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_mensagens_conversas_id_mensagem_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 328 (class 1259 OID 25699)
-- Name: tb_metricas_dau; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_metricas_dau (
    data_referencia date NOT NULL,
    dau integer NOT NULL,
    wau integer NOT NULL,
    mau integer NOT NULL,
    novos_usuarios integer NOT NULL,
    usuarios_retornantes integer NOT NULL,
    sessoes_total integer NOT NULL,
    minutos_medios numeric(6,2),
    stickiness numeric(5,2),
    calculado_em timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_metricas_dau_contagens CHECK (((dau >= 0) AND (wau >= dau) AND (mau >= wau))),
    CONSTRAINT ck_metricas_dau_novos CHECK (((novos_usuarios + usuarios_retornantes) = dau)),
    CONSTRAINT ck_metricas_dau_stickiness CHECK (((stickiness IS NULL) OR ((stickiness >= (0)::numeric) AND (stickiness <= (100)::numeric))))
);


--
-- TOC entry 331 (class 1259 OID 25727)
-- Name: tb_migracao_ids_map; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_migracao_ids_map (
    entidade text NOT NULL,
    legacy_id bigint NOT NULL,
    novo_id bigint NOT NULL,
    migrado_em timestamp with time zone DEFAULT now() NOT NULL
);


--
-- TOC entry 259 (class 1259 OID 24810)
-- Name: tb_moradores; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_moradores (
    id_morador integer NOT NULL,
    condominio_id integer NOT NULL,
    usuario_id integer NOT NULL,
    torre_id integer
);


--
-- TOC entry 258 (class 1259 OID 24809)
-- Name: tb_moradores_id_morador_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_moradores ALTER COLUMN id_morador ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_moradores_id_morador_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 301 (class 1259 OID 25384)
-- Name: tb_movimentacoes_pontos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_movimentacoes_pontos (
    id_movimentacao bigint NOT NULL,
    usuario_id integer NOT NULL,
    condominio_id integer,
    torre_id integer,
    categoria_id integer,
    origem_tipo character varying(20) NOT NULL,
    postagem_id integer,
    tentativa_quiz_id integer,
    tipo_movimentacao character varying(20) NOT NULL,
    pontos integer NOT NULL,
    movimentacao_referencia_id bigint,
    idempotency_key character varying(150) NOT NULL,
    ocorrido_em timestamp with time zone DEFAULT now() NOT NULL,
    redis_sincronizado boolean DEFAULT false NOT NULL,
    redis_sincronizado_em timestamp with time zone,
    tentativas_sync_redis integer DEFAULT 0 NOT NULL,
    ultimo_erro_redis text,
    CONSTRAINT ck_mov_pontos_origem_consistente CHECK (((((origem_tipo)::text = 'POSTAGEM'::text) AND (postagem_id IS NOT NULL) AND (tentativa_quiz_id IS NULL) AND (categoria_id IS NOT NULL)) OR (((origem_tipo)::text = 'QUIZ'::text) AND (tentativa_quiz_id IS NOT NULL) AND (postagem_id IS NULL) AND (categoria_id IS NULL)))),
    CONSTRAINT ck_mov_pontos_origem_tipo CHECK (((origem_tipo)::text = ANY ((ARRAY['POSTAGEM'::character varying, 'QUIZ'::character varying])::text[]))),
    CONSTRAINT ck_mov_pontos_pontos_positivo CHECK ((pontos > 0)),
    CONSTRAINT ck_mov_pontos_referencia_consistente CHECK (((((tipo_movimentacao)::text = 'CREDITO'::text) AND (movimentacao_referencia_id IS NULL)) OR (((tipo_movimentacao)::text = ANY ((ARRAY['ESTORNO'::character varying, 'RESTAURACAO'::character varying])::text[])) AND (movimentacao_referencia_id IS NOT NULL)))),
    CONSTRAINT ck_mov_pontos_sync_timestamp CHECK (((redis_sincronizado = false) OR (redis_sincronizado_em IS NOT NULL))),
    CONSTRAINT ck_mov_pontos_tentativas_sync CHECK ((tentativas_sync_redis >= 0)),
    CONSTRAINT ck_mov_pontos_tipo_movimentacao CHECK (((tipo_movimentacao)::text = ANY ((ARRAY['CREDITO'::character varying, 'ESTORNO'::character varying, 'RESTAURACAO'::character varying])::text[])))
);


--
-- TOC entry 300 (class 1259 OID 25383)
-- Name: tb_movimentacoes_pontos_id_movimentacao_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_movimentacoes_pontos ALTER COLUMN id_movimentacao ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_movimentacoes_pontos_id_movimentacao_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 253 (class 1259 OID 24744)
-- Name: tb_notificacoes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_notificacoes (
    id_notificacao integer NOT NULL,
    usuario_id integer NOT NULL,
    titulo_mensagem character varying(255) NOT NULL,
    corpo_mensagem text,
    tipo_notificacao character varying(50),
    data_envio timestamp with time zone DEFAULT now() NOT NULL
);


--
-- TOC entry 252 (class 1259 OID 24743)
-- Name: tb_notificacoes_id_notificacao_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_notificacoes ALTER COLUMN id_notificacao ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_notificacoes_id_notificacao_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 289 (class 1259 OID 25235)
-- Name: tb_perguntas_quiz; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_perguntas_quiz (
    id_pergunta integer NOT NULL,
    quiz_id integer NOT NULL,
    enunciado text NOT NULL,
    ordem integer NOT NULL,
    explicacao text
);


--
-- TOC entry 288 (class 1259 OID 25234)
-- Name: tb_perguntas_quiz_id_pergunta_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_perguntas_quiz ALTER COLUMN id_pergunta ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_perguntas_quiz_id_pergunta_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 263 (class 1259 OID 24863)
-- Name: tb_pontos_coletas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_pontos_coletas (
    id_ponto_coleta integer NOT NULL,
    nome_ponto character varying(255) NOT NULL,
    endereco_id integer,
    cooperativa_id integer,
    horario_abertura time without time zone,
    horario_fechamento time without time zone,
    ativo boolean DEFAULT true NOT NULL
);


--
-- TOC entry 262 (class 1259 OID 24862)
-- Name: tb_pontos_coletas_id_ponto_coleta_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_pontos_coletas ALTER COLUMN id_ponto_coleta ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_pontos_coletas_id_ponto_coleta_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 271 (class 1259 OID 24970)
-- Name: tb_postagens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_postagens (
    id_postagem integer NOT NULL,
    usuario_id integer NOT NULL,
    condominio_id integer NOT NULL,
    torre_id integer,
    categoria_id integer NOT NULL,
    url_foto character varying(500) NOT NULL,
    hash_foto character varying(128) NOT NULL,
    capturada_em timestamp with time zone NOT NULL,
    data_postagem timestamp with time zone DEFAULT now() NOT NULL,
    status_validacao_id integer DEFAULT 1 NOT NULL,
    saldo_confianca integer DEFAULT 0 NOT NULL,
    pontuacao_ativa boolean DEFAULT true NOT NULL,
    pontuacao_reconciliacao_pendente boolean DEFAULT false NOT NULL,
    triagem_automatica_aprovada boolean,
    triagem_automatica_confianca numeric(5,2),
    data_limite_analise timestamp with time zone DEFAULT (now() + '24:00:00'::interval) NOT NULL,
    resolvido_em timestamp with time zone,
    CONSTRAINT ck_postagens_capturada_em CHECK ((capturada_em <= data_postagem)),
    CONSTRAINT ck_postagens_triagem_confianca CHECK (((triagem_automatica_confianca IS NULL) OR ((triagem_automatica_confianca >= (0)::numeric) AND (triagem_automatica_confianca <= (100)::numeric))))
);


--
-- TOC entry 270 (class 1259 OID 24969)
-- Name: tb_postagens_id_postagem_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_postagens ALTER COLUMN id_postagem ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_postagens_id_postagem_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 287 (class 1259 OID 25204)
-- Name: tb_quizzes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_quizzes (
    id_quiz integer NOT NULL,
    curso_id integer NOT NULL,
    aula_id integer NOT NULL,
    titulo_quiz character varying(255) NOT NULL,
    nota_minima_aprovacao numeric(5,2) DEFAULT 70 NOT NULL,
    pontos_recompensa integer NOT NULL,
    ativo boolean DEFAULT true NOT NULL,
    criado_em timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_quizzes_nota_minima_aprovacao CHECK (((nota_minima_aprovacao >= (0)::numeric) AND (nota_minima_aprovacao <= (100)::numeric))),
    CONSTRAINT ck_quizzes_pontos_recompensa CHECK ((pontos_recompensa > 0))
);


--
-- TOC entry 286 (class 1259 OID 25203)
-- Name: tb_quizzes_id_quiz_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_quizzes ALTER COLUMN id_quiz ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_quizzes_id_quiz_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 265 (class 1259 OID 24883)
-- Name: tb_rel_cooperativas_categorias_materiais; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_rel_cooperativas_categorias_materiais (
    id_cooperativa_categoria integer CONSTRAINT tb_rel_cooperativas_categoria_id_cooperativa_categoria_not_null NOT NULL,
    cooperativa_id integer CONSTRAINT tb_rel_cooperativas_categorias_materiai_cooperativa_id_not_null NOT NULL,
    categoria_residuo_id integer CONSTRAINT tb_rel_cooperativas_categorias_ma_categoria_residuo_id_not_null NOT NULL
);


--
-- TOC entry 264 (class 1259 OID 24882)
-- Name: tb_rel_cooperativas_categorias_mat_id_cooperativa_categoria_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_rel_cooperativas_categorias_materiais ALTER COLUMN id_cooperativa_categoria ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_rel_cooperativas_categorias_mat_id_cooperativa_categoria_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 267 (class 1259 OID 24904)
-- Name: tb_rel_pontos_coletas_categorias; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_rel_pontos_coletas_categorias (
    id_ponto_coleta_categoria integer CONSTRAINT tb_rel_pontos_coletas_catego_id_ponto_coleta_categoria_not_null NOT NULL,
    ponto_coleta_id integer NOT NULL,
    categoria_residuo_id integer NOT NULL
);


--
-- TOC entry 266 (class 1259 OID 24903)
-- Name: tb_rel_pontos_coletas_categorias_id_ponto_coleta_categoria_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_rel_pontos_coletas_categorias ALTER COLUMN id_ponto_coleta_categoria ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_rel_pontos_coletas_categorias_id_ponto_coleta_categoria_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 281 (class 1259 OID 25152)
-- Name: tb_rel_recorrencias_agendamentos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_rel_recorrencias_agendamentos (
    id_recorrencia_agendamento integer CONSTRAINT tb_rel_recorrencias_agendam_id_recorrencia_agendamento_not_null NOT NULL,
    agendamento_coleta_id integer NOT NULL,
    dia_semana_id integer NOT NULL
);


--
-- TOC entry 280 (class 1259 OID 25151)
-- Name: tb_rel_recorrencias_agendamentos_id_recorrencia_agendamento_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_rel_recorrencias_agendamentos ALTER COLUMN id_recorrencia_agendamento ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_rel_recorrencias_agendamentos_id_recorrencia_agendamento_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 295 (class 1259 OID 25306)
-- Name: tb_rel_respostas_tentativas_quiz; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_rel_respostas_tentativas_quiz (
    id_resposta integer NOT NULL,
    tentativa_id integer NOT NULL,
    pergunta_id integer NOT NULL,
    alternativa_escolhida_id integer CONSTRAINT tb_rel_respostas_tentativas_q_alternativa_escolhida_id_not_null NOT NULL,
    correta boolean NOT NULL
);


--
-- TOC entry 294 (class 1259 OID 25305)
-- Name: tb_rel_respostas_tentativas_quiz_id_resposta_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_rel_respostas_tentativas_quiz ALTER COLUMN id_resposta ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_rel_respostas_tentativas_quiz_id_resposta_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 269 (class 1259 OID 24925)
-- Name: tb_rel_usuarios_condominios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_rel_usuarios_condominios (
    id_usuario_condominio integer NOT NULL,
    usuario_id integer NOT NULL,
    condominio_id integer NOT NULL,
    data_entrada timestamp with time zone DEFAULT now() NOT NULL,
    data_saida timestamp with time zone,
    aprovado boolean DEFAULT false NOT NULL,
    aprovado_por_usuario_id integer,
    nivel_confianca_id integer DEFAULT 1 NOT NULL,
    trust_score numeric(6,2) NOT NULL,
    postagens_validadas_sem_contestacao integer CONSTRAINT tb_rel_usuarios_condominios_postagens_validadas_sem_co_not_null NOT NULL,
    denuncias_realizadas integer NOT NULL,
    denuncias_procedentes integer NOT NULL,
    taxa_acerto_denuncias numeric(5,2),
    CONSTRAINT ck_usuarios_condominios_denuncias_procedentes CHECK (((denuncias_procedentes >= 0) AND (denuncias_procedentes <= denuncias_realizadas))),
    CONSTRAINT ck_usuarios_condominios_denuncias_realizadas CHECK ((denuncias_realizadas >= 0)),
    CONSTRAINT ck_usuarios_condominios_postagens_validadas CHECK ((postagens_validadas_sem_contestacao >= 0)),
    CONSTRAINT ck_usuarios_condominios_taxa_acerto_denuncias CHECK (((taxa_acerto_denuncias IS NULL) OR ((taxa_acerto_denuncias >= (0)::numeric) AND (taxa_acerto_denuncias <= (100)::numeric))))
);


--
-- TOC entry 268 (class 1259 OID 24924)
-- Name: tb_rel_usuarios_condominios_id_usuario_condominio_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_rel_usuarios_condominios ALTER COLUMN id_usuario_condominio ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_rel_usuarios_condominios_id_usuario_condominio_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 297 (class 1259 OID 25334)
-- Name: tb_rel_usuarios_cursos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_rel_usuarios_cursos (
    id_usuario_curso integer NOT NULL,
    usuario_id integer NOT NULL,
    aula_id integer NOT NULL,
    concluido boolean DEFAULT false NOT NULL,
    data_inicio timestamp with time zone DEFAULT now() NOT NULL,
    data_conclusao timestamp with time zone
);


--
-- TOC entry 296 (class 1259 OID 25333)
-- Name: tb_rel_usuarios_cursos_id_usuario_curso_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_rel_usuarios_cursos ALTER COLUMN id_usuario_curso ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_rel_usuarios_cursos_id_usuario_curso_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 273 (class 1259 OID 25026)
-- Name: tb_rel_votos_postagens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_rel_votos_postagens (
    id_voto integer NOT NULL,
    postagem_id integer NOT NULL,
    usuario_id integer NOT NULL,
    tipo_voto_id integer NOT NULL,
    motivo_denuncia_id integer,
    peso_aplicado integer NOT NULL,
    comentario character varying(255),
    votado_em timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_votos_postagens_peso_aplicado CHECK ((peso_aplicado > 0))
);


--
-- TOC entry 272 (class 1259 OID 25025)
-- Name: tb_rel_votos_postagens_id_voto_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_rel_votos_postagens ALTER COLUMN id_voto ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_rel_votos_postagens_id_voto_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 249 (class 1259 OID 24713)
-- Name: tb_sindicos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_sindicos (
    id_sindico integer NOT NULL,
    usuario_id integer NOT NULL
);


--
-- TOC entry 248 (class 1259 OID 24712)
-- Name: tb_sindicos_id_sindico_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_sindicos ALTER COLUMN id_sindico ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_sindicos_id_sindico_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 251 (class 1259 OID 24728)
-- Name: tb_telefones; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_telefones (
    id_telefone integer NOT NULL,
    usuario_id integer NOT NULL,
    numero_contato character varying(20) NOT NULL,
    tipo_telefone character varying(20),
    ativo boolean DEFAULT true NOT NULL
);


--
-- TOC entry 250 (class 1259 OID 24727)
-- Name: tb_telefones_id_telefone_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_telefones ALTER COLUMN id_telefone ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_telefones_id_telefone_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 293 (class 1259 OID 25272)
-- Name: tb_tentativas_quiz; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_tentativas_quiz (
    id_tentativa integer NOT NULL,
    usuario_id integer NOT NULL,
    quiz_id integer NOT NULL,
    condominio_id integer,
    torre_id integer,
    nota numeric(5,2),
    aprovado boolean DEFAULT false NOT NULL,
    iniciado_em timestamp with time zone DEFAULT now() NOT NULL,
    concluido_em timestamp with time zone,
    CONSTRAINT ck_tentativas_quiz_nota CHECK (((nota IS NULL) OR ((nota >= (0)::numeric) AND (nota <= (100)::numeric))))
);


--
-- TOC entry 292 (class 1259 OID 25271)
-- Name: tb_tentativas_quiz_id_tentativa_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_tentativas_quiz ALTER COLUMN id_tentativa ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_tentativas_quiz_id_tentativa_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 257 (class 1259 OID 24792)
-- Name: tb_torres; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_torres (
    id_torre integer NOT NULL,
    nome_torre character varying(100) NOT NULL,
    condominio_id integer NOT NULL
);


--
-- TOC entry 256 (class 1259 OID 24791)
-- Name: tb_torres_id_torre_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_torres ALTER COLUMN id_torre ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_torres_id_torre_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 247 (class 1259 OID 24683)
-- Name: tb_usuarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_usuarios (
    id_usuario integer NOT NULL,
    nome_usuario character varying(100) NOT NULL,
    email_usuario character varying(255) NOT NULL,
    senha_hash character varying(255) NOT NULL,
    data_nascimento date,
    cpf character varying(11),
    url_avatar character varying(500),
    ativo boolean DEFAULT true NOT NULL,
    registro_em timestamp with time zone DEFAULT now() NOT NULL,
    tipo_usuario_id integer NOT NULL,
    endereco_id integer NOT NULL
);


--
-- TOC entry 246 (class 1259 OID 24682)
-- Name: tb_usuarios_id_usuario_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_usuarios ALTER COLUMN id_usuario ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_usuarios_id_usuario_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 277 (class 1259 OID 25105)
-- Name: tb_visitas_coletas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tb_visitas_coletas (
    id_visita_coleta integer NOT NULL,
    agendamento_coleta_id integer NOT NULL,
    data_visita timestamp with time zone NOT NULL,
    foi_realizada boolean DEFAULT false NOT NULL,
    houve_confirmacao boolean DEFAULT false NOT NULL,
    confirmado_em timestamp with time zone,
    observacao text
);


--
-- TOC entry 276 (class 1259 OID 25104)
-- Name: tb_visitas_coletas_id_visita_coleta_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tb_visitas_coletas ALTER COLUMN id_visita_coleta ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tb_visitas_coletas_id_visita_coleta_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 318 (class 1259 OID 25629)
-- Name: vw_coletas_por_condominio; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.vw_coletas_por_condominio AS
 SELECT c.id_condominio,
    c.nome_condominio,
    count(DISTINCT ac.id_agendamento_coleta) AS coletas_agendadas,
    count(DISTINCT vc.id_visita_coleta) AS visitas_programadas,
    count(DISTINCT vc.id_visita_coleta) FILTER (WHERE (vc.foi_realizada = true)) AS coletas_realizadas,
    round(((100.0 * (count(DISTINCT vc.id_visita_coleta) FILTER (WHERE (vc.foi_realizada = true)))::numeric) / (NULLIF(count(DISTINCT vc.id_visita_coleta), 0))::numeric), 2) AS taxa_sucesso_percentual
   FROM ((public.tb_condominios c
     LEFT JOIN public.tb_agendamentos_coletas ac ON ((ac.condominio_id = c.id_condominio)))
     LEFT JOIN public.tb_visitas_coletas vc ON ((vc.agendamento_coleta_id = ac.id_agendamento_coleta)))
  GROUP BY c.id_condominio, c.nome_condominio;


--
-- TOC entry 319 (class 1259 OID 25634)
-- Name: vw_coletas_por_cooperativa; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.vw_coletas_por_cooperativa AS
 SELECT coop.id_cooperativa,
    coop.nome_cooperativa,
    count(DISTINCT ac.id_agendamento_coleta) AS coletas_agendadas,
    count(DISTINCT vc.id_visita_coleta) AS visitas_programadas,
    count(DISTINCT vc.id_visita_coleta) FILTER (WHERE (vc.foi_realizada = true)) AS coletas_realizadas,
    round(((100.0 * (count(DISTINCT vc.id_visita_coleta) FILTER (WHERE (vc.foi_realizada = true)))::numeric) / (NULLIF(count(DISTINCT vc.id_visita_coleta), 0))::numeric), 2) AS taxa_sucesso_percentual
   FROM ((public.tb_cooperativas coop
     LEFT JOIN public.tb_agendamentos_coletas ac ON ((ac.cooperativa_id = coop.id_cooperativa)))
     LEFT JOIN public.tb_visitas_coletas vc ON ((vc.agendamento_coleta_id = ac.id_agendamento_coleta)))
  GROUP BY coop.id_cooperativa, coop.nome_cooperativa;


--
-- TOC entry 317 (class 1259 OID 25624)
-- Name: vw_coletas_por_mes; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.vw_coletas_por_mes AS
 SELECT (EXTRACT(year FROM COALESCE(vc.data_visita, ac.data_inicio)))::integer AS ano,
    (EXTRACT(month FROM COALESCE(vc.data_visita, ac.data_inicio)))::integer AS mes,
    count(DISTINCT ac.id_agendamento_coleta) AS coletas_agendadas,
    count(DISTINCT vc.id_visita_coleta) AS visitas_programadas,
    count(DISTINCT vc.id_visita_coleta) FILTER (WHERE (vc.foi_realizada = true)) AS coletas_realizadas
   FROM (public.tb_agendamentos_coletas ac
     LEFT JOIN public.tb_visitas_coletas vc ON ((vc.agendamento_coleta_id = ac.id_agendamento_coleta)))
  GROUP BY (EXTRACT(year FROM COALESCE(vc.data_visita, ac.data_inicio))), (EXTRACT(month FROM COALESCE(vc.data_visita, ac.data_inicio)));


--
-- TOC entry 321 (class 1259 OID 25644)
-- Name: vw_crescimento_usuarios; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.vw_crescimento_usuarios AS
 SELECT (EXTRACT(year FROM u.registro_em))::integer AS ano,
    (EXTRACT(month FROM u.registro_em))::integer AS mes,
    tu.id_tipo_usuario,
    tu.nome_tipo,
    count(u.id_usuario) AS novos_usuarios
   FROM (public.tb_usuarios u
     LEFT JOIN public.tb_lkp_tipos_usuarios tu ON ((tu.id_tipo_usuario = u.tipo_usuario_id)))
  GROUP BY (EXTRACT(year FROM u.registro_em)), (EXTRACT(month FROM u.registro_em)), tu.id_tipo_usuario, tu.nome_tipo;


--
-- TOC entry 336 (class 1259 OID 25803)
-- Name: vw_dashboard_sindico; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.vw_dashboard_sindico AS
 WITH moradores AS (
         SELECT m.condominio_id,
            count(*) AS total_moradores
           FROM public.tb_moradores m
          GROUP BY m.condominio_id
        ), postagens AS (
         SELECT p.condominio_id,
            count(*) FILTER (WHERE (((sv.nome_status)::text = 'aprovada'::text) AND (p.resolvido_em IS NOT NULL))) AS materiais_reciclados,
            count(*) FILTER (WHERE (p.resolvido_em IS NULL)) AS postagens_em_validacao,
            count(*) FILTER (WHERE (((sv.nome_status)::text = 'em_analise'::text) AND (p.resolvido_em IS NOT NULL))) AS postagens_aguardando_sindico,
            count(*) FILTER (WHERE (((sv.nome_status)::text = 'reprovada'::text) AND (p.resolvido_em IS NOT NULL))) AS postagens_reprovadas
           FROM (public.tb_postagens p
             JOIN public.tb_lkp_status_validacoes_postagens sv ON ((sv.id_status_validacao = p.status_validacao_id)))
          GROUP BY p.condominio_id
        ), materiais_categoria AS (
         SELECT p.condominio_id,
            cr.id_categoria,
            cr.nome_categoria,
            cr.cor_identificacao,
            count(*) AS quantidade
           FROM ((public.tb_postagens p
             JOIN public.tb_lkp_status_validacoes_postagens sv ON ((sv.id_status_validacao = p.status_validacao_id)))
             JOIN public.tb_lkp_categorias_residuos cr ON ((cr.id_categoria = p.categoria_id)))
          WHERE (((sv.nome_status)::text = 'aprovada'::text) AND (p.resolvido_em IS NOT NULL))
          GROUP BY p.condominio_id, cr.id_categoria, cr.nome_categoria, cr.cor_identificacao
        ), materiais_json AS (
         SELECT materiais_categoria.condominio_id,
            jsonb_agg(jsonb_build_object('id_categoria', materiais_categoria.id_categoria, 'categoria', materiais_categoria.nome_categoria, 'cor', materiais_categoria.cor_identificacao, 'quantidade', materiais_categoria.quantidade) ORDER BY materiais_categoria.quantidade DESC, materiais_categoria.nome_categoria) AS materiais_por_categoria
           FROM materiais_categoria
          GROUP BY materiais_categoria.condominio_id
        ), pontos AS (
         SELECT mp.condominio_id,
            sum(
                CASE
                    WHEN ((mp.tipo_movimentacao)::text = 'ESTORNO'::text) THEN (- mp.pontos)
                    ELSE mp.pontos
                END) AS pontos_total,
            sum(
                CASE
                    WHEN ((mp.tipo_movimentacao)::text = 'ESTORNO'::text) THEN (- mp.pontos)
                    ELSE mp.pontos
                END) FILTER (WHERE ((mp.origem_tipo)::text = 'POSTAGEM'::text)) AS pontos_postagens,
            sum(
                CASE
                    WHEN ((mp.tipo_movimentacao)::text = 'ESTORNO'::text) THEN (- mp.pontos)
                    ELSE mp.pontos
                END) FILTER (WHERE ((mp.origem_tipo)::text = 'QUIZ'::text)) AS pontos_quizzes,
            sum(
                CASE
                    WHEN ((mp.tipo_movimentacao)::text = 'ESTORNO'::text) THEN (- mp.pontos)
                    ELSE mp.pontos
                END) FILTER (WHERE (mp.ocorrido_em >= (date_trunc('month'::text, (now() AT TIME ZONE 'America/Sao_Paulo'::text)) AT TIME ZONE 'America/Sao_Paulo'::text))) AS pontos_mes_atual
           FROM public.tb_movimentacoes_pontos mp
          WHERE (mp.condominio_id IS NOT NULL)
          GROUP BY mp.condominio_id
        ), quizzes AS (
         SELECT tq.condominio_id,
            count(*) FILTER (WHERE (tq.concluido_em IS NOT NULL)) AS tentativas_concluidas,
            count(*) FILTER (WHERE (tq.aprovado = true)) AS tentativas_aprovadas,
            count(DISTINCT tq.usuario_id) FILTER (WHERE (tq.concluido_em IS NOT NULL)) AS moradores_com_quiz,
            round(avg(tq.nota) FILTER (WHERE (tq.concluido_em IS NOT NULL)), 2) AS nota_media_quizzes,
            round(((100.0 * (count(*) FILTER (WHERE (tq.aprovado = true)))::numeric) / (NULLIF(count(*) FILTER (WHERE (tq.concluido_em IS NOT NULL)), 0))::numeric), 2) AS taxa_aprovacao_quizzes
           FROM public.tb_tentativas_quiz tq
          WHERE (tq.condominio_id IS NOT NULL)
          GROUP BY tq.condominio_id
        ), quizzes_detalhe AS (
         SELECT tq.condominio_id,
            q.id_quiz,
            q.titulo_quiz,
            count(*) AS tentativas,
            count(*) FILTER (WHERE (tq.aprovado = true)) AS aprovadas,
            round(avg(tq.nota), 2) AS nota_media
           FROM (public.tb_tentativas_quiz tq
             JOIN public.tb_quizzes q ON ((q.id_quiz = tq.quiz_id)))
          WHERE ((tq.condominio_id IS NOT NULL) AND (tq.concluido_em IS NOT NULL))
          GROUP BY tq.condominio_id, q.id_quiz, q.titulo_quiz
        ), quizzes_json AS (
         SELECT quizzes_detalhe.condominio_id,
            jsonb_agg(jsonb_build_object('id_quiz', quizzes_detalhe.id_quiz, 'quiz', quizzes_detalhe.titulo_quiz, 'tentativas', quizzes_detalhe.tentativas, 'aprovadas', quizzes_detalhe.aprovadas, 'nota_media', quizzes_detalhe.nota_media) ORDER BY quizzes_detalhe.titulo_quiz) AS notas_por_quiz
           FROM quizzes_detalhe
          GROUP BY quizzes_detalhe.condominio_id
        )
 SELECT c.id_condominio,
    c.nome_condominio,
    s.usuario_id AS sindico_usuario_id,
    us.nome_usuario AS nome_sindico,
    COALESCE(mo.total_moradores, (0)::bigint) AS total_moradores,
    COALESCE(po.materiais_reciclados, (0)::bigint) AS materiais_reciclados,
    COALESCE(po.postagens_em_validacao, (0)::bigint) AS postagens_em_validacao,
    COALESCE(po.postagens_aguardando_sindico, (0)::bigint) AS postagens_aguardando_sindico,
    COALESCE(po.postagens_reprovadas, (0)::bigint) AS postagens_reprovadas,
    COALESCE(mj.materiais_por_categoria, '[]'::jsonb) AS materiais_por_categoria,
    COALESCE(pt.pontos_total, (0)::bigint) AS pontos_total,
    COALESCE(pt.pontos_postagens, (0)::bigint) AS pontos_postagens,
    COALESCE(pt.pontos_quizzes, (0)::bigint) AS pontos_quizzes,
    COALESCE(pt.pontos_mes_atual, (0)::bigint) AS pontos_mes_atual,
    COALESCE(qz.tentativas_concluidas, (0)::bigint) AS tentativas_quiz_concluidas,
    COALESCE(qz.tentativas_aprovadas, (0)::bigint) AS tentativas_quiz_aprovadas,
    COALESCE(qz.moradores_com_quiz, (0)::bigint) AS moradores_com_quiz,
    qz.nota_media_quizzes,
    qz.taxa_aprovacao_quizzes,
    COALESCE(qj.notas_por_quiz, '[]'::jsonb) AS notas_por_quiz
   FROM ((((((((public.tb_condominios c
     LEFT JOIN public.tb_sindicos s ON ((s.id_sindico = c.sindico_id)))
     LEFT JOIN public.tb_usuarios us ON ((us.id_usuario = s.usuario_id)))
     LEFT JOIN moradores mo ON ((mo.condominio_id = c.id_condominio)))
     LEFT JOIN postagens po ON ((po.condominio_id = c.id_condominio)))
     LEFT JOIN materiais_json mj ON ((mj.condominio_id = c.id_condominio)))
     LEFT JOIN pontos pt ON ((pt.condominio_id = c.id_condominio)))
     LEFT JOIN quizzes qz ON ((qz.condominio_id = c.id_condominio)))
     LEFT JOIN quizzes_json qj ON ((qj.condominio_id = c.id_condominio)));


--
-- TOC entry 329 (class 1259 OID 25717)
-- Name: vw_dau_por_tipo_usuario; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.vw_dau_por_tipo_usuario AS
 SELECT a.data_atividade,
    tu.nome_tipo,
    count(*) AS dau,
    sum(a.qtd_sessoes) AS sessoes,
    round(avg(a.minutos_ativos), 2) AS minutos_medios
   FROM ((public.tb_atividades_diarias_usuarios a
     JOIN public.tb_usuarios u ON ((u.id_usuario = a.usuario_id)))
     JOIN public.tb_lkp_tipos_usuarios tu ON ((tu.id_tipo_usuario = u.tipo_usuario_id)))
  GROUP BY a.data_atividade, tu.nome_tipo;


--
-- TOC entry 325 (class 1259 OID 25663)
-- Name: vw_denuncias_por_motivo; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.vw_denuncias_por_motivo AS
 SELECT md.id_motivo_denuncia,
    md.descricao AS motivo,
    md.ativo,
    count(vp.id_voto) AS qtd_denuncias,
    count(DISTINCT vp.postagem_id) AS qtd_postagens_denunciadas,
    count(vp.id_voto) FILTER (WHERE ((svp.nome_status)::text = 'reprovada'::text)) AS qtd_denuncias_procedentes,
    round(((100.0 * (count(vp.id_voto) FILTER (WHERE ((svp.nome_status)::text = 'reprovada'::text)))::numeric) / (NULLIF(count(vp.id_voto), 0))::numeric), 2) AS taxa_procedencia_percentual
   FROM (((public.tb_lkp_motivos_denuncia md
     LEFT JOIN public.tb_rel_votos_postagens vp ON ((vp.motivo_denuncia_id = md.id_motivo_denuncia)))
     LEFT JOIN public.tb_postagens p ON ((p.id_postagem = vp.postagem_id)))
     LEFT JOIN public.tb_lkp_status_validacoes_postagens svp ON ((svp.id_status_validacao = p.status_validacao_id)))
  GROUP BY md.id_motivo_denuncia, md.descricao, md.ativo;


--
-- TOC entry 316 (class 1259 OID 25619)
-- Name: vw_desempenho_coletas; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.vw_desempenho_coletas AS
 SELECT c.id_condominio,
    c.nome_condominio,
    count(DISTINCT ac.id_agendamento_coleta) AS coletas_agendadas,
    count(DISTINCT vc.id_visita_coleta) AS visitas_programadas,
    count(DISTINCT vc.id_visita_coleta) FILTER (WHERE (vc.foi_realizada = true)) AS coletas_realizadas,
    round(((100.0 * (count(DISTINCT vc.id_visita_coleta) FILTER (WHERE (vc.foi_realizada = true)))::numeric) / (NULLIF(count(DISTINCT vc.id_visita_coleta), 0))::numeric), 2) AS taxa_sucesso_percentual
   FROM ((public.tb_condominios c
     LEFT JOIN public.tb_agendamentos_coletas ac ON ((ac.condominio_id = c.id_condominio)))
     LEFT JOIN public.tb_visitas_coletas vc ON ((vc.agendamento_coleta_id = ac.id_agendamento_coleta)))
  GROUP BY c.id_condominio, c.nome_condominio;


--
-- TOC entry 311 (class 1259 OID 25594)
-- Name: vw_desempenho_condominio; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.vw_desempenho_condominio AS
 WITH moradores_condominio AS (
         SELECT m.id_morador,
            m.usuario_id,
            m.condominio_id
           FROM public.tb_moradores m
        ), metricas_moradores AS (
         SELECT moradores_condominio.condominio_id,
            count(DISTINCT moradores_condominio.id_morador) AS total_moradores
           FROM moradores_condominio
          GROUP BY moradores_condominio.condominio_id
        ), metricas_postagens AS (
         SELECT p.condominio_id,
            count(DISTINCT p.id_postagem) AS total_postagens,
            count(DISTINCT p.id_postagem) FILTER (WHERE ((svp.nome_status)::text = 'aprovada'::text)) AS postagens_aprovadas,
            count(DISTINCT p.id_postagem) FILTER (WHERE ((svp.nome_status)::text = 'reprovada'::text)) AS postagens_rejeitadas,
            count(DISTINCT p.id_postagem) FILTER (WHERE ((svp.nome_status)::text = 'em_analise'::text)) AS postagens_pendentes
           FROM (public.tb_postagens p
             LEFT JOIN public.tb_lkp_status_validacoes_postagens svp ON ((svp.id_status_validacao = p.status_validacao_id)))
          GROUP BY p.condominio_id
        ), metricas_quizzes AS (
         SELECT mc_1.condominio_id,
            count(DISTINCT uc.id_usuario_curso) FILTER (WHERE (uc.concluido = true)) AS quizzes_realizados
           FROM (moradores_condominio mc_1
             JOIN public.tb_rel_usuarios_cursos uc ON ((uc.usuario_id = mc_1.usuario_id)))
          GROUP BY mc_1.condominio_id
        ), metricas_coletas AS (
         SELECT ac.condominio_id,
            count(DISTINCT vc.id_visita_coleta) AS tb_visitas_coletas,
            count(DISTINCT vc.id_visita_coleta) FILTER (WHERE (vc.foi_realizada = true)) AS visitas_coletas_realizadas
           FROM (public.tb_agendamentos_coletas ac
             LEFT JOIN public.tb_visitas_coletas vc ON ((vc.agendamento_coleta_id = ac.id_agendamento_coleta)))
          GROUP BY ac.condominio_id
        )
 SELECT c.id_condominio,
    c.nome_condominio,
    COALESCE(mm.total_moradores, (0)::bigint) AS total_moradores,
    COALESCE(mp.total_postagens, (0)::bigint) AS total_postagens,
    COALESCE(mp.postagens_aprovadas, (0)::bigint) AS postagens_aprovadas,
    COALESCE(mp.postagens_rejeitadas, (0)::bigint) AS postagens_rejeitadas,
    COALESCE(mp.postagens_pendentes, (0)::bigint) AS postagens_pendentes,
    COALESCE(mq.quizzes_realizados, (0)::bigint) AS quizzes_realizados,
    COALESCE(mc.tb_visitas_coletas, (0)::bigint) AS tb_visitas_coletas,
    COALESCE(mc.visitas_coletas_realizadas, (0)::bigint) AS visitas_coletas_realizadas
   FROM ((((public.tb_condominios c
     LEFT JOIN metricas_moradores mm ON ((mm.condominio_id = c.id_condominio)))
     LEFT JOIN metricas_postagens mp ON ((mp.condominio_id = c.id_condominio)))
     LEFT JOIN metricas_quizzes mq ON ((mq.condominio_id = c.id_condominio)))
     LEFT JOIN metricas_coletas mc ON ((mc.condominio_id = c.id_condominio)));


--
-- TOC entry 324 (class 1259 OID 25658)
-- Name: vw_desempenho_quizzes; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.vw_desempenho_quizzes AS
 SELECT q.id_quiz,
    q.titulo_quiz,
    a.id_aula,
    a.titulo_aula,
    cu.id_curso,
    cu.titulo_curso,
    q.nota_minima_aprovacao,
    q.ativo,
    count(DISTINCT tq.usuario_id) AS usuarios_unicos,
    count(tq.id_tentativa) AS total_tentativas,
    count(tq.id_tentativa) FILTER (WHERE (tq.concluido_em IS NOT NULL)) AS tentativas_concluidas,
    count(tq.id_tentativa) FILTER (WHERE (tq.aprovado = true)) AS tentativas_aprovadas,
    round(((100.0 * (count(tq.id_tentativa) FILTER (WHERE (tq.aprovado = true)))::numeric) / (NULLIF(count(tq.id_tentativa) FILTER (WHERE (tq.concluido_em IS NOT NULL)), 0))::numeric), 2) AS taxa_aprovacao_percentual,
    round(avg(tq.nota) FILTER (WHERE (tq.concluido_em IS NOT NULL)), 2) AS nota_media
   FROM (((public.tb_quizzes q
     JOIN public.tb_aulas a ON ((a.id_aula = q.aula_id)))
     JOIN public.tb_cursos cu ON ((cu.id_curso = a.curso_id)))
     LEFT JOIN public.tb_tentativas_quiz tq ON ((tq.quiz_id = q.id_quiz)))
  GROUP BY q.id_quiz, q.titulo_quiz, a.id_aula, a.titulo_aula, cu.id_curso, cu.titulo_curso, q.nota_minima_aprovacao, q.ativo;


--
-- TOC entry 323 (class 1259 OID 25654)
-- Name: vw_distribuicao_moradores; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.vw_distribuicao_moradores AS
 SELECT c.id_condominio,
    c.nome_condominio,
    count(DISTINCT m.id_morador) AS qtd_moradores
   FROM (public.tb_condominios c
     LEFT JOIN public.tb_moradores m ON ((m.condominio_id = c.id_condominio)))
  GROUP BY c.id_condominio, c.nome_condominio;


--
-- TOC entry 310 (class 1259 OID 25589)
-- Name: vw_engajamento_moradores; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.vw_engajamento_moradores AS
 WITH morador_base AS (
         SELECT m.id_morador,
            u.id_usuario,
            u.nome_usuario,
            m.condominio_id,
            c.nome_condominio
           FROM ((public.tb_moradores m
             JOIN public.tb_usuarios u ON ((u.id_usuario = m.usuario_id)))
             LEFT JOIN public.tb_condominios c ON ((c.id_condominio = m.condominio_id)))
        ), acoes_foto AS (
         SELECT p.usuario_id,
            p.condominio_id,
            count(*) AS qtd_fotos
           FROM public.tb_postagens p
          GROUP BY p.usuario_id, p.condominio_id
        ), acoes_quiz AS (
         SELECT uc.usuario_id,
            count(*) FILTER (WHERE (uc.concluido = true)) AS qtd_quizzes
           FROM public.tb_rel_usuarios_cursos uc
          GROUP BY uc.usuario_id
        )
 SELECT mb.id_morador,
    mb.id_usuario,
    mb.nome_usuario,
    mb.condominio_id,
    mb.nome_condominio,
    COALESCE(af.qtd_fotos, (0)::bigint) AS qtd_fotos_realizadas,
    COALESCE(aq.qtd_quizzes, (0)::bigint) AS qtd_quizzes_realizados,
    (COALESCE(af.qtd_fotos, (0)::bigint) + COALESCE(aq.qtd_quizzes, (0)::bigint)) AS qtd_acoes_total
   FROM ((morador_base mb
     LEFT JOIN acoes_foto af ON (((af.usuario_id = mb.id_usuario) AND (NOT (af.condominio_id IS DISTINCT FROM mb.condominio_id)))))
     LEFT JOIN acoes_quiz aq ON ((aq.usuario_id = mb.id_usuario)));


--
-- TOC entry 315 (class 1259 OID 25614)
-- Name: vw_participacao_sustentabilidade; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.vw_participacao_sustentabilidade AS
 WITH moradores_condominio AS (
         SELECT m.id_morador,
            m.usuario_id,
            m.condominio_id
           FROM public.tb_moradores m
        ), acoes AS (
         SELECT DISTINCT p.usuario_id,
            p.condominio_id
           FROM public.tb_postagens p
        UNION
         SELECT DISTINCT uc.usuario_id,
            mc_1.condominio_id
           FROM (public.tb_rel_usuarios_cursos uc
             JOIN moradores_condominio mc_1 ON ((mc_1.usuario_id = uc.usuario_id)))
          WHERE (uc.concluido = true)
        )
 SELECT c.id_condominio,
    c.nome_condominio,
    count(DISTINCT mc.id_morador) AS total_moradores,
    count(DISTINCT mc.id_morador) FILTER (WHERE (a.usuario_id IS NOT NULL)) AS moradores_participantes,
    round(((100.0 * (count(DISTINCT mc.id_morador) FILTER (WHERE (a.usuario_id IS NOT NULL)))::numeric) / (NULLIF(count(DISTINCT mc.id_morador), 0))::numeric), 2) AS taxa_participacao_percentual
   FROM ((public.tb_condominios c
     LEFT JOIN moradores_condominio mc ON ((mc.condominio_id = c.id_condominio)))
     LEFT JOIN acoes a ON (((a.usuario_id = mc.usuario_id) AND (NOT (a.condominio_id IS DISTINCT FROM mc.condominio_id)))))
  GROUP BY c.id_condominio, c.nome_condominio;


--
-- TOC entry 313 (class 1259 OID 25604)
-- Name: vw_postagens_aprovadas_rejeitadas_condominio; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.vw_postagens_aprovadas_rejeitadas_condominio AS
 SELECT p.condominio_id,
    c.nome_condominio,
    count(*) AS total_postagens,
    count(*) FILTER (WHERE ((svp.nome_status)::text = 'aprovada'::text)) AS qtd_aprovadas,
    count(*) FILTER (WHERE ((svp.nome_status)::text = 'reprovada'::text)) AS qtd_rejeitadas,
    count(*) FILTER (WHERE ((svp.nome_status)::text = 'em_analise'::text)) AS qtd_pendentes,
    round(((100.0 * (count(*) FILTER (WHERE ((svp.nome_status)::text = 'aprovada'::text)))::numeric) / (NULLIF(count(*), 0))::numeric), 2) AS taxa_aprovacao_percentual,
    round(((100.0 * (count(*) FILTER (WHERE ((svp.nome_status)::text = 'reprovada'::text)))::numeric) / (NULLIF(count(*), 0))::numeric), 2) AS taxa_rejeicao_percentual,
    round(((100.0 * (count(*) FILTER (WHERE ((svp.nome_status)::text = 'em_analise'::text)))::numeric) / (NULLIF(count(*), 0))::numeric), 2) AS taxa_pendencia_percentual
   FROM ((public.tb_postagens p
     LEFT JOIN public.tb_condominios c ON ((c.id_condominio = p.condominio_id)))
     LEFT JOIN public.tb_lkp_status_validacoes_postagens svp ON ((svp.id_status_validacao = p.status_validacao_id)))
  GROUP BY p.condominio_id, c.nome_condominio;


--
-- TOC entry 312 (class 1259 OID 25599)
-- Name: vw_postagens_por_categoria; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.vw_postagens_por_categoria AS
 SELECT cr.id_categoria,
    cr.nome_categoria,
    count(p.id_postagem) AS qtd_postagens,
    count(p.id_postagem) FILTER (WHERE ((svp.nome_status)::text = 'aprovada'::text)) AS qtd_aprovadas,
    count(p.id_postagem) FILTER (WHERE ((svp.nome_status)::text = 'reprovada'::text)) AS qtd_rejeitadas,
    count(p.id_postagem) FILTER (WHERE ((svp.nome_status)::text = 'em_analise'::text)) AS qtd_pendentes
   FROM ((public.tb_lkp_categorias_residuos cr
     LEFT JOIN public.tb_postagens p ON ((p.categoria_id = cr.id_categoria)))
     LEFT JOIN public.tb_lkp_status_validacoes_postagens svp ON ((svp.id_status_validacao = p.status_validacao_id)))
  GROUP BY cr.id_categoria, cr.nome_categoria;


--
-- TOC entry 314 (class 1259 OID 25609)
-- Name: vw_residuos_por_periodo; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.vw_residuos_por_periodo AS
 SELECT (EXTRACT(year FROM data_postagem))::integer AS ano,
    (EXTRACT(month FROM data_postagem))::integer AS mes,
    make_date((EXTRACT(year FROM data_postagem))::integer, (EXTRACT(month FROM data_postagem))::integer, 1) AS periodo,
    count(id_postagem) AS qtd_residuos_registrados
   FROM public.tb_postagens p
  GROUP BY (EXTRACT(year FROM data_postagem)), (EXTRACT(month FROM data_postagem))
  ORDER BY ((EXTRACT(year FROM data_postagem))::integer), ((EXTRACT(month FROM data_postagem))::integer);


--
-- TOC entry 330 (class 1259 OID 25722)
-- Name: vw_retencao_coortes_mensais; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.vw_retencao_coortes_mensais AS
 WITH coortes AS (
         SELECT tb_usuarios.id_usuario,
            (date_trunc('month'::text, (tb_usuarios.registro_em AT TIME ZONE 'America/Sao_Paulo'::text)))::date AS mes_coorte
           FROM public.tb_usuarios
        ), tamanho AS (
         SELECT coortes.mes_coorte,
            count(*) AS usuarios_coorte
           FROM coortes
          GROUP BY coortes.mes_coorte
        ), atividade AS (
         SELECT DISTINCT c.mes_coorte,
            a_1.usuario_id,
            (((((EXTRACT(year FROM a_1.data_atividade) - EXTRACT(year FROM c.mes_coorte)) * (12)::numeric) + EXTRACT(month FROM a_1.data_atividade)) - EXTRACT(month FROM c.mes_coorte)))::integer AS mes_relativo
           FROM (public.tb_atividades_diarias_usuarios a_1
             JOIN coortes c ON ((c.id_usuario = a_1.usuario_id)))
        )
 SELECT a.mes_coorte,
    t.usuarios_coorte,
    a.mes_relativo,
    count(*) AS usuarios_ativos,
    round(((100.0 * (count(*))::numeric) / (t.usuarios_coorte)::numeric), 2) AS taxa_retencao_percentual
   FROM (atividade a
     JOIN tamanho t ON ((t.mes_coorte = a.mes_coorte)))
  WHERE (a.mes_relativo >= 0)
  GROUP BY a.mes_coorte, t.usuarios_coorte, a.mes_relativo;


--
-- TOC entry 320 (class 1259 OID 25639)
-- Name: vw_taxa_comparecimento_cooperativas; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.vw_taxa_comparecimento_cooperativas AS
 SELECT coop.id_cooperativa,
    coop.nome_cooperativa,
    count(DISTINCT vc.id_visita_coleta) AS visitas_programadas,
    count(DISTINCT vc.id_visita_coleta) FILTER (WHERE (vc.foi_realizada = true)) AS visitas_realizadas,
    round(((100.0 * (count(DISTINCT vc.id_visita_coleta) FILTER (WHERE (vc.foi_realizada = true)))::numeric) / (NULLIF(count(DISTINCT vc.id_visita_coleta), 0))::numeric), 2) AS taxa_comparecimento_percentual
   FROM ((public.tb_cooperativas coop
     LEFT JOIN public.tb_agendamentos_coletas ac ON ((ac.cooperativa_id = coop.id_cooperativa)))
     LEFT JOIN public.tb_visitas_coletas vc ON ((vc.agendamento_coleta_id = ac.id_agendamento_coleta)))
  GROUP BY coop.id_cooperativa, coop.nome_cooperativa;


--
-- TOC entry 322 (class 1259 OID 25649)
-- Name: vw_usuarios_ativos_mensal; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.vw_usuarios_ativos_mensal AS
 WITH acoes_usuarios AS (
         SELECT p.usuario_id,
            p.condominio_id,
            p.data_postagem AS data_acao
           FROM public.tb_postagens p
        UNION ALL
         SELECT uc.usuario_id,
            m.condominio_id,
            uc.data_conclusao AS data_acao
           FROM (public.tb_rel_usuarios_cursos uc
             LEFT JOIN public.tb_moradores m ON ((m.usuario_id = uc.usuario_id)))
          WHERE ((uc.concluido = true) AND (uc.data_conclusao IS NOT NULL))
        )
 SELECT (EXTRACT(year FROM au.data_acao))::integer AS ano,
    (EXTRACT(month FROM au.data_acao))::integer AS mes,
    au.condominio_id,
    c.nome_condominio,
    count(DISTINCT u.id_usuario) AS usuarios_ativos
   FROM ((acoes_usuarios au
     JOIN public.tb_usuarios u ON (((u.id_usuario = au.usuario_id) AND (u.ativo = true))))
     LEFT JOIN public.tb_condominios c ON ((c.id_condominio = au.condominio_id)))
  WHERE (au.data_acao IS NOT NULL)
  GROUP BY (EXTRACT(year FROM au.data_acao)), (EXTRACT(month FROM au.data_acao)), au.condominio_id, c.nome_condominio;


--
-- TOC entry 3741 (class 2604 OID 73988)
-- Name: dim_categoria_residuo sk_categoria; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dim_categoria_residuo ALTER COLUMN sk_categoria SET DEFAULT nextval('public.dim_categoria_residuo_sk_categoria_seq'::regclass);


--
-- TOC entry 3737 (class 2604 OID 73962)
-- Name: dim_condominio sk_condominio; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dim_condominio ALTER COLUMN sk_condominio SET DEFAULT nextval('public.dim_condominio_sk_condominio_seq'::regclass);


--
-- TOC entry 3739 (class 2604 OID 73975)
-- Name: dim_cooperativa sk_cooperativa; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dim_cooperativa ALTER COLUMN sk_cooperativa SET DEFAULT nextval('public.dim_cooperativa_sk_cooperativa_seq'::regclass);


--
-- TOC entry 3744 (class 2604 OID 74015)
-- Name: dim_status_agendamento sk_status_agendamento; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dim_status_agendamento ALTER COLUMN sk_status_agendamento SET DEFAULT nextval('public.dim_status_agendamento_sk_status_agendamento_seq'::regclass);


--
-- TOC entry 3743 (class 2604 OID 74001)
-- Name: dim_status_postagem sk_status_postagem; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dim_status_postagem ALTER COLUMN sk_status_postagem SET DEFAULT nextval('public.dim_status_postagem_sk_status_postagem_seq'::regclass);


--
-- TOC entry 3735 (class 2604 OID 73949)
-- Name: dim_usuario sk_usuario; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dim_usuario ALTER COLUMN sk_usuario SET DEFAULT nextval('public.dim_usuario_sk_usuario_seq'::regclass);


--
-- TOC entry 3749 (class 2604 OID 74077)
-- Name: fato_coletas sk_fato_coleta; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fato_coletas ALTER COLUMN sk_fato_coleta SET DEFAULT nextval('public.fato_coletas_sk_fato_coleta_seq'::regclass);


--
-- TOC entry 3745 (class 2604 OID 74026)
-- Name: fato_postagens sk_fato_postagem; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fato_postagens ALTER COLUMN sk_fato_postagem SET DEFAULT nextval('public.fato_postagens_sk_fato_postagem_seq'::regclass);


--
-- TOC entry 3895 (class 2606 OID 25197)
-- Name: tb_aulas aulas_index_4; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_aulas
    ADD CONSTRAINT aulas_index_4 UNIQUE (curso_id, ordem);


--
-- TOC entry 3853 (class 2606 OID 24892)
-- Name: tb_rel_cooperativas_categorias_materiais cooperativas_categorias_materiais_index_1; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_cooperativas_categorias_materiais
    ADD CONSTRAINT cooperativas_categorias_materiais_index_1 UNIQUE (cooperativa_id, categoria_residuo_id);


--
-- TOC entry 3996 (class 2606 OID 73996)
-- Name: dim_categoria_residuo dim_categoria_residuo_id_categoria_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dim_categoria_residuo
    ADD CONSTRAINT dim_categoria_residuo_id_categoria_key UNIQUE (id_categoria);


--
-- TOC entry 3998 (class 2606 OID 73994)
-- Name: dim_categoria_residuo dim_categoria_residuo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dim_categoria_residuo
    ADD CONSTRAINT dim_categoria_residuo_pkey PRIMARY KEY (sk_categoria);


--
-- TOC entry 3988 (class 2606 OID 73970)
-- Name: dim_condominio dim_condominio_id_condominio_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dim_condominio
    ADD CONSTRAINT dim_condominio_id_condominio_key UNIQUE (id_condominio);


--
-- TOC entry 3990 (class 2606 OID 73968)
-- Name: dim_condominio dim_condominio_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dim_condominio
    ADD CONSTRAINT dim_condominio_pkey PRIMARY KEY (sk_condominio);


--
-- TOC entry 3992 (class 2606 OID 73983)
-- Name: dim_cooperativa dim_cooperativa_id_cooperativa_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dim_cooperativa
    ADD CONSTRAINT dim_cooperativa_id_cooperativa_key UNIQUE (id_cooperativa);


--
-- TOC entry 3994 (class 2606 OID 73981)
-- Name: dim_cooperativa dim_cooperativa_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dim_cooperativa
    ADD CONSTRAINT dim_cooperativa_pkey PRIMARY KEY (sk_cooperativa);


--
-- TOC entry 4006 (class 2606 OID 74021)
-- Name: dim_status_agendamento dim_status_agendamento_id_status_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dim_status_agendamento
    ADD CONSTRAINT dim_status_agendamento_id_status_key UNIQUE (id_status);


--
-- TOC entry 4008 (class 2606 OID 74019)
-- Name: dim_status_agendamento dim_status_agendamento_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dim_status_agendamento
    ADD CONSTRAINT dim_status_agendamento_pkey PRIMARY KEY (sk_status_agendamento);


--
-- TOC entry 4000 (class 2606 OID 74010)
-- Name: dim_status_postagem dim_status_postagem_codigo_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dim_status_postagem
    ADD CONSTRAINT dim_status_postagem_codigo_key UNIQUE (codigo);


--
-- TOC entry 4002 (class 2606 OID 74008)
-- Name: dim_status_postagem dim_status_postagem_id_status_validacao_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dim_status_postagem
    ADD CONSTRAINT dim_status_postagem_id_status_validacao_key UNIQUE (id_status_validacao);


--
-- TOC entry 4004 (class 2606 OID 74006)
-- Name: dim_status_postagem dim_status_postagem_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dim_status_postagem
    ADD CONSTRAINT dim_status_postagem_pkey PRIMARY KEY (sk_status_postagem);


--
-- TOC entry 3980 (class 2606 OID 73944)
-- Name: dim_tempo dim_tempo_data_completa_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dim_tempo
    ADD CONSTRAINT dim_tempo_data_completa_key UNIQUE (data_completa);


--
-- TOC entry 3982 (class 2606 OID 73942)
-- Name: dim_tempo dim_tempo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dim_tempo
    ADD CONSTRAINT dim_tempo_pkey PRIMARY KEY (sk_tempo);


--
-- TOC entry 3984 (class 2606 OID 73957)
-- Name: dim_usuario dim_usuario_id_usuario_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dim_usuario
    ADD CONSTRAINT dim_usuario_id_usuario_key UNIQUE (id_usuario);


--
-- TOC entry 3986 (class 2606 OID 73955)
-- Name: dim_usuario dim_usuario_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dim_usuario
    ADD CONSTRAINT dim_usuario_pkey PRIMARY KEY (sk_usuario);


--
-- TOC entry 4018 (class 2606 OID 74093)
-- Name: fato_coletas fato_coletas_id_visita_coleta_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fato_coletas
    ADD CONSTRAINT fato_coletas_id_visita_coleta_key UNIQUE (id_visita_coleta);


--
-- TOC entry 4020 (class 2606 OID 74091)
-- Name: fato_coletas fato_coletas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fato_coletas
    ADD CONSTRAINT fato_coletas_pkey PRIMARY KEY (sk_fato_coleta);


--
-- TOC entry 4010 (class 2606 OID 74043)
-- Name: fato_postagens fato_postagens_id_postagem_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fato_postagens
    ADD CONSTRAINT fato_postagens_id_postagem_key UNIQUE (id_postagem);


--
-- TOC entry 4012 (class 2606 OID 74041)
-- Name: fato_postagens fato_postagens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fato_postagens
    ADD CONSTRAINT fato_postagens_pkey PRIMARY KEY (sk_fato_postagem);


--
-- TOC entry 3903 (class 2606 OID 25247)
-- Name: tb_perguntas_quiz perguntas_quiz_index_12; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_perguntas_quiz
    ADD CONSTRAINT perguntas_quiz_index_12 UNIQUE (quiz_id, ordem);


--
-- TOC entry 3962 (class 2606 OID 25738)
-- Name: tb_migracao_ids_map pk_migracao_ids_map; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_migracao_ids_map
    ADD CONSTRAINT pk_migracao_ids_map PRIMARY KEY (entidade, legacy_id);


--
-- TOC entry 3857 (class 2606 OID 24913)
-- Name: tb_rel_pontos_coletas_categorias pontos_coletas_categorias_index_2; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_pontos_coletas_categorias
    ADD CONSTRAINT pontos_coletas_categorias_index_2 UNIQUE (ponto_coleta_id, categoria_residuo_id);


--
-- TOC entry 3889 (class 2606 OID 25161)
-- Name: tb_rel_recorrencias_agendamentos recorrencias_agendamentos_index_3; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_recorrencias_agendamentos
    ADD CONSTRAINT recorrencias_agendamentos_index_3 UNIQUE (agendamento_coleta_id, dia_semana_id);


--
-- TOC entry 3915 (class 2606 OID 25317)
-- Name: tb_rel_respostas_tentativas_quiz respostas_tentativas_quiz_index_13; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_respostas_tentativas_quiz
    ADD CONSTRAINT respostas_tentativas_quiz_index_13 UNIQUE (tentativa_id, pergunta_id);


--
-- TOC entry 3880 (class 2606 OID 25078)
-- Name: tb_agendamentos_coletas tb_agendamentos_coletas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_agendamentos_coletas
    ADD CONSTRAINT tb_agendamentos_coletas_pkey PRIMARY KEY (id_agendamento_coleta);


--
-- TOC entry 3907 (class 2606 OID 25265)
-- Name: tb_alternativas_quiz tb_alternativas_quiz_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_alternativas_quiz
    ADD CONSTRAINT tb_alternativas_quiz_pkey PRIMARY KEY (id_alternativa);


--
-- TOC entry 3955 (class 2606 OID 25690)
-- Name: tb_atividades_diarias_usuarios tb_atividades_diarias_usuarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_atividades_diarias_usuarios
    ADD CONSTRAINT tb_atividades_diarias_usuarios_pkey PRIMARY KEY (id_atividade_diaria);


--
-- TOC entry 3897 (class 2606 OID 25195)
-- Name: tb_aulas tb_aulas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_aulas
    ADD CONSTRAINT tb_aulas_pkey PRIMARY KEY (id_aula);


--
-- TOC entry 3923 (class 2606 OID 25375)
-- Name: tb_autenticacoes_api tb_autenticacoes_api_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_autenticacoes_api
    ADD CONSTRAINT tb_autenticacoes_api_pkey PRIMARY KEY (id_autenticao_api);


--
-- TOC entry 3885 (class 2606 OID 25138)
-- Name: tb_avaliacoes_visitas_coletas tb_avaliacoes_visitas_coletas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_avaliacoes_visitas_coletas
    ADD CONSTRAINT tb_avaliacoes_visitas_coletas_pkey PRIMARY KEY (id_avaliacao);


--
-- TOC entry 3829 (class 2606 OID 24773)
-- Name: tb_condominios tb_condominios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_condominios
    ADD CONSTRAINT tb_condominios_pkey PRIMARY KEY (id_condominio);


--
-- TOC entry 3966 (class 2606 OID 25755)
-- Name: tb_conversas tb_conversas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_conversas
    ADD CONSTRAINT tb_conversas_pkey PRIMARY KEY (id_conversa);


--
-- TOC entry 3845 (class 2606 OID 24847)
-- Name: tb_cooperativas tb_cooperativas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_cooperativas
    ADD CONSTRAINT tb_cooperativas_pkey PRIMARY KEY (id_cooperativa);


--
-- TOC entry 3893 (class 2606 OID 25183)
-- Name: tb_cursos tb_cursos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_cursos
    ADD CONSTRAINT tb_cursos_pkey PRIMARY KEY (id_curso);


--
-- TOC entry 3815 (class 2606 OID 24681)
-- Name: tb_enderecos tb_enderecos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_enderecos
    ADD CONSTRAINT tb_enderecos_pkey PRIMARY KEY (id_endereco);


--
-- TOC entry 3976 (class 2606 OID 57353)
-- Name: tb_recuperacoes_senha tb_esqueci_senha_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_recuperacoes_senha
    ADD CONSTRAINT tb_esqueci_senha_pkey PRIMARY KEY (id_esqueci_senha);


--
-- TOC entry 3801 (class 2606 OID 24624)
-- Name: tb_lkp_categorias_residuos tb_lkp_categorias_residuos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_lkp_categorias_residuos
    ADD CONSTRAINT tb_lkp_categorias_residuos_pkey PRIMARY KEY (id_categoria);


--
-- TOC entry 3799 (class 2606 OID 24610)
-- Name: tb_lkp_dias_semanas tb_lkp_dias_semanas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_lkp_dias_semanas
    ADD CONSTRAINT tb_lkp_dias_semanas_pkey PRIMARY KEY (id_dia_semana);


--
-- TOC entry 3809 (class 2606 OID 24660)
-- Name: tb_lkp_motivos_denuncia tb_lkp_motivos_denuncia_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_lkp_motivos_denuncia
    ADD CONSTRAINT tb_lkp_motivos_denuncia_pkey PRIMARY KEY (id_motivo_denuncia);


--
-- TOC entry 3803 (class 2606 OID 24634)
-- Name: tb_lkp_niveis_confianca tb_lkp_niveis_confianca_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_lkp_niveis_confianca
    ADD CONSTRAINT tb_lkp_niveis_confianca_pkey PRIMARY KEY (id_nivel_confianca);


--
-- TOC entry 3795 (class 2606 OID 24600)
-- Name: tb_lkp_status_agendamentos tb_lkp_status_agendamentos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_lkp_status_agendamentos
    ADD CONSTRAINT tb_lkp_status_agendamentos_pkey PRIMARY KEY (id_status);


--
-- TOC entry 3805 (class 2606 OID 24642)
-- Name: tb_lkp_status_validacoes_postagens tb_lkp_status_validacoes_postagens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_lkp_status_validacoes_postagens
    ADD CONSTRAINT tb_lkp_status_validacoes_postagens_pkey PRIMARY KEY (id_status_validacao);


--
-- TOC entry 3793 (class 2606 OID 24592)
-- Name: tb_lkp_tipos_condominios tb_lkp_tipos_condominios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_lkp_tipos_condominios
    ADD CONSTRAINT tb_lkp_tipos_condominios_pkey PRIMARY KEY (id_tipo_condominio);


--
-- TOC entry 3811 (class 2606 OID 24667)
-- Name: tb_lkp_tipos_eventos_auditados tb_lkp_tipos_eventos_auditados_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_lkp_tipos_eventos_auditados
    ADD CONSTRAINT tb_lkp_tipos_eventos_auditados_pkey PRIMARY KEY (id_tipo_evento_auditado);


--
-- TOC entry 3813 (class 2606 OID 24674)
-- Name: tb_lkp_tipos_operacoes_auditoria tb_lkp_tipos_operacoes_auditoria_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_lkp_tipos_operacoes_auditoria
    ADD CONSTRAINT tb_lkp_tipos_operacoes_auditoria_pkey PRIMARY KEY (id_tipo_operacao);


--
-- TOC entry 3791 (class 2606 OID 24584)
-- Name: tb_lkp_tipos_usuarios tb_lkp_tipos_usuarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_lkp_tipos_usuarios
    ADD CONSTRAINT tb_lkp_tipos_usuarios_pkey PRIMARY KEY (id_tipo_usuario);


--
-- TOC entry 3807 (class 2606 OID 24650)
-- Name: tb_lkp_tipos_votos_postagens tb_lkp_tipos_votos_postagens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_lkp_tipos_votos_postagens
    ADD CONSTRAINT tb_lkp_tipos_votos_postagens_pkey PRIMARY KEY (id_tipo_voto);


--
-- TOC entry 3946 (class 2606 OID 25522)
-- Name: tb_log_auditoria_agendamentos_coletas tb_log_auditoria_agendamentos_coletas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_log_auditoria_agendamentos_coletas
    ADD CONSTRAINT tb_log_auditoria_agendamentos_coletas_pkey PRIMARY KEY (id_auditoria_agendamento);


--
-- TOC entry 3940 (class 2606 OID 25472)
-- Name: tb_log_auditoria tb_log_auditoria_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_log_auditoria
    ADD CONSTRAINT tb_log_auditoria_pkey PRIMARY KEY (id_auditoria);


--
-- TOC entry 3942 (class 2606 OID 25491)
-- Name: tb_log_auditoria_postagens tb_log_auditoria_postagens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_log_auditoria_postagens
    ADD CONSTRAINT tb_log_auditoria_postagens_pkey PRIMARY KEY (id_auditoria_postagem);


--
-- TOC entry 3950 (class 2606 OID 25553)
-- Name: tb_log_auditoria_usuarios_condominios tb_log_auditoria_usuarios_condominios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_log_auditoria_usuarios_condominios
    ADD CONSTRAINT tb_log_auditoria_usuarios_condominios_pkey PRIMARY KEY (id_auditoria_usuario_condominio);


--
-- TOC entry 3972 (class 2606 OID 25785)
-- Name: tb_mensagens_conversas tb_mensagens_conversas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_mensagens_conversas
    ADD CONSTRAINT tb_mensagens_conversas_pkey PRIMARY KEY (id_mensagem);


--
-- TOC entry 3959 (class 2606 OID 25715)
-- Name: tb_metricas_dau tb_metricas_dau_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_metricas_dau
    ADD CONSTRAINT tb_metricas_dau_pkey PRIMARY KEY (data_referencia);


--
-- TOC entry 3841 (class 2606 OID 24817)
-- Name: tb_moradores tb_moradores_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_moradores
    ADD CONSTRAINT tb_moradores_pkey PRIMARY KEY (id_morador);


--
-- TOC entry 3933 (class 2606 OID 25409)
-- Name: tb_movimentacoes_pontos tb_movimentacoes_pontos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_movimentacoes_pontos
    ADD CONSTRAINT tb_movimentacoes_pontos_pkey PRIMARY KEY (id_movimentacao);


--
-- TOC entry 3827 (class 2606 OID 24755)
-- Name: tb_notificacoes tb_notificacoes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_notificacoes
    ADD CONSTRAINT tb_notificacoes_pkey PRIMARY KEY (id_notificacao);


--
-- TOC entry 3905 (class 2606 OID 25245)
-- Name: tb_perguntas_quiz tb_perguntas_quiz_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_perguntas_quiz
    ADD CONSTRAINT tb_perguntas_quiz_pkey PRIMARY KEY (id_pergunta);


--
-- TOC entry 3851 (class 2606 OID 24871)
-- Name: tb_pontos_coletas tb_pontos_coletas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_pontos_coletas
    ADD CONSTRAINT tb_pontos_coletas_pkey PRIMARY KEY (id_ponto_coleta);


--
-- TOC entry 3869 (class 2606 OID 24997)
-- Name: tb_postagens tb_postagens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_postagens
    ADD CONSTRAINT tb_postagens_pkey PRIMARY KEY (id_postagem);


--
-- TOC entry 3899 (class 2606 OID 25221)
-- Name: tb_quizzes tb_quizzes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_quizzes
    ADD CONSTRAINT tb_quizzes_pkey PRIMARY KEY (id_quiz);


--
-- TOC entry 3855 (class 2606 OID 24890)
-- Name: tb_rel_cooperativas_categorias_materiais tb_rel_cooperativas_categorias_materiais_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_cooperativas_categorias_materiais
    ADD CONSTRAINT tb_rel_cooperativas_categorias_materiais_pkey PRIMARY KEY (id_cooperativa_categoria);


--
-- TOC entry 3859 (class 2606 OID 24911)
-- Name: tb_rel_pontos_coletas_categorias tb_rel_pontos_coletas_categorias_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_pontos_coletas_categorias
    ADD CONSTRAINT tb_rel_pontos_coletas_categorias_pkey PRIMARY KEY (id_ponto_coleta_categoria);


--
-- TOC entry 3891 (class 2606 OID 25159)
-- Name: tb_rel_recorrencias_agendamentos tb_rel_recorrencias_agendamentos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_recorrencias_agendamentos
    ADD CONSTRAINT tb_rel_recorrencias_agendamentos_pkey PRIMARY KEY (id_recorrencia_agendamento);


--
-- TOC entry 3917 (class 2606 OID 25315)
-- Name: tb_rel_respostas_tentativas_quiz tb_rel_respostas_tentativas_quiz_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_respostas_tentativas_quiz
    ADD CONSTRAINT tb_rel_respostas_tentativas_quiz_pkey PRIMARY KEY (id_resposta);


--
-- TOC entry 3861 (class 2606 OID 24946)
-- Name: tb_rel_usuarios_condominios tb_rel_usuarios_condominios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_usuarios_condominios
    ADD CONSTRAINT tb_rel_usuarios_condominios_pkey PRIMARY KEY (id_usuario_condominio);


--
-- TOC entry 3919 (class 2606 OID 25345)
-- Name: tb_rel_usuarios_cursos tb_rel_usuarios_cursos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_usuarios_cursos
    ADD CONSTRAINT tb_rel_usuarios_cursos_pkey PRIMARY KEY (id_usuario_curso);


--
-- TOC entry 3874 (class 2606 OID 25038)
-- Name: tb_rel_votos_postagens tb_rel_votos_postagens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_votos_postagens
    ADD CONSTRAINT tb_rel_votos_postagens_pkey PRIMARY KEY (id_voto);


--
-- TOC entry 3821 (class 2606 OID 24719)
-- Name: tb_sindicos tb_sindicos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_sindicos
    ADD CONSTRAINT tb_sindicos_pkey PRIMARY KEY (id_sindico);


--
-- TOC entry 3825 (class 2606 OID 24737)
-- Name: tb_telefones tb_telefones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_telefones
    ADD CONSTRAINT tb_telefones_pkey PRIMARY KEY (id_telefone);


--
-- TOC entry 3913 (class 2606 OID 25284)
-- Name: tb_tentativas_quiz tb_tentativas_quiz_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_tentativas_quiz
    ADD CONSTRAINT tb_tentativas_quiz_pkey PRIMARY KEY (id_tentativa);


--
-- TOC entry 3833 (class 2606 OID 24799)
-- Name: tb_torres tb_torres_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_torres
    ADD CONSTRAINT tb_torres_pkey PRIMARY KEY (id_torre);


--
-- TOC entry 3817 (class 2606 OID 24699)
-- Name: tb_usuarios tb_usuarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_usuarios
    ADD CONSTRAINT tb_usuarios_pkey PRIMARY KEY (id_usuario);


--
-- TOC entry 3883 (class 2606 OID 25118)
-- Name: tb_visitas_coletas tb_visitas_coletas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_visitas_coletas
    ADD CONSTRAINT tb_visitas_coletas_pkey PRIMARY KEY (id_visita_coleta);


--
-- TOC entry 3835 (class 2606 OID 24801)
-- Name: tb_torres torres_index_0; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_torres
    ADD CONSTRAINT torres_index_0 UNIQUE (condominio_id, nome_torre);


--
-- TOC entry 3957 (class 2606 OID 25692)
-- Name: tb_atividades_diarias_usuarios uq_atividades_diarias_usuario_data; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_atividades_diarias_usuarios
    ADD CONSTRAINT uq_atividades_diarias_usuario_data UNIQUE (usuario_id, data_atividade);


--
-- TOC entry 3948 (class 2606 OID 25524)
-- Name: tb_log_auditoria_agendamentos_coletas uq_auditoria_agendamentos_coletas_auditoria_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_log_auditoria_agendamentos_coletas
    ADD CONSTRAINT uq_auditoria_agendamentos_coletas_auditoria_id UNIQUE (auditoria_id);


--
-- TOC entry 3944 (class 2606 OID 25493)
-- Name: tb_log_auditoria_postagens uq_auditoria_postagens_auditoria_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_log_auditoria_postagens
    ADD CONSTRAINT uq_auditoria_postagens_auditoria_id UNIQUE (auditoria_id);


--
-- TOC entry 3952 (class 2606 OID 25555)
-- Name: tb_log_auditoria_usuarios_condominios uq_auditoria_usuarios_condominios_auditoria_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_log_auditoria_usuarios_condominios
    ADD CONSTRAINT uq_auditoria_usuarios_condominios_auditoria_id UNIQUE (auditoria_id);


--
-- TOC entry 3925 (class 2606 OID 25377)
-- Name: tb_autenticacoes_api uq_autenticao_api_token; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_autenticacoes_api
    ADD CONSTRAINT uq_autenticao_api_token UNIQUE (token);


--
-- TOC entry 3887 (class 2606 OID 25140)
-- Name: tb_avaliacoes_visitas_coletas uq_avaliacoes_visitas_coletas_visita_coleta_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_avaliacoes_visitas_coletas
    ADD CONSTRAINT uq_avaliacoes_visitas_coletas_visita_coleta_id UNIQUE (visita_coleta_id);


--
-- TOC entry 3831 (class 2606 OID 24775)
-- Name: tb_condominios uq_condominios_endereco_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_condominios
    ADD CONSTRAINT uq_condominios_endereco_id UNIQUE (endereco_id);


--
-- TOC entry 3968 (class 2606 OID 25757)
-- Name: tb_conversas uq_conversas_condominio_cooperativa; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_conversas
    ADD CONSTRAINT uq_conversas_condominio_cooperativa UNIQUE (condominio_id, cooperativa_id);


--
-- TOC entry 3847 (class 2606 OID 24851)
-- Name: tb_cooperativas uq_cooperativas_endereco_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_cooperativas
    ADD CONSTRAINT uq_cooperativas_endereco_id UNIQUE (endereco_id);


--
-- TOC entry 3849 (class 2606 OID 24849)
-- Name: tb_cooperativas uq_cooperativas_usuario_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_cooperativas
    ADD CONSTRAINT uq_cooperativas_usuario_id UNIQUE (usuario_id);


--
-- TOC entry 3978 (class 2606 OID 57355)
-- Name: tb_recuperacoes_senha uq_esqueci_senha_token_hash; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_recuperacoes_senha
    ADD CONSTRAINT uq_esqueci_senha_token_hash UNIQUE (token_hash);


--
-- TOC entry 3797 (class 2606 OID 24602)
-- Name: tb_lkp_status_agendamentos uq_lkp_status_agendamentos_nome_status; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_lkp_status_agendamentos
    ADD CONSTRAINT uq_lkp_status_agendamentos_nome_status UNIQUE (nome_status);


--
-- TOC entry 3843 (class 2606 OID 24819)
-- Name: tb_moradores uq_moradores_usuario_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_moradores
    ADD CONSTRAINT uq_moradores_usuario_id UNIQUE (usuario_id);


--
-- TOC entry 3937 (class 2606 OID 25411)
-- Name: tb_movimentacoes_pontos uq_mov_pontos_idempotency_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_movimentacoes_pontos
    ADD CONSTRAINT uq_mov_pontos_idempotency_key UNIQUE (idempotency_key);


--
-- TOC entry 3871 (class 2606 OID 24999)
-- Name: tb_postagens uq_postagens_hash_foto; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_postagens
    ADD CONSTRAINT uq_postagens_hash_foto UNIQUE (hash_foto);


--
-- TOC entry 3901 (class 2606 OID 25223)
-- Name: tb_quizzes uq_quizzes_aula_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_quizzes
    ADD CONSTRAINT uq_quizzes_aula_id UNIQUE (aula_id);


--
-- TOC entry 3823 (class 2606 OID 24721)
-- Name: tb_sindicos uq_sindicos_usuario_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_sindicos
    ADD CONSTRAINT uq_sindicos_usuario_id UNIQUE (usuario_id);


--
-- TOC entry 3837 (class 2606 OID 24803)
-- Name: tb_torres uq_torres_id_condominio; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_torres
    ADD CONSTRAINT uq_torres_id_condominio UNIQUE (id_torre, condominio_id);


--
-- TOC entry 3819 (class 2606 OID 24701)
-- Name: tb_usuarios uq_usuarios_cpf; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_usuarios
    ADD CONSTRAINT uq_usuarios_cpf UNIQUE (cpf);


--
-- TOC entry 3863 (class 2606 OID 24948)
-- Name: tb_rel_usuarios_condominios usuarios_condominios_index_5; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_usuarios_condominios
    ADD CONSTRAINT usuarios_condominios_index_5 UNIQUE (usuario_id, condominio_id);


--
-- TOC entry 3921 (class 2606 OID 25347)
-- Name: tb_rel_usuarios_cursos usuarios_cursos_index_6; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_usuarios_cursos
    ADD CONSTRAINT usuarios_cursos_index_6 UNIQUE (usuario_id, aula_id);


--
-- TOC entry 3876 (class 2606 OID 25040)
-- Name: tb_rel_votos_postagens votos_postagens_index_7; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_votos_postagens
    ADD CONSTRAINT votos_postagens_index_7 UNIQUE (postagem_id, usuario_id);


--
-- TOC entry 3877 (class 1259 OID 25582)
-- Name: idx_agendamentos_coletas_condominio_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_agendamentos_coletas_condominio_id ON public.tb_agendamentos_coletas USING btree (condominio_id);


--
-- TOC entry 3878 (class 1259 OID 25583)
-- Name: idx_agendamentos_coletas_cooperativa_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_agendamentos_coletas_cooperativa_id ON public.tb_agendamentos_coletas USING btree (cooperativa_id);


--
-- TOC entry 3953 (class 1259 OID 25698)
-- Name: idx_atividades_diarias_data; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_atividades_diarias_data ON public.tb_atividades_diarias_usuarios USING btree (data_atividade, usuario_id);


--
-- TOC entry 3938 (class 1259 OID 25580)
-- Name: idx_auditoria_log_executado_em; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auditoria_log_executado_em ON public.tb_log_auditoria USING btree (executado_em DESC);


--
-- TOC entry 3963 (class 1259 OID 25799)
-- Name: idx_conversas_condominio_ultima; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_conversas_condominio_ultima ON public.tb_conversas USING btree (condominio_id, ultima_mensagem_em DESC);


--
-- TOC entry 3964 (class 1259 OID 25798)
-- Name: idx_conversas_cooperativa_ultima; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_conversas_cooperativa_ultima ON public.tb_conversas USING btree (cooperativa_id, ultima_mensagem_em DESC);


--
-- TOC entry 3973 (class 1259 OID 57361)
-- Name: idx_esqueci_senha_usuario_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_esqueci_senha_usuario_id ON public.tb_recuperacoes_senha USING btree (usuario_id);


--
-- TOC entry 3974 (class 1259 OID 65728)
-- Name: idx_esqueci_senhas_usuario_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_esqueci_senhas_usuario_id ON public.tb_recuperacoes_senha USING btree (usuario_id);


--
-- TOC entry 4021 (class 1259 OID 74115)
-- Name: idx_fato_coletas_condominio; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fato_coletas_condominio ON public.fato_coletas USING btree (sk_condominio);


--
-- TOC entry 4022 (class 1259 OID 74116)
-- Name: idx_fato_coletas_cooperativa; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fato_coletas_cooperativa ON public.fato_coletas USING btree (sk_cooperativa);


--
-- TOC entry 4023 (class 1259 OID 74114)
-- Name: idx_fato_coletas_tempo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fato_coletas_tempo ON public.fato_coletas USING btree (sk_tempo);


--
-- TOC entry 4013 (class 1259 OID 74072)
-- Name: idx_fato_postagens_categoria; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fato_postagens_categoria ON public.fato_postagens USING btree (sk_categoria);


--
-- TOC entry 4014 (class 1259 OID 74071)
-- Name: idx_fato_postagens_condominio; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fato_postagens_condominio ON public.fato_postagens USING btree (sk_condominio);


--
-- TOC entry 4015 (class 1259 OID 74069)
-- Name: idx_fato_postagens_tempo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fato_postagens_tempo ON public.fato_postagens USING btree (sk_tempo);


--
-- TOC entry 4016 (class 1259 OID 74070)
-- Name: idx_fato_postagens_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fato_postagens_usuario ON public.fato_postagens USING btree (sk_usuario);


--
-- TOC entry 3969 (class 1259 OID 25796)
-- Name: idx_mensagens_conversas_conversa_enviada; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mensagens_conversas_conversa_enviada ON public.tb_mensagens_conversas USING btree (conversa_id, enviada_em DESC);


--
-- TOC entry 3970 (class 1259 OID 25797)
-- Name: idx_mensagens_conversas_nao_lidas; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mensagens_conversas_nao_lidas ON public.tb_mensagens_conversas USING btree (conversa_id, remetente_tipo) WHERE (lida_em IS NULL);


--
-- TOC entry 3960 (class 1259 OID 25739)
-- Name: idx_migracao_ids_map_entidade_novo_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_migracao_ids_map_entidade_novo_id ON public.tb_migracao_ids_map USING btree (entidade, novo_id);


--
-- TOC entry 3838 (class 1259 OID 25587)
-- Name: idx_moradores_condominio_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_moradores_condominio_id ON public.tb_moradores USING btree (condominio_id);


--
-- TOC entry 3839 (class 1259 OID 25588)
-- Name: idx_moradores_torre_condominio; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_moradores_torre_condominio ON public.tb_moradores USING btree (torre_id, condominio_id) WHERE (torre_id IS NOT NULL);


--
-- TOC entry 3926 (class 1259 OID 25450)
-- Name: idx_mov_pontos_condominio_ocorrido; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mov_pontos_condominio_ocorrido ON public.tb_movimentacoes_pontos USING btree (condominio_id, ocorrido_em);


--
-- TOC entry 3927 (class 1259 OID 25451)
-- Name: idx_mov_pontos_postagem; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mov_pontos_postagem ON public.tb_movimentacoes_pontos USING btree (postagem_id);


--
-- TOC entry 3928 (class 1259 OID 25454)
-- Name: idx_mov_pontos_postagem_tipo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mov_pontos_postagem_tipo ON public.tb_movimentacoes_pontos USING btree (postagem_id, tipo_movimentacao);


--
-- TOC entry 3929 (class 1259 OID 25453)
-- Name: idx_mov_pontos_redis_pendente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mov_pontos_redis_pendente ON public.tb_movimentacoes_pontos USING btree (redis_sincronizado, ocorrido_em);


--
-- TOC entry 3930 (class 1259 OID 25452)
-- Name: idx_mov_pontos_tentativa_quiz; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mov_pontos_tentativa_quiz ON public.tb_movimentacoes_pontos USING btree (tentativa_quiz_id);


--
-- TOC entry 3931 (class 1259 OID 25449)
-- Name: idx_mov_pontos_usuario_ocorrido; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mov_pontos_usuario_ocorrido ON public.tb_movimentacoes_pontos USING btree (usuario_id, ocorrido_em);


--
-- TOC entry 3864 (class 1259 OID 25581)
-- Name: idx_postagens_condominio_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_postagens_condominio_status ON public.tb_postagens USING btree (condominio_id, status_validacao_id);


--
-- TOC entry 3865 (class 1259 OID 25455)
-- Name: idx_postagens_janela_validacao; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_postagens_janela_validacao ON public.tb_postagens USING btree (data_limite_analise, resolvido_em);


--
-- TOC entry 3866 (class 1259 OID 25578)
-- Name: idx_postagens_pendentes_moderacao; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_postagens_pendentes_moderacao ON public.tb_postagens USING btree (condominio_id, data_limite_analise) WHERE (resolvido_em IS NULL);


--
-- TOC entry 3867 (class 1259 OID 25456)
-- Name: idx_postagens_usuario_categoria_data; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_postagens_usuario_categoria_data ON public.tb_postagens USING btree (usuario_id, categoria_id, data_postagem);


--
-- TOC entry 3908 (class 1259 OID 25802)
-- Name: idx_tentativas_quiz_condominio_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tentativas_quiz_condominio_id ON public.tb_tentativas_quiz USING btree (condominio_id);


--
-- TOC entry 3909 (class 1259 OID 25586)
-- Name: idx_tentativas_quiz_quiz_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tentativas_quiz_quiz_id ON public.tb_tentativas_quiz USING btree (quiz_id);


--
-- TOC entry 3910 (class 1259 OID 25585)
-- Name: idx_tentativas_quiz_usuario_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tentativas_quiz_usuario_id ON public.tb_tentativas_quiz USING btree (usuario_id);


--
-- TOC entry 3911 (class 1259 OID 25457)
-- Name: idx_tentativas_quiz_usuario_quiz_aprovado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tentativas_quiz_usuario_quiz_aprovado ON public.tb_tentativas_quiz USING btree (usuario_id, quiz_id, aprovado);


--
-- TOC entry 3881 (class 1259 OID 25584)
-- Name: idx_visitas_coletas_agendamento_coleta_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_visitas_coletas_agendamento_coleta_id ON public.tb_visitas_coletas USING btree (agendamento_coleta_id);


--
-- TOC entry 3872 (class 1259 OID 25579)
-- Name: idx_votos_postagens_usuario_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_votos_postagens_usuario_id ON public.tb_rel_votos_postagens USING btree (usuario_id);


--
-- TOC entry 3934 (class 1259 OID 25447)
-- Name: uq_mov_pontos_credito_postagem; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_mov_pontos_credito_postagem ON public.tb_movimentacoes_pontos USING btree (postagem_id) WHERE (((origem_tipo)::text = 'POSTAGEM'::text) AND ((tipo_movimentacao)::text = 'CREDITO'::text));


--
-- TOC entry 3935 (class 1259 OID 25448)
-- Name: uq_mov_pontos_credito_quiz; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_mov_pontos_credito_quiz ON public.tb_movimentacoes_pontos USING btree (tentativa_quiz_id) WHERE (((origem_tipo)::text = 'QUIZ'::text) AND ((tipo_movimentacao)::text = 'CREDITO'::text));


--
-- TOC entry 4118 (class 2620 OID 25575)
-- Name: tb_agendamentos_coletas trg_auditoria_agendamentos_coletas; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditoria_agendamentos_coletas AFTER INSERT OR DELETE OR UPDATE ON public.tb_agendamentos_coletas FOR EACH ROW EXECUTE FUNCTION public.fn_trg_auditoria_agendamentos_coletas();


--
-- TOC entry 4117 (class 2620 OID 25573)
-- Name: tb_postagens trg_auditoria_postagens; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditoria_postagens AFTER INSERT OR DELETE OR UPDATE ON public.tb_postagens FOR EACH ROW EXECUTE FUNCTION public.fn_trg_auditoria_postagens();


--
-- TOC entry 4116 (class 2620 OID 25577)
-- Name: tb_rel_usuarios_condominios trg_auditoria_usuarios_condominios; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_auditoria_usuarios_condominios AFTER INSERT OR DELETE OR UPDATE ON public.tb_rel_usuarios_condominios FOR EACH ROW EXECUTE FUNCTION public.fn_trg_auditoria_usuarios_condominios();


--
-- TOC entry 4121 (class 2620 OID 25801)
-- Name: tb_mensagens_conversas trg_validar_mensagem_conversa; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_validar_mensagem_conversa BEFORE INSERT ON public.tb_mensagens_conversas FOR EACH ROW EXECUTE FUNCTION public.fn_trg_validar_mensagem_conversa();


--
-- TOC entry 4119 (class 2620 OID 25742)
-- Name: tb_agendamentos_coletas trg_validar_partes_agendamento; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_validar_partes_agendamento BEFORE INSERT OR UPDATE OF solicitado_por_usuario_id, origem_solicitacao, respondido_por_usuario_id, condominio_id, cooperativa_id ON public.tb_agendamentos_coletas FOR EACH ROW EXECUTE FUNCTION public.fn_trg_validar_partes_agendamento();


--
-- TOC entry 4120 (class 2620 OID 25459)
-- Name: tb_movimentacoes_pontos trg_validar_referencia_movimentacao_pontos; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_validar_referencia_movimentacao_pontos BEFORE INSERT OR UPDATE OF movimentacao_referencia_id, tipo_movimentacao, pontos, usuario_id, origem_tipo, postagem_id, tentativa_quiz_id ON public.tb_movimentacoes_pontos FOR EACH ROW EXECUTE FUNCTION public.fn_validar_referencia_movimentacao_pontos();


--
-- TOC entry 4112 (class 2606 OID 74099)
-- Name: fato_coletas fato_coletas_sk_condominio_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fato_coletas
    ADD CONSTRAINT fato_coletas_sk_condominio_fkey FOREIGN KEY (sk_condominio) REFERENCES public.dim_condominio(sk_condominio);


--
-- TOC entry 4113 (class 2606 OID 74104)
-- Name: fato_coletas fato_coletas_sk_cooperativa_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fato_coletas
    ADD CONSTRAINT fato_coletas_sk_cooperativa_fkey FOREIGN KEY (sk_cooperativa) REFERENCES public.dim_cooperativa(sk_cooperativa);


--
-- TOC entry 4114 (class 2606 OID 74109)
-- Name: fato_coletas fato_coletas_sk_status_agendamento_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fato_coletas
    ADD CONSTRAINT fato_coletas_sk_status_agendamento_fkey FOREIGN KEY (sk_status_agendamento) REFERENCES public.dim_status_agendamento(sk_status_agendamento);


--
-- TOC entry 4115 (class 2606 OID 74094)
-- Name: fato_coletas fato_coletas_sk_tempo_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fato_coletas
    ADD CONSTRAINT fato_coletas_sk_tempo_fkey FOREIGN KEY (sk_tempo) REFERENCES public.dim_tempo(sk_tempo);


--
-- TOC entry 4107 (class 2606 OID 74059)
-- Name: fato_postagens fato_postagens_sk_categoria_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fato_postagens
    ADD CONSTRAINT fato_postagens_sk_categoria_fkey FOREIGN KEY (sk_categoria) REFERENCES public.dim_categoria_residuo(sk_categoria);


--
-- TOC entry 4108 (class 2606 OID 74054)
-- Name: fato_postagens fato_postagens_sk_condominio_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fato_postagens
    ADD CONSTRAINT fato_postagens_sk_condominio_fkey FOREIGN KEY (sk_condominio) REFERENCES public.dim_condominio(sk_condominio);


--
-- TOC entry 4109 (class 2606 OID 74064)
-- Name: fato_postagens fato_postagens_sk_status_postagem_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fato_postagens
    ADD CONSTRAINT fato_postagens_sk_status_postagem_fkey FOREIGN KEY (sk_status_postagem) REFERENCES public.dim_status_postagem(sk_status_postagem);


--
-- TOC entry 4110 (class 2606 OID 74044)
-- Name: fato_postagens fato_postagens_sk_tempo_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fato_postagens
    ADD CONSTRAINT fato_postagens_sk_tempo_fkey FOREIGN KEY (sk_tempo) REFERENCES public.dim_tempo(sk_tempo);


--
-- TOC entry 4111 (class 2606 OID 74049)
-- Name: fato_postagens fato_postagens_sk_usuario_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fato_postagens
    ADD CONSTRAINT fato_postagens_sk_usuario_fkey FOREIGN KEY (sk_usuario) REFERENCES public.dim_usuario(sk_usuario);


--
-- TOC entry 4057 (class 2606 OID 25089)
-- Name: tb_agendamentos_coletas fk_agendamentos_coletas_condominio_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_agendamentos_coletas
    ADD CONSTRAINT fk_agendamentos_coletas_condominio_id FOREIGN KEY (condominio_id) REFERENCES public.tb_condominios(id_condominio);


--
-- TOC entry 4058 (class 2606 OID 25094)
-- Name: tb_agendamentos_coletas fk_agendamentos_coletas_cooperativa_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_agendamentos_coletas
    ADD CONSTRAINT fk_agendamentos_coletas_cooperativa_id FOREIGN KEY (cooperativa_id) REFERENCES public.tb_cooperativas(id_cooperativa);


--
-- TOC entry 4059 (class 2606 OID 25084)
-- Name: tb_agendamentos_coletas fk_agendamentos_coletas_respondido_por_usuario_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_agendamentos_coletas
    ADD CONSTRAINT fk_agendamentos_coletas_respondido_por_usuario_id FOREIGN KEY (respondido_por_usuario_id) REFERENCES public.tb_usuarios(id_usuario);


--
-- TOC entry 4060 (class 2606 OID 25079)
-- Name: tb_agendamentos_coletas fk_agendamentos_coletas_solicitado_por_usuario_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_agendamentos_coletas
    ADD CONSTRAINT fk_agendamentos_coletas_solicitado_por_usuario_id FOREIGN KEY (solicitado_por_usuario_id) REFERENCES public.tb_usuarios(id_usuario);


--
-- TOC entry 4061 (class 2606 OID 25099)
-- Name: tb_agendamentos_coletas fk_agendamentos_coletas_status_agendamento_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_agendamentos_coletas
    ADD CONSTRAINT fk_agendamentos_coletas_status_agendamento_id FOREIGN KEY (status_agendamento_id) REFERENCES public.tb_lkp_status_agendamentos(id_status);


--
-- TOC entry 4071 (class 2606 OID 25266)
-- Name: tb_alternativas_quiz fk_alternativas_quiz_pergunta_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_alternativas_quiz
    ADD CONSTRAINT fk_alternativas_quiz_pergunta_id FOREIGN KEY (pergunta_id) REFERENCES public.tb_perguntas_quiz(id_pergunta) ON UPDATE CASCADE;


--
-- TOC entry 4101 (class 2606 OID 25693)
-- Name: tb_atividades_diarias_usuarios fk_atividades_diarias_usuario_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_atividades_diarias_usuarios
    ADD CONSTRAINT fk_atividades_diarias_usuario_id FOREIGN KEY (usuario_id) REFERENCES public.tb_usuarios(id_usuario);


--
-- TOC entry 4095 (class 2606 OID 25530)
-- Name: tb_log_auditoria_agendamentos_coletas fk_auditoria_agendamentos_coletas_agendamento_coleta_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_log_auditoria_agendamentos_coletas
    ADD CONSTRAINT fk_auditoria_agendamentos_coletas_agendamento_coleta_id FOREIGN KEY (agendamento_coleta_id) REFERENCES public.tb_agendamentos_coletas(id_agendamento_coleta);


--
-- TOC entry 4096 (class 2606 OID 25525)
-- Name: tb_log_auditoria_agendamentos_coletas fk_auditoria_agendamentos_coletas_auditoria_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_log_auditoria_agendamentos_coletas
    ADD CONSTRAINT fk_auditoria_agendamentos_coletas_auditoria_id FOREIGN KEY (auditoria_id) REFERENCES public.tb_log_auditoria(id_auditoria);


--
-- TOC entry 4097 (class 2606 OID 25535)
-- Name: tb_log_auditoria_agendamentos_coletas fk_auditoria_agendamentos_coletas_status_anterior_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_log_auditoria_agendamentos_coletas
    ADD CONSTRAINT fk_auditoria_agendamentos_coletas_status_anterior_id FOREIGN KEY (status_anterior_id) REFERENCES public.tb_lkp_status_agendamentos(id_status);


--
-- TOC entry 4098 (class 2606 OID 25540)
-- Name: tb_log_auditoria_agendamentos_coletas fk_auditoria_agendamentos_coletas_status_novo_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_log_auditoria_agendamentos_coletas
    ADD CONSTRAINT fk_auditoria_agendamentos_coletas_status_novo_id FOREIGN KEY (status_novo_id) REFERENCES public.tb_lkp_status_agendamentos(id_status);


--
-- TOC entry 4089 (class 2606 OID 25473)
-- Name: tb_log_auditoria fk_auditoria_log_tipo_evento_auditado_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_log_auditoria
    ADD CONSTRAINT fk_auditoria_log_tipo_evento_auditado_id FOREIGN KEY (tipo_evento_auditado_id) REFERENCES public.tb_lkp_tipos_eventos_auditados(id_tipo_evento_auditado);


--
-- TOC entry 4090 (class 2606 OID 25478)
-- Name: tb_log_auditoria fk_auditoria_log_tipo_operacao_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_log_auditoria
    ADD CONSTRAINT fk_auditoria_log_tipo_operacao_id FOREIGN KEY (tipo_operacao_id) REFERENCES public.tb_lkp_tipos_operacoes_auditoria(id_tipo_operacao);


--
-- TOC entry 4091 (class 2606 OID 25494)
-- Name: tb_log_auditoria_postagens fk_auditoria_postagens_auditoria_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_log_auditoria_postagens
    ADD CONSTRAINT fk_auditoria_postagens_auditoria_id FOREIGN KEY (auditoria_id) REFERENCES public.tb_log_auditoria(id_auditoria);


--
-- TOC entry 4092 (class 2606 OID 25499)
-- Name: tb_log_auditoria_postagens fk_auditoria_postagens_postagem_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_log_auditoria_postagens
    ADD CONSTRAINT fk_auditoria_postagens_postagem_id FOREIGN KEY (postagem_id) REFERENCES public.tb_postagens(id_postagem);


--
-- TOC entry 4093 (class 2606 OID 25504)
-- Name: tb_log_auditoria_postagens fk_auditoria_postagens_status_validacao_anterior_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_log_auditoria_postagens
    ADD CONSTRAINT fk_auditoria_postagens_status_validacao_anterior_id FOREIGN KEY (status_validacao_anterior_id) REFERENCES public.tb_lkp_status_validacoes_postagens(id_status_validacao);


--
-- TOC entry 4094 (class 2606 OID 25509)
-- Name: tb_log_auditoria_postagens fk_auditoria_postagens_status_validacao_novo_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_log_auditoria_postagens
    ADD CONSTRAINT fk_auditoria_postagens_status_validacao_novo_id FOREIGN KEY (status_validacao_novo_id) REFERENCES public.tb_lkp_status_validacoes_postagens(id_status_validacao);


--
-- TOC entry 4099 (class 2606 OID 25556)
-- Name: tb_log_auditoria_usuarios_condominios fk_auditoria_usuarios_condominios_auditoria_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_log_auditoria_usuarios_condominios
    ADD CONSTRAINT fk_auditoria_usuarios_condominios_auditoria_id FOREIGN KEY (auditoria_id) REFERENCES public.tb_log_auditoria(id_auditoria);


--
-- TOC entry 4100 (class 2606 OID 25561)
-- Name: tb_log_auditoria_usuarios_condominios fk_auditoria_usuarios_condominios_usuario_condominio_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_log_auditoria_usuarios_condominios
    ADD CONSTRAINT fk_auditoria_usuarios_condominios_usuario_condominio_id FOREIGN KEY (usuario_condominio_id) REFERENCES public.tb_rel_usuarios_condominios(id_usuario_condominio);


--
-- TOC entry 4067 (class 2606 OID 25198)
-- Name: tb_aulas fk_aulas_curso_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_aulas
    ADD CONSTRAINT fk_aulas_curso_id FOREIGN KEY (curso_id) REFERENCES public.tb_cursos(id_curso);


--
-- TOC entry 4081 (class 2606 OID 25378)
-- Name: tb_autenticacoes_api fk_autenticao_api_usuario_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_autenticacoes_api
    ADD CONSTRAINT fk_autenticao_api_usuario_id FOREIGN KEY (usuario_id) REFERENCES public.tb_usuarios(id_usuario);


--
-- TOC entry 4063 (class 2606 OID 25146)
-- Name: tb_avaliacoes_visitas_coletas fk_avaliacoes_visitas_coletas_usuario_avaliador_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_avaliacoes_visitas_coletas
    ADD CONSTRAINT fk_avaliacoes_visitas_coletas_usuario_avaliador_id FOREIGN KEY (usuario_avaliador_id) REFERENCES public.tb_usuarios(id_usuario);


--
-- TOC entry 4064 (class 2606 OID 25141)
-- Name: tb_avaliacoes_visitas_coletas fk_avaliacoes_visitas_coletas_visita_coleta_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_avaliacoes_visitas_coletas
    ADD CONSTRAINT fk_avaliacoes_visitas_coletas_visita_coleta_id FOREIGN KEY (visita_coleta_id) REFERENCES public.tb_visitas_coletas(id_visita_coleta);


--
-- TOC entry 4029 (class 2606 OID 24786)
-- Name: tb_condominios fk_condominios_endereco_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_condominios
    ADD CONSTRAINT fk_condominios_endereco_id FOREIGN KEY (endereco_id) REFERENCES public.tb_enderecos(id_endereco);


--
-- TOC entry 4030 (class 2606 OID 24781)
-- Name: tb_condominios fk_condominios_sindico_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_condominios
    ADD CONSTRAINT fk_condominios_sindico_id FOREIGN KEY (sindico_id) REFERENCES public.tb_sindicos(id_sindico);


--
-- TOC entry 4031 (class 2606 OID 24776)
-- Name: tb_condominios fk_condominios_tipo_condominio_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_condominios
    ADD CONSTRAINT fk_condominios_tipo_condominio_id FOREIGN KEY (tipo_condominio_id) REFERENCES public.tb_lkp_tipos_condominios(id_tipo_condominio);


--
-- TOC entry 4102 (class 2606 OID 25758)
-- Name: tb_conversas fk_conversas_condominio_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_conversas
    ADD CONSTRAINT fk_conversas_condominio_id FOREIGN KEY (condominio_id) REFERENCES public.tb_condominios(id_condominio);


--
-- TOC entry 4103 (class 2606 OID 25763)
-- Name: tb_conversas fk_conversas_cooperativa_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_conversas
    ADD CONSTRAINT fk_conversas_cooperativa_id FOREIGN KEY (cooperativa_id) REFERENCES public.tb_cooperativas(id_cooperativa);


--
-- TOC entry 4040 (class 2606 OID 24898)
-- Name: tb_rel_cooperativas_categorias_materiais fk_cooperativas_categorias_materiais_categoria_residuo_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_cooperativas_categorias_materiais
    ADD CONSTRAINT fk_cooperativas_categorias_materiais_categoria_residuo_id FOREIGN KEY (categoria_residuo_id) REFERENCES public.tb_lkp_categorias_residuos(id_categoria);


--
-- TOC entry 4041 (class 2606 OID 24893)
-- Name: tb_rel_cooperativas_categorias_materiais fk_cooperativas_categorias_materiais_cooperativa_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_cooperativas_categorias_materiais
    ADD CONSTRAINT fk_cooperativas_categorias_materiais_cooperativa_id FOREIGN KEY (cooperativa_id) REFERENCES public.tb_cooperativas(id_cooperativa);


--
-- TOC entry 4036 (class 2606 OID 24857)
-- Name: tb_cooperativas fk_cooperativas_endereco_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_cooperativas
    ADD CONSTRAINT fk_cooperativas_endereco_id FOREIGN KEY (endereco_id) REFERENCES public.tb_enderecos(id_endereco);


--
-- TOC entry 4037 (class 2606 OID 24852)
-- Name: tb_cooperativas fk_cooperativas_usuario_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_cooperativas
    ADD CONSTRAINT fk_cooperativas_usuario_id FOREIGN KEY (usuario_id) REFERENCES public.tb_usuarios(id_usuario);


--
-- TOC entry 4106 (class 2606 OID 57356)
-- Name: tb_recuperacoes_senha fk_esqueci_senha_usuario_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_recuperacoes_senha
    ADD CONSTRAINT fk_esqueci_senha_usuario_id FOREIGN KEY (usuario_id) REFERENCES public.tb_usuarios(id_usuario);


--
-- TOC entry 4104 (class 2606 OID 25786)
-- Name: tb_mensagens_conversas fk_mensagens_conversas_conversa_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_mensagens_conversas
    ADD CONSTRAINT fk_mensagens_conversas_conversa_id FOREIGN KEY (conversa_id) REFERENCES public.tb_conversas(id_conversa);


--
-- TOC entry 4105 (class 2606 OID 25791)
-- Name: tb_mensagens_conversas fk_mensagens_conversas_remetente_usuario_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_mensagens_conversas
    ADD CONSTRAINT fk_mensagens_conversas_remetente_usuario_id FOREIGN KEY (remetente_usuario_id) REFERENCES public.tb_usuarios(id_usuario);


--
-- TOC entry 4033 (class 2606 OID 24825)
-- Name: tb_moradores fk_moradores_condominio_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_moradores
    ADD CONSTRAINT fk_moradores_condominio_id FOREIGN KEY (condominio_id) REFERENCES public.tb_condominios(id_condominio);


--
-- TOC entry 4034 (class 2606 OID 24830)
-- Name: tb_moradores fk_moradores_torre_condominio; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_moradores
    ADD CONSTRAINT fk_moradores_torre_condominio FOREIGN KEY (torre_id, condominio_id) REFERENCES public.tb_torres(id_torre, condominio_id);


--
-- TOC entry 4035 (class 2606 OID 24820)
-- Name: tb_moradores fk_moradores_usuario_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_moradores
    ADD CONSTRAINT fk_moradores_usuario_id FOREIGN KEY (usuario_id) REFERENCES public.tb_usuarios(id_usuario);


--
-- TOC entry 4082 (class 2606 OID 25427)
-- Name: tb_movimentacoes_pontos fk_mov_pontos_categoria_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_movimentacoes_pontos
    ADD CONSTRAINT fk_mov_pontos_categoria_id FOREIGN KEY (categoria_id) REFERENCES public.tb_lkp_categorias_residuos(id_categoria);


--
-- TOC entry 4083 (class 2606 OID 25417)
-- Name: tb_movimentacoes_pontos fk_mov_pontos_condominio_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_movimentacoes_pontos
    ADD CONSTRAINT fk_mov_pontos_condominio_id FOREIGN KEY (condominio_id) REFERENCES public.tb_condominios(id_condominio);


--
-- TOC entry 4084 (class 2606 OID 25442)
-- Name: tb_movimentacoes_pontos fk_mov_pontos_movimentacao_referencia_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_movimentacoes_pontos
    ADD CONSTRAINT fk_mov_pontos_movimentacao_referencia_id FOREIGN KEY (movimentacao_referencia_id) REFERENCES public.tb_movimentacoes_pontos(id_movimentacao);


--
-- TOC entry 4085 (class 2606 OID 25432)
-- Name: tb_movimentacoes_pontos fk_mov_pontos_postagem_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_movimentacoes_pontos
    ADD CONSTRAINT fk_mov_pontos_postagem_id FOREIGN KEY (postagem_id) REFERENCES public.tb_postagens(id_postagem);


--
-- TOC entry 4086 (class 2606 OID 25437)
-- Name: tb_movimentacoes_pontos fk_mov_pontos_tentativa_quiz_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_movimentacoes_pontos
    ADD CONSTRAINT fk_mov_pontos_tentativa_quiz_id FOREIGN KEY (tentativa_quiz_id) REFERENCES public.tb_tentativas_quiz(id_tentativa);


--
-- TOC entry 4087 (class 2606 OID 25422)
-- Name: tb_movimentacoes_pontos fk_mov_pontos_torre_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_movimentacoes_pontos
    ADD CONSTRAINT fk_mov_pontos_torre_id FOREIGN KEY (torre_id) REFERENCES public.tb_torres(id_torre);


--
-- TOC entry 4088 (class 2606 OID 25412)
-- Name: tb_movimentacoes_pontos fk_mov_pontos_usuario_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_movimentacoes_pontos
    ADD CONSTRAINT fk_mov_pontos_usuario_id FOREIGN KEY (usuario_id) REFERENCES public.tb_usuarios(id_usuario);


--
-- TOC entry 4028 (class 2606 OID 24756)
-- Name: tb_notificacoes fk_notificacoes_usuario_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_notificacoes
    ADD CONSTRAINT fk_notificacoes_usuario_id FOREIGN KEY (usuario_id) REFERENCES public.tb_usuarios(id_usuario);


--
-- TOC entry 4070 (class 2606 OID 25248)
-- Name: tb_perguntas_quiz fk_perguntas_quiz_quiz_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_perguntas_quiz
    ADD CONSTRAINT fk_perguntas_quiz_quiz_id FOREIGN KEY (quiz_id) REFERENCES public.tb_quizzes(id_quiz) ON UPDATE CASCADE;


--
-- TOC entry 4042 (class 2606 OID 24919)
-- Name: tb_rel_pontos_coletas_categorias fk_pontos_coletas_categorias_categoria_residuo_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_pontos_coletas_categorias
    ADD CONSTRAINT fk_pontos_coletas_categorias_categoria_residuo_id FOREIGN KEY (categoria_residuo_id) REFERENCES public.tb_lkp_categorias_residuos(id_categoria);


--
-- TOC entry 4043 (class 2606 OID 24914)
-- Name: tb_rel_pontos_coletas_categorias fk_pontos_coletas_categorias_ponto_coleta_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_pontos_coletas_categorias
    ADD CONSTRAINT fk_pontos_coletas_categorias_ponto_coleta_id FOREIGN KEY (ponto_coleta_id) REFERENCES public.tb_pontos_coletas(id_ponto_coleta);


--
-- TOC entry 4038 (class 2606 OID 24877)
-- Name: tb_pontos_coletas fk_pontos_coletas_cooperativa_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_pontos_coletas
    ADD CONSTRAINT fk_pontos_coletas_cooperativa_id FOREIGN KEY (cooperativa_id) REFERENCES public.tb_cooperativas(id_cooperativa);


--
-- TOC entry 4039 (class 2606 OID 24872)
-- Name: tb_pontos_coletas fk_pontos_coletas_endereco_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_pontos_coletas
    ADD CONSTRAINT fk_pontos_coletas_endereco_id FOREIGN KEY (endereco_id) REFERENCES public.tb_enderecos(id_endereco);


--
-- TOC entry 4048 (class 2606 OID 25015)
-- Name: tb_postagens fk_postagens_categoria_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_postagens
    ADD CONSTRAINT fk_postagens_categoria_id FOREIGN KEY (categoria_id) REFERENCES public.tb_lkp_categorias_residuos(id_categoria);


--
-- TOC entry 4049 (class 2606 OID 25005)
-- Name: tb_postagens fk_postagens_condominio_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_postagens
    ADD CONSTRAINT fk_postagens_condominio_id FOREIGN KEY (condominio_id) REFERENCES public.tb_condominios(id_condominio);


--
-- TOC entry 4050 (class 2606 OID 25020)
-- Name: tb_postagens fk_postagens_status_validacao_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_postagens
    ADD CONSTRAINT fk_postagens_status_validacao_id FOREIGN KEY (status_validacao_id) REFERENCES public.tb_lkp_status_validacoes_postagens(id_status_validacao);


--
-- TOC entry 4051 (class 2606 OID 25010)
-- Name: tb_postagens fk_postagens_torre_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_postagens
    ADD CONSTRAINT fk_postagens_torre_id FOREIGN KEY (torre_id) REFERENCES public.tb_torres(id_torre);


--
-- TOC entry 4052 (class 2606 OID 25000)
-- Name: tb_postagens fk_postagens_usuario_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_postagens
    ADD CONSTRAINT fk_postagens_usuario_id FOREIGN KEY (usuario_id) REFERENCES public.tb_usuarios(id_usuario);


--
-- TOC entry 4068 (class 2606 OID 25224)
-- Name: tb_quizzes fk_quizzes_aula_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_quizzes
    ADD CONSTRAINT fk_quizzes_aula_id FOREIGN KEY (aula_id) REFERENCES public.tb_aulas(id_aula) ON UPDATE CASCADE;


--
-- TOC entry 4069 (class 2606 OID 25229)
-- Name: tb_quizzes fk_quizzes_curso_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_quizzes
    ADD CONSTRAINT fk_quizzes_curso_id FOREIGN KEY (curso_id) REFERENCES public.tb_cursos(id_curso);


--
-- TOC entry 4065 (class 2606 OID 25162)
-- Name: tb_rel_recorrencias_agendamentos fk_recorrencias_agendamentos_agendamento_coleta_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_recorrencias_agendamentos
    ADD CONSTRAINT fk_recorrencias_agendamentos_agendamento_coleta_id FOREIGN KEY (agendamento_coleta_id) REFERENCES public.tb_agendamentos_coletas(id_agendamento_coleta);


--
-- TOC entry 4066 (class 2606 OID 25167)
-- Name: tb_rel_recorrencias_agendamentos fk_recorrencias_agendamentos_dia_semana_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_recorrencias_agendamentos
    ADD CONSTRAINT fk_recorrencias_agendamentos_dia_semana_id FOREIGN KEY (dia_semana_id) REFERENCES public.tb_lkp_dias_semanas(id_dia_semana);


--
-- TOC entry 4076 (class 2606 OID 25328)
-- Name: tb_rel_respostas_tentativas_quiz fk_respostas_tentativas_quiz_alternativa_escolhida_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_respostas_tentativas_quiz
    ADD CONSTRAINT fk_respostas_tentativas_quiz_alternativa_escolhida_id FOREIGN KEY (alternativa_escolhida_id) REFERENCES public.tb_alternativas_quiz(id_alternativa);


--
-- TOC entry 4077 (class 2606 OID 25323)
-- Name: tb_rel_respostas_tentativas_quiz fk_respostas_tentativas_quiz_pergunta_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_respostas_tentativas_quiz
    ADD CONSTRAINT fk_respostas_tentativas_quiz_pergunta_id FOREIGN KEY (pergunta_id) REFERENCES public.tb_perguntas_quiz(id_pergunta);


--
-- TOC entry 4078 (class 2606 OID 25318)
-- Name: tb_rel_respostas_tentativas_quiz fk_respostas_tentativas_quiz_tentativa_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_respostas_tentativas_quiz
    ADD CONSTRAINT fk_respostas_tentativas_quiz_tentativa_id FOREIGN KEY (tentativa_id) REFERENCES public.tb_tentativas_quiz(id_tentativa) ON UPDATE CASCADE;


--
-- TOC entry 4026 (class 2606 OID 24722)
-- Name: tb_sindicos fk_sindicos_usuario_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_sindicos
    ADD CONSTRAINT fk_sindicos_usuario_id FOREIGN KEY (usuario_id) REFERENCES public.tb_usuarios(id_usuario);


--
-- TOC entry 4027 (class 2606 OID 24738)
-- Name: tb_telefones fk_telefones_usuario_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_telefones
    ADD CONSTRAINT fk_telefones_usuario_id FOREIGN KEY (usuario_id) REFERENCES public.tb_usuarios(id_usuario);


--
-- TOC entry 4072 (class 2606 OID 25295)
-- Name: tb_tentativas_quiz fk_tentativas_quiz_condominio_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_tentativas_quiz
    ADD CONSTRAINT fk_tentativas_quiz_condominio_id FOREIGN KEY (condominio_id) REFERENCES public.tb_condominios(id_condominio);


--
-- TOC entry 4073 (class 2606 OID 25290)
-- Name: tb_tentativas_quiz fk_tentativas_quiz_quiz_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_tentativas_quiz
    ADD CONSTRAINT fk_tentativas_quiz_quiz_id FOREIGN KEY (quiz_id) REFERENCES public.tb_quizzes(id_quiz);


--
-- TOC entry 4074 (class 2606 OID 25300)
-- Name: tb_tentativas_quiz fk_tentativas_quiz_torre_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_tentativas_quiz
    ADD CONSTRAINT fk_tentativas_quiz_torre_id FOREIGN KEY (torre_id) REFERENCES public.tb_torres(id_torre);


--
-- TOC entry 4075 (class 2606 OID 25285)
-- Name: tb_tentativas_quiz fk_tentativas_quiz_usuario_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_tentativas_quiz
    ADD CONSTRAINT fk_tentativas_quiz_usuario_id FOREIGN KEY (usuario_id) REFERENCES public.tb_usuarios(id_usuario);


--
-- TOC entry 4032 (class 2606 OID 24804)
-- Name: tb_torres fk_torres_condominio_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_torres
    ADD CONSTRAINT fk_torres_condominio_id FOREIGN KEY (condominio_id) REFERENCES public.tb_condominios(id_condominio);


--
-- TOC entry 4044 (class 2606 OID 24959)
-- Name: tb_rel_usuarios_condominios fk_usuarios_condominios_aprovado_por_usuario_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_usuarios_condominios
    ADD CONSTRAINT fk_usuarios_condominios_aprovado_por_usuario_id FOREIGN KEY (aprovado_por_usuario_id) REFERENCES public.tb_usuarios(id_usuario);


--
-- TOC entry 4045 (class 2606 OID 24954)
-- Name: tb_rel_usuarios_condominios fk_usuarios_condominios_condominio_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_usuarios_condominios
    ADD CONSTRAINT fk_usuarios_condominios_condominio_id FOREIGN KEY (condominio_id) REFERENCES public.tb_condominios(id_condominio);


--
-- TOC entry 4046 (class 2606 OID 24964)
-- Name: tb_rel_usuarios_condominios fk_usuarios_condominios_nivel_confianca_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_usuarios_condominios
    ADD CONSTRAINT fk_usuarios_condominios_nivel_confianca_id FOREIGN KEY (nivel_confianca_id) REFERENCES public.tb_lkp_niveis_confianca(id_nivel_confianca);


--
-- TOC entry 4047 (class 2606 OID 24949)
-- Name: tb_rel_usuarios_condominios fk_usuarios_condominios_usuario_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_usuarios_condominios
    ADD CONSTRAINT fk_usuarios_condominios_usuario_id FOREIGN KEY (usuario_id) REFERENCES public.tb_usuarios(id_usuario);


--
-- TOC entry 4079 (class 2606 OID 25353)
-- Name: tb_rel_usuarios_cursos fk_usuarios_cursos_aula_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_usuarios_cursos
    ADD CONSTRAINT fk_usuarios_cursos_aula_id FOREIGN KEY (aula_id) REFERENCES public.tb_aulas(id_aula);


--
-- TOC entry 4080 (class 2606 OID 25348)
-- Name: tb_rel_usuarios_cursos fk_usuarios_cursos_usuario_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_usuarios_cursos
    ADD CONSTRAINT fk_usuarios_cursos_usuario_id FOREIGN KEY (usuario_id) REFERENCES public.tb_usuarios(id_usuario);


--
-- TOC entry 4024 (class 2606 OID 24707)
-- Name: tb_usuarios fk_usuarios_endereco_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_usuarios
    ADD CONSTRAINT fk_usuarios_endereco_id FOREIGN KEY (endereco_id) REFERENCES public.tb_enderecos(id_endereco);


--
-- TOC entry 4025 (class 2606 OID 24702)
-- Name: tb_usuarios fk_usuarios_tipo_usuario_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_usuarios
    ADD CONSTRAINT fk_usuarios_tipo_usuario_id FOREIGN KEY (tipo_usuario_id) REFERENCES public.tb_lkp_tipos_usuarios(id_tipo_usuario);


--
-- TOC entry 4062 (class 2606 OID 25119)
-- Name: tb_visitas_coletas fk_visitas_coletas_agendamento_coleta_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_visitas_coletas
    ADD CONSTRAINT fk_visitas_coletas_agendamento_coleta_id FOREIGN KEY (agendamento_coleta_id) REFERENCES public.tb_agendamentos_coletas(id_agendamento_coleta);


--
-- TOC entry 4053 (class 2606 OID 25056)
-- Name: tb_rel_votos_postagens fk_votos_postagens_motivo_denuncia_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_votos_postagens
    ADD CONSTRAINT fk_votos_postagens_motivo_denuncia_id FOREIGN KEY (motivo_denuncia_id) REFERENCES public.tb_lkp_motivos_denuncia(id_motivo_denuncia);


--
-- TOC entry 4054 (class 2606 OID 25041)
-- Name: tb_rel_votos_postagens fk_votos_postagens_postagem_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_votos_postagens
    ADD CONSTRAINT fk_votos_postagens_postagem_id FOREIGN KEY (postagem_id) REFERENCES public.tb_postagens(id_postagem) ON UPDATE CASCADE;


--
-- TOC entry 4055 (class 2606 OID 25051)
-- Name: tb_rel_votos_postagens fk_votos_postagens_tipo_voto_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_votos_postagens
    ADD CONSTRAINT fk_votos_postagens_tipo_voto_id FOREIGN KEY (tipo_voto_id) REFERENCES public.tb_lkp_tipos_votos_postagens(id_tipo_voto);


--
-- TOC entry 4056 (class 2606 OID 25046)
-- Name: tb_rel_votos_postagens fk_votos_postagens_usuario_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tb_rel_votos_postagens
    ADD CONSTRAINT fk_votos_postagens_usuario_id FOREIGN KEY (usuario_id) REFERENCES public.tb_usuarios(id_usuario);


-- Completed on 2026-10-29 23:14:41

--
-- PostgreSQL database dump complete
--


