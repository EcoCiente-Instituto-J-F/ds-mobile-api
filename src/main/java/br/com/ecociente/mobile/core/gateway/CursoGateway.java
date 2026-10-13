package br.com.ecociente.mobile.core.gateway;

import java.util.List;
import java.util.Optional;

import br.com.ecociente.mobile.core.domain.Curso;

public interface CursoGateway {

    List<Curso> buscarCursosAtivos();

    Optional<Curso> buscarCursoAtivoPorId(Integer cursoId);
}