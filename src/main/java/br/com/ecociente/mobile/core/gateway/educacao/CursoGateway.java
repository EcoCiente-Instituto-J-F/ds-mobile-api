package br.com.ecociente.mobile.core.gateway.educacao;

import java.util.List;
import java.util.Optional;

import br.com.ecociente.mobile.core.domain.educacao.Curso;

public interface CursoGateway {

    List<Curso> buscarCursosAtivos();

    Optional<Curso> buscarCursoAtivoPorId(Integer cursoId);
}