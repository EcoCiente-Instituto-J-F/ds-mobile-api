package br.com.ecociente.mobile.core.service.calendario;

import java.util.List;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.calendario.Perfil;
import br.com.ecociente.mobile.core.exception.calendario.PerfilNaoAutorizadoException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ResolvedorConsultaAgendamento {
  
  private final List<ConsultaAgendamentoPorPerfil> perfils;

  public ConsultaAgendamentoPorPerfil resolver(Perfil perfil){
    return perfils.stream()
      .filter(e -> e.perfilSuportado() == perfil)
      .findFirst()
      .orElseThrow(() -> new PerfilNaoAutorizadoException(perfil.name()));
  }
}
