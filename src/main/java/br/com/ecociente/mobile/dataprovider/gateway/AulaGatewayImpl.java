package br.com.ecociente.mobile.dataprovider.gateway;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.Aula;
import br.com.ecociente.mobile.core.gateway.AulaGateway;
import br.com.ecociente.mobile.dataprovider.mapper.AulaEntityMapper;
import br.com.ecociente.mobile.dataprovider.repository.AulaRepository;

@Component
public class AulaGatewayImpl implements AulaGateway {

    private final AulaRepository repository;
    private final AulaEntityMapper mapper;

    public AulaGatewayImpl(
        AulaRepository repository,
        AulaEntityMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Aula> buscarAulasPorCursoId(Integer cursoId) {
        return repository.findByCursoIdOrderByOrdemAscIdAsc(cursoId)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public Optional<Aula> buscarAulaDeCursoAtivoPorId(Integer aulaId) {
        return repository.buscarAulaDeCursoAtivoPorId(aulaId)
            .map(mapper::toDomain);
    }
}