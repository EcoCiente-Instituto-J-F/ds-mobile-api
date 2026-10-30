package br.com.ecociente.mobile.core.service.educacao;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.educacao.Aula;
import br.com.ecociente.mobile.core.gateway.educacao.AulaGateway;
import br.com.ecociente.mobile.core.gateway.educacao.CursoGateway;

@Service
public class AulaService {

    private final AulaGateway aulaGateway;
    private final CursoGateway cursoGateway;

    public AulaService(
        AulaGateway aulaGateway,
        CursoGateway cursoGateway
    ) {
        this.aulaGateway = aulaGateway;
        this.cursoGateway = cursoGateway;
    }

    @Transactional(readOnly = true)
    public Optional<List<Aula>> listarAulas(Integer cursoId) {
        return cursoGateway.buscarCursoAtivoPorId(cursoId)
            .map(curso -> aulaGateway.buscarAulasPorCursoId(curso.id()));
    }

    @Transactional(readOnly = true)
    public Optional<Aula> buscarAula(Integer aulaId) {
        return aulaGateway.buscarAulaDeCursoAtivoPorId(aulaId);
    }
}