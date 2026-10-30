package br.com.ecociente.mobile.core.service.calendario;

import java.util.Optional;

import org.springframework.stereotype.Service;

import br.com.ecociente.mobile.core.domain.calendario.AgendamentoColeta;
import br.com.ecociente.mobile.core.domain.calendario.AgendamentoFiltro;
import br.com.ecociente.mobile.core.domain.calendario.Perfil;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class BuscarProximaVisitaService {

   private final ResolvedorConsultaAgendamento resolvedor;

    public Optional<AgendamentoColeta> executar(
      Integer usuarioId, 
      AgendamentoFiltro filtro,
      Perfil perfil) {
      ConsultaAgendamentoPorPerfil consulta = resolvedor.resolver(perfil);
      return consulta.buscarProximo(usuarioId, filtro);
    }
  
}
