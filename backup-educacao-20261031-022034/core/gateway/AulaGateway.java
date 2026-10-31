package br.com.ecociente.mobile.core.gateway;

import java.util.List;
import java.util.Optional;

import br.com.ecociente.mobile.core.domain.educacao.Aula;

public interface AulaGateway {

    List<Aula> buscarAulasPorCursoId(Integer cursoId);

    Optional<Aula> buscarAulaDeCursoAtivoPorId(Integer aulaId);
}