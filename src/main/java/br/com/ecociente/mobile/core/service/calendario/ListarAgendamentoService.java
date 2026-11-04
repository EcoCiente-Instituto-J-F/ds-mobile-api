package br.com.ecociente.mobile.core.service.calendario;

import br.com.ecociente.mobile.core.domain.calendario.PaginaCalendario;
import br.com.ecociente.mobile.core.domain.calendario.PaginacaoCalendario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.calendario.AgendamentoColeta;
import br.com.ecociente.mobile.core.domain.calendario.AgendamentoFiltro;
import br.com.ecociente.mobile.core.domain.calendario.Perfil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListarAgendamentoService {

    private final ResolvedorConsultaAgendamento resolvedor;

    @Transactional(readOnly = true)
    public PaginaCalendario<AgendamentoColeta> executar(
            Integer usuarioId,
            Perfil perfil,
            AgendamentoFiltro filtro,
            PaginacaoCalendario paginacao) {

        ConsultaAgendamentoPorPerfil consulta = resolvedor.resolver(perfil);
        return consulta.listar(usuarioId, filtro, paginacao);
    }
}
