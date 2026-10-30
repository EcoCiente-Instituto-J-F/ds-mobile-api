package br.com.ecociente.mobile.entrypoint.controller.calendario;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.core.Authentication;

import br.com.ecociente.mobile.config.security.CalendarioUsuario;
import br.com.ecociente.mobile.core.domain.calendario.AgendamentoFiltro;
import br.com.ecociente.mobile.core.domain.calendario.StatusType;
import br.com.ecociente.mobile.core.service.calendario.BuscarProximaVisitaService;
import br.com.ecociente.mobile.core.service.calendario.ListarAgendamentoService;
import br.com.ecociente.mobile.entrypoint.dto.calendario.response.CalendarioResponseDto;
import br.com.ecociente.mobile.entrypoint.mapper.calendario.AgendamentoFiltroMapper;
import br.com.ecociente.mobile.entrypoint.mapper.calendario.CalendarioResponseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/v1/agendamentos")
@RequiredArgsConstructor
@Tag(name = "Agendamento", description = "API responsável pelo gerenciamento dos agendamentos de coleta.")
public class AgendamentoController {

  private final BuscarProximaVisitaService buscarProximaVisitaService;
  private final ListarAgendamentoService listarAgendamentoService;
  private final CalendarioResponseMapper calendarioResponseMapper;
  private final AgendamentoFiltroMapper agendamentoFiltroMapper;

  @GetMapping
  @Operation(
    summary = "Listar agendamentos",
    description = "Endpoint para listar os agendamentos de coleta de acordo com o perfil do usuário autenticado."
                  +"Além disso, é possível filtrar os agendamentos por status, data de início, data de fim e se possui recorrência."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lista de agendamentos retornada com sucesso."),
    @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado ou token inválido."),
    @ApiResponse(responseCode = "403", description = "Usuário não autorizado")
  })
  public ResponseEntity<Page<CalendarioResponseDto>> listarAgendamentos(
    Authentication authentication,

    @Parameter(
      description = "Filtra pelo status do agendamento",
      example = "AGENDAMENTO")
    @RequestParam(required = false) StatusType status,

    @Parameter(
      description = "Data inicial do intervalo de buscar",
      example = "2025-09-01T00:00:00")
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,

    @Parameter(
      description = "Data final do intervalo de busca",
      example = "2025-09-30T23:59:59")
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,

    @Parameter(
      description = "Filtra agendamentos recorrentes ou não recorrentes",
      example = "false")
    @RequestParam(required = false) Boolean possuiRecorrencia,

    @Parameter(
      description = "Filtra por condominio. Para SINDICO e MORADOR, o filtro é aplicado somente entre os condomínios vinculados ao usuário autenticado",
      example = "10")
    @RequestParam(required = false) Integer condominioId,

    @Parameter(
      description = "Filtra pela cooperativa responsável. Para SINDICO e MORADOR o resultado continua limitado aos condomínios vinculados ao usuário autenticado",
      example = "5")
    @RequestParam(required = false) Integer cooperativaId,

    @Parameter(
      description = "Parâmetros de paginação e ordenação")
    @PageableDefault(size = 10,sort ="dataInicio", direction = Sort.Direction.ASC) Pageable pageable){
      AgendamentoFiltro filtro = agendamentoFiltroMapper.toDomain(status,dataInicio,dataFim,condominioId,cooperativaId,possuiRecorrencia);
      Page<CalendarioResponseDto> response = listarAgendamentoService
        .executar(CalendarioUsuario.id(authentication),CalendarioUsuario.perfil(authentication), filtro, pageable)
        .map(calendarioResponseMapper::toResponseDto);
        return ResponseEntity.ok(response);

  }

  @GetMapping("/proxima")
  @Operation(
    summary = "Buscar próximo agendamento",
    description = "Endpoint para buscar o próximo agendamento de coleta de acordo com o perfil do usuário autenticado."
                  +"Além disso, é possível filtrar os agendamentos por status, data de início, data de fim e se possui recorrência."

  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Próximo agendamento retornado com sucesso."),
    @ApiResponse (responseCode = "400", description = "Parâmetros inválidos"),
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado ou token inválido."),
    @ApiResponse(responseCode = "403", description = "Usuário não autorizado"),
    @ApiResponse (responseCode = "404", description = "Nenhum próximo agendamento encontrado")
  })
  public ResponseEntity<CalendarioResponseDto> buscarProximoAgendamento(
    Authentication authentication,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,
    @RequestParam(required = false) Boolean possuiRecorrencia,
    @RequestParam(required = false) Integer condominioId,
    @RequestParam(required = false) Integer cooperativaId
  ){
    AgendamentoFiltro filtro = agendamentoFiltroMapper.toDomain(null,dataInicio,dataFim, condominioId, cooperativaId, possuiRecorrencia);
    return buscarProximaVisitaService
      .executar(CalendarioUsuario.id(authentication), filtro,CalendarioUsuario.perfil(authentication))
      .map(calendarioResponseMapper::toResponseDto)
      .map(ResponseEntity::ok)
      .orElse(ResponseEntity.notFound().build());
  } 
  
}
