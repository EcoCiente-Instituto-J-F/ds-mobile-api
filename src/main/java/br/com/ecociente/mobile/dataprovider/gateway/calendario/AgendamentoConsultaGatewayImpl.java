package br.com.ecociente.mobile.dataprovider.gateway.calendario;

import br.com.ecociente.mobile.core.domain.calendario.PaginaCalendario;
import br.com.ecociente.mobile.core.domain.calendario.PaginacaoCalendario;
import br.com.ecociente.mobile.dataprovider.mapper.calendario.PaginacaoCalendarioPersistenceMapper;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.calendario.AgendamentoColeta;
import br.com.ecociente.mobile.core.domain.calendario.AgendamentoFiltro;
import br.com.ecociente.mobile.core.gateway.calendario.AgendamentoConsultaGateway;
import br.com.ecociente.mobile.dataprovider.mapper.calendario.AgendamentoColetaMapper;
import br.com.ecociente.mobile.dataprovider.repository.AgendamentoColetaRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class AgendamentoConsultaGatewayImpl implements AgendamentoConsultaGateway {

  private final AgendamentoColetaRepository agendamentoColetaRepository;
  private final AgendamentoColetaMapper agendamentoColetaMapper;

  public AgendamentoConsultaGatewayImpl(AgendamentoColetaRepository agendamentoColetaRepository, AgendamentoColetaMapper agendamentoColetaMapper) {
    this.agendamentoColetaRepository = agendamentoColetaRepository;
    this.agendamentoColetaMapper = agendamentoColetaMapper;
  }

  
  @Override
  public PaginaCalendario<AgendamentoColeta> buscarPorSindico(
      Integer usuarioId,
      AgendamentoFiltro filtro,
      PaginacaoCalendario paginacao) {

    var pageable = PaginacaoCalendarioPersistenceMapper.toPageable(
        paginacao);

    return PaginacaoCalendarioPersistenceMapper.toDomain(
        buscarPorSindicoPersistencia(usuarioId, filtro, pageable),
        paginacao);
  }

  private Page<AgendamentoColeta> buscarPorSindicoPersistencia(
      Integer usuarioId,
      AgendamentoFiltro filtro,
      Pageable pageable) {
    return complementarPagina(agendamentoColetaRepository
            .buscarPorSindico(
              usuarioId, 
              filtro.status() == null ? null : filtro.status().name(),
              filtro.dataInicio(),
              filtro.dataFim(),
              filtro.condominioId(),
              filtro.cooperativaId(),
              filtro.possuiRecorrencia(),
              semOrdenacao(pageable))
            .map(agendamentoColetaMapper::toDomain));
}

  @Override
  public PaginaCalendario<AgendamentoColeta> buscarPorCooperativa(
      Integer usuarioId,
      AgendamentoFiltro filtro,
      PaginacaoCalendario paginacao) {

    var pageable = PaginacaoCalendarioPersistenceMapper.toPageable(
        paginacao);

    return PaginacaoCalendarioPersistenceMapper.toDomain(
        buscarPorCooperativaPersistencia(usuarioId, filtro, pageable),
        paginacao);
  }

  private Page<AgendamentoColeta> buscarPorCooperativaPersistencia(
      Integer usuarioId,
      AgendamentoFiltro filtro,
      Pageable pageable) {
    return complementarPagina(agendamentoColetaRepository
            .buscarPorCooperativa(
              usuarioId, 
              filtro.status() == null ? null : filtro.status().name(),
              filtro.dataInicio(),
              filtro.dataFim(),
              filtro.condominioId(),
              filtro.possuiRecorrencia(),
              semOrdenacao(pageable))
            .map(agendamentoColetaMapper::toDomain));
}

  @Override
  public Optional<AgendamentoColeta> buscarProximoAgendamentoPorSindico(Integer usuarioId, AgendamentoFiltro filtro) {
    return agendamentoColetaRepository.buscarProximoAgendamentoPorSindico(
      usuarioId,
      filtro.dataInicio(),
      filtro.dataFim(),
      filtro.condominioId(),
      filtro.possuiRecorrencia(),
      filtro.cooperativaId()
    )
    .map(agendamentoColetaMapper::toDomain)
    .map(this::complementar);
  }

  @Override
  public Optional<AgendamentoColeta> buscarProximoAgendamentoPorCooperativa(Integer usuarioId, AgendamentoFiltro filtro) {
    return agendamentoColetaRepository.buscarProximoAgendamentoPorCooperativa(
      usuarioId,
      filtro.dataInicio(),
      filtro.dataFim(),
      filtro.possuiRecorrencia(),
      filtro.condominioId()
    )
    .map(agendamentoColetaMapper::toDomain)
    .map(this::complementar);
  }

  // A consulta devolve só os ids; o nome da cooperativa e do condomínio vem em duas consultas
  // por página, e não uma por agendamento.
  private Page<AgendamentoColeta> complementarPagina(Page<AgendamentoColeta> pagina) {
    List<AgendamentoColeta> agendamentos = pagina.getContent();

    if (agendamentos.isEmpty()) {
      return pagina;
    }

    Map<Integer, String> cooperativas = mapa(agendamentoColetaRepository.nomesDeCooperativas(
        agendamentos.stream().map(AgendamentoColeta::getCooperativaId).distinct().toList()));
    Map<Integer, String> condominios = mapa(agendamentoColetaRepository.nomesDeCondominios(
        agendamentos.stream().map(AgendamentoColeta::getCondominioId).distinct().toList()));

    agendamentos.forEach(agendamento -> {
      agendamento.setCooperativaNome(cooperativas.get(agendamento.getCooperativaId()));
      agendamento.setCondominioNome(condominios.get(agendamento.getCondominioId()));
    });

    return pagina;
  }

  private AgendamentoColeta complementar(AgendamentoColeta agendamento) {
    agendamento.setCooperativaNome(mapa(
        agendamentoColetaRepository.nomesDeCooperativas(List.of(agendamento.getCooperativaId())))
        .get(agendamento.getCooperativaId()));
    agendamento.setCondominioNome(mapa(
        agendamentoColetaRepository.nomesDeCondominios(List.of(agendamento.getCondominioId())))
        .get(agendamento.getCondominioId()));

    return agendamento;
  }

  private static Map<Integer, String> mapa(List<AgendamentoColetaRepository.NomeProjecao> nomes) {
    Map<Integer, String> resultado = new HashMap<>();
    nomes.forEach(item -> resultado.put(item.getId(), item.getNome()));
    return resultado;
  }

  // Em consulta nativa o Spring Data usa o nome da propriedade ("dataInicio") como se fosse
  // coluna e a consulta falha; a ordem fixa (data de início, depois id) já está na SQL.
  private static Pageable semOrdenacao(Pageable pageable) {
    return pageable.isPaged()
        ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())
        : pageable;
  }
}
