package br.com.ecociente.mobile.core.service.cadastro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;

import org.springframework.stereotype.Service;
import br.com.ecociente.mobile.core.gateway.cadastro.TiposCadastroGateway;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.cadastro.Condominio;
import br.com.ecociente.mobile.core.domain.cadastro.Endereco;
import br.com.ecociente.mobile.core.domain.cadastro.Morador;
import br.com.ecociente.mobile.core.domain.cadastro.Usuario;
import br.com.ecociente.mobile.core.exception.cadastro.AlreadyExistsException;
import br.com.ecociente.mobile.core.exception.cadastro.BusinessException;
import br.com.ecociente.mobile.core.exception.cadastro.NotFoundException;
import br.com.ecociente.mobile.core.gateway.cadastro.CondominioGateway;
import br.com.ecociente.mobile.core.gateway.cadastro.EnderecoGateway;
import br.com.ecociente.mobile.core.gateway.cadastro.UsuarioGateway;
import br.com.ecociente.mobile.core.domain.cadastro.ResultadoCadastroMorador;
import br.com.ecociente.mobile.core.gateway.cadastro.CodificadorSenhaGateway;

@Service
public class CadastrarMoradorService {
  private final UsuarioGateway usuarioGateway;
  private final CondominioGateway condominioGateway;
  private final TiposCadastroGateway tiposCadastro;
  private final EnderecoGateway enderecoGateway;
  private final CodificadorSenhaGateway codificadorSenhaGateway;

  public CadastrarMoradorService(
      UsuarioGateway usuarioGateway,
      CondominioGateway condominioGateway,
      CodificadorSenhaGateway codificadorSenhaGateway, TiposCadastroGateway tiposCadastro,
      EnderecoGateway enderecoGateway) {
    this.usuarioGateway = usuarioGateway;
    this.condominioGateway = condominioGateway;
    this.codificadorSenhaGateway = codificadorSenhaGateway;
    this.tiposCadastro = tiposCadastro;
    this.enderecoGateway = enderecoGateway;
  }

  @Transactional
  public ResultadoCadastroMorador executar(String nomeCompleto, String email, String senha, LocalDate dataNascimento,
      String cpf,
      String codigoCondominio) {
    validarDataNascimento(dataNascimento);
    String emailLimpo = email.trim().toLowerCase();
    String cpfLimpo = cpf.replaceAll("\\D", "");

    if (usuarioGateway.existePorEmail(emailLimpo)) {
      throw new AlreadyExistsException("EMAIL_ALREADY_EXISTS", "Email já cadastrado");
    }
    if (usuarioGateway.existePorCpf(cpfLimpo)) {
      throw new AlreadyExistsException("CPF_ALREADY_EXISTS", "CPF já cadastrado");
    }

    Condominio condominio = condominioGateway.buscarPorCodigoAcesso(codigoCondominio.trim().toUpperCase())
        .orElseThrow(() -> new NotFoundException("CONDOMINIUM_NOT_FOUND", "Código de condomínio não encontrado"));

    int tipoUsuarioId = tiposCadastro.morador(condominio.getTipoCondominioId());

    Endereco enderecoCondominio = enderecoGateway.buscarPorId(condominio.getEnderecoId());

    String senhaHash = codificadorSenhaGateway.gerarHash(senha);
    Usuario usuario = new Usuario(
        null, nomeCompleto, emailLimpo, senhaHash, dataNascimento, cpfLimpo,
        null, true, OffsetDateTime.now(), tipoUsuarioId, enderecoCondominio.getIdEndereco());

    Usuario usuarioSalvo = usuarioGateway.salvar(usuario);

    Morador vinculoMorador = new Morador(
        null, usuarioSalvo.getIdUsuario(), condominio.getIdCondominio(),
        OffsetDateTime.now(), null, true, null, tiposCadastro.nivelInicial(), BigDecimal.ZERO, 0, 0, 0, null);

    condominioGateway.vincularMorador(vinculoMorador);

    enderecoGateway.buscarPorId(condominio.getEnderecoId());
    return new ResultadoCadastroMorador(
        usuarioSalvo,
        condominio.getCodigoAcesso(),
        condominio.getNomeCondominio(),
        enderecoCondominio);
  }

  private void validarDataNascimento(LocalDate data) {
    if (data.isAfter(LocalDate.now())) {
      throw new BusinessException("VALIDATION_ERROR", "Data de nascimento não pode ser futura");
    }
    if (Period.between(data, LocalDate.now()).getYears() < 10) {
      throw new BusinessException("VALIDATION_ERROR", "Idade mínima para cadastro é de 10 anos");
    }
  }

}
