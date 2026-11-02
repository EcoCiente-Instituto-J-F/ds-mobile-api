package br.com.ecociente.mobile.core.service.cadastro;

import java.time.OffsetDateTime;

import br.com.ecociente.mobile.core.gateway.cadastro.CodificadorSenhaGateway;

import org.springframework.stereotype.Service;
import br.com.ecociente.mobile.core.gateway.cadastro.TiposCadastroGateway;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.cadastro.Cooperativa;
import br.com.ecociente.mobile.core.domain.cadastro.Endereco;
import br.com.ecociente.mobile.core.domain.cadastro.Usuario;
import br.com.ecociente.mobile.core.exception.cadastro.AlreadyExistsException;
import br.com.ecociente.mobile.core.gateway.cadastro.CepGateway;
import br.com.ecociente.mobile.core.gateway.cadastro.CooperativaGateway;
import br.com.ecociente.mobile.core.gateway.cadastro.EnderecoGateway;
import br.com.ecociente.mobile.core.gateway.cadastro.UsuarioGateway;
import br.com.ecociente.mobile.core.domain.cadastro.ResultadoCadastroCooperativa;
import br.com.ecociente.mobile.core.domain.cadastro.Cnpj;
import br.com.ecociente.mobile.core.gateway.cadastro.ConsultaDocumentoGateway;

@Service
public class CadastrarCooperativaService {
  private final UsuarioGateway usuarioGateway;
  private final CooperativaGateway cooperativaGateway;
  private final EnderecoGateway enderecoGateway;
  private final CepGateway cepGateway;
  private final CodificadorSenhaGateway codificadorSenhaGateway;
  private final TiposCadastroGateway tiposCadastro;
  private final ConsultaDocumentoGateway consultaDocumentoGateway;

  public CadastrarCooperativaService(
      UsuarioGateway usuarioGateway,
      CooperativaGateway cooperativaGateway,
      EnderecoGateway enderecoGateway,
      CepGateway cepGateway,
      CodificadorSenhaGateway codificadorSenhaGateway, TiposCadastroGateway tiposCadastro,
      ConsultaDocumentoGateway consultaDocumentoGateway) {
    this.usuarioGateway = usuarioGateway;
    this.cooperativaGateway = cooperativaGateway;
    this.enderecoGateway = enderecoGateway;
    this.cepGateway = cepGateway;
    this.codificadorSenhaGateway = codificadorSenhaGateway;
    this.tiposCadastro = tiposCadastro;
    this.consultaDocumentoGateway = consultaDocumentoGateway;
  }

  @Transactional
  public ResultadoCadastroCooperativa executar(String nomeResponsavel, String email, String senha,
      String nomeCooperativa, String cnpj,
      String emailCooperativa, String telefone, String cep, String numero, String complemento) {
    String emailLimpo = email.trim().toLowerCase();
    String cnpjLimpo = Cnpj.normalizarEValidar(cnpj);

    if (usuarioGateway.existePorEmail(emailLimpo)) {
      throw new AlreadyExistsException("EMAIL_ALREADY_EXISTS", "Email já cadastrado");
    }
    if (cooperativaGateway.existePorCnpj(cnpjLimpo)) {
      throw new AlreadyExistsException("CNPJ_ALREADY_EXISTS", "CPF já cadastrado");
    }
    consultaDocumentoGateway.validarCnpj(cnpjLimpo);

    Endereco enderecoCep = cepGateway.buscarEnderecoPorCep(cep);
    Endereco enderecoSalvo = enderecoGateway.salvar(new Endereco(
        null, enderecoCep.getCep(), enderecoCep.getEstado(), enderecoCep.getCidade(),
        enderecoCep.getLogradouro(), numero, complemento));

    String senhaHash = codificadorSenhaGateway.gerarHash(senha);
    Usuario usuario = new Usuario(
        null, nomeResponsavel, emailLimpo, senhaHash, null, null,
        null, true, OffsetDateTime.now(), tiposCadastro.cooperativa(), enderecoSalvo.getIdEndereco());
    Usuario usuarioSalvo = usuarioGateway.salvar(usuario);

    Cooperativa cooperativa = new Cooperativa(
        null, cnpjLimpo, nomeCooperativa, emailCooperativa.trim().toLowerCase(),
        telefone.replaceAll("\\D", ""), OffsetDateTime.now(), usuarioSalvo.getIdUsuario(),
        enderecoSalvo.getIdEndereco());

    Cooperativa cooperativaSalva = cooperativaGateway.salvar(cooperativa);
    return new ResultadoCadastroCooperativa(usuarioSalvo, cooperativaSalva, enderecoSalvo);
  }

}
