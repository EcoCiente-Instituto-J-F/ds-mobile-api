package br.com.ecociente.mobile.dataprovider.gateway;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.Curso;
import br.com.ecociente.mobile.core.gateway.CursoGateway;
import br.com.ecociente.mobile.dataprovider.mapper.CursoEntityMapper;
import br.com.ecociente.mobile.dataprovider.repository.CursoRepository;

@Component
public class CursoGatewayImpl implements CursoGateway {

    private final CursoRepository repository;
    private final CursoEntityMapper mapper;

    public CursoGatewayImpl(
        CursoRepository repository,
        CursoEntityMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Curso> buscarCursosAtivos() {
        return repository.findByEstaAtivoTrueOrderByTituloAscIdAsc()
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public Optional<Curso> buscarCursoAtivoPorId(Integer cursoId) {
        return repository.findByIdAndEstaAtivoTrue(cursoId)
            .map(mapper::toDomain);
    }
}