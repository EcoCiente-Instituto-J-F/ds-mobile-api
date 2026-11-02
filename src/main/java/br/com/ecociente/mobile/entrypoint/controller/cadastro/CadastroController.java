package br.com.ecociente.mobile.entrypoint.controller.cadastro;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.ecociente.mobile.core.service.cadastro.CadastrarCooperativaService;
import br.com.ecociente.mobile.core.service.cadastro.CadastrarMoradorService;
import br.com.ecociente.mobile.core.service.cadastro.CadastrarUsuarioComumService;
import br.com.ecociente.mobile.entrypoint.dto.cadastro.request.ComumCadastroRequestDto;
import br.com.ecociente.mobile.entrypoint.dto.cadastro.request.CooperativaCadastroResquestDto;
import br.com.ecociente.mobile.entrypoint.dto.cadastro.request.MoradorCadastroRequestDto;
import br.com.ecociente.mobile.entrypoint.dto.cadastro.response.ComumCadastroResponseDto;
import br.com.ecociente.mobile.entrypoint.dto.cadastro.response.CooperativaCadastroResponseDto;
import br.com.ecociente.mobile.entrypoint.dto.cadastro.response.MoradorCadastroResponseDto;
import br.com.ecociente.mobile.entrypoint.mapper.cadastro.UsuarioMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController 
@RequestMapping("/api/cadastro")
@AllArgsConstructor 
public class CadastroController {

  private final CadastrarUsuarioComumService CadastrarUsuarioComumService;
  private final CadastrarMoradorService CadastrarMoradorService;
  private final CadastrarCooperativaService CadastrarCooperativaService;
  private final UsuarioMapper usuarioMapper;

  @PostMapping("/comum")
  @Operation (
    summary = "Cadastar usuário comum",
    description = "Endpoint para cadastar usuário comum"
  )
  @ApiResponses ({
    @ApiResponse (responseCode = "201", description = "Usuário criado com sucesso"),
    @ApiResponse (responseCode = "400", description = "Erro nos dados enviados"),
    @ApiResponse (responseCode = "500", description = "Erro no servidor")
  })
  public ResponseEntity<ComumCadastroResponseDto> cadastroComum(@Valid @RequestBody ComumCadastroRequestDto requestDto) {
    var saida = CadastrarUsuarioComumService.executar(
      requestDto.nomeUsuario(),
      requestDto.email(),
      requestDto.senha(),
      LocalDate.parse(requestDto.dataNascimento(), DateTimeFormatter.ISO_LOCAL_DATE),
      requestDto.cpf(),
      requestDto.cep(),
      requestDto.numero(),
      requestDto.complemento()
      );
    
    ComumCadastroResponseDto responseDto = usuarioMapper.toComumCadastroResponse(saida.usuario(),saida.endereco());  
    return ResponseEntity.status(HttpStatus.CREATED).body(responseDto); 
  }

  @PostMapping("/morador")
  @Operation (
    summary = "Cadastar morador",
    description = "Endpoint para cadastar usuários que pertencem a condomínios"
  )
  @ApiResponses ({
    @ApiResponse (responseCode = "201", description = "Usuário criado com sucesso"),
    @ApiResponse (responseCode = "400", description = "Erro nos dados enviados"),
    @ApiResponse (responseCode = "500", description = "Erro no servidor")
  })
  public ResponseEntity<MoradorCadastroResponseDto> cadastrarMorador (@Valid @RequestBody MoradorCadastroRequestDto requestDto) {
    var saida = CadastrarMoradorService.executar(
      requestDto.nomeUsuario(),
      requestDto.email(),
      requestDto.senha(),
      LocalDate.parse(requestDto.dataNascimento(), DateTimeFormatter.ISO_LOCAL_DATE),
      requestDto.cpf(),
      requestDto.codigoCondominio()
    );

    MoradorCadastroResponseDto response = usuarioMapper.toMoradorCadastroResponse(saida.usuario(),saida.codigoCondominio(),saida.nomeCondominio(),saida.enderecoCondominio());

    return ResponseEntity.status(HttpStatus.CREATED).body(response);    

  }

  @PostMapping("/cooperativa")
  @Operation (
    summary = "Cadastar cooperativa",
    description = "Endpoint para cadastar cooperativas"
  )
  @ApiResponses ({
    @ApiResponse (responseCode = "201", description = "Usuário criado com sucesso"),
    @ApiResponse (responseCode = "400", description = "Erro nos dados enviados"),
    @ApiResponse (responseCode = "500", description = "Erro no servidor")
  })
  public ResponseEntity<CooperativaCadastroResponseDto> cadastrarCooperativa (@Valid @RequestBody CooperativaCadastroResquestDto requestDto) {
    var saida = CadastrarCooperativaService.executar(
      requestDto.nomeResponsavel(),
      requestDto.email(),
      requestDto.senha(),
      requestDto.nomeCooperativa(),
      requestDto.cnpj(),
      requestDto.emailCooperativa(),
      requestDto.telefone(),
      requestDto.cep(),
      requestDto.numero(),
      requestDto.complemento()
    );

    CooperativaCadastroResponseDto response = usuarioMapper.toCooperativaCadastroResponse(saida.usuario(),saida.cooperativa().getNomeCoop(),saida.cooperativa().getCnpjCoop(),saida.cooperativa().getEmailCoop(),saida.cooperativa().getTelefoneCoop(),saida.endereco());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);    

  }
  
}
