package br.com.ecociente.mobile.core.service.calendario;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import br.com.ecociente.mobile.core.domain.calendario.AgendamentoColeta;
import br.com.ecociente.mobile.core.domain.calendario.AgendamentoFiltro;
import br.com.ecociente.mobile.core.domain.calendario.Perfil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListarAgendamentoService {

  private final ResolvedorConsultaAgendamento resolvedor;
    public Page<AgendamentoColeta> executar(
            Integer usuarioId,
            Perfil perfil,
            AgendamentoFiltro filtro,
            Pageable pageable) {

        ConsultaAgendamentoPorPerfil consulta = resolvedor.resolver(perfil);
        return consulta.listar(usuarioId, filtro, pageable);
    }
  
}
