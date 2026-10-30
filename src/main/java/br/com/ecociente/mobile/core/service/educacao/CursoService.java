package br.com.ecociente.mobile.core.service.educacao;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.educacao.Curso;
import br.com.ecociente.mobile.core.gateway.educacao.CursoGateway;

@Service
public class CursoService {

    private final CursoGateway gateway;

    public CursoService(CursoGateway gateway) {
        this.gateway = gateway;
    }

    @Transactional(readOnly = true)
    public List<Curso> listarCursos() {
        return gateway.buscarCursosAtivos();
    }

    @Transactional(readOnly = true)
    public Optional<Curso> buscarCurso(Integer cursoId) {
        return gateway.buscarCursoAtivoPorId(cursoId);
    }
}