package br.com.ecociente.mobile.core.service.cadastro;

import java.time.LocalDate;
import java.time.Period;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import br.com.ecociente.mobile.core.gateway.cadastro.TiposCadastroGateway;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;

import br.com.ecociente.mobile.core.domain.cadastro.Endereco;
import br.com.ecociente.mobile.core.domain.cadastro.Usuario;
import br.com.ecociente.mobile.core.exception.cadastro.AlreadyExistsException;
import br.com.ecociente.mobile.core.exception.cadastro.BusinessException;
import br.com.ecociente.mobile.core.gateway.cadastro.CepGateway;
import br.com.ecociente.mobile.core.gateway.cadastro.ConsultaDocumentoGateway;
import br.com.ecociente.mobile.core.gateway.cadastro.EnderecoGateway;
import br.com.ecociente.mobile.core.gateway.cadastro.UsuarioGateway;
import br.com.ecociente.mobile.core.usecase.cadastro.input.CadastroUsuarioComumUseCase;
import br.com.ecociente.mobile.core.usecase.cadastro.output.CadastroUsuarioComumOutput;

@Service 
public class CadastrarUsuarioComumService implements CadastroUsuarioComumUseCase {

  private final UsuarioGateway usuarioGateway;
  private final EnderecoGateway enderecoGateway;
  private final CepGateway cepGateway;
  private final PasswordEncoder passwordEncoder;
  private final TiposCadastroGateway tiposCadastro;

  public CadastrarUsuarioComumService(
    UsuarioGateway usuarioGateway,
    EnderecoGateway enderecoGateway,
    CepGateway cepGateway,
    PasswordEncoder passwordEncoder, TiposCadastroGateway tiposCadastro,
    ConsultaDocumentoGateway consultaDocumentoGateway
  ){
    this.usuarioGateway = usuarioGateway;
    this.enderecoGateway = enderecoGateway;
    this.cepGateway = cepGateway;
    this.passwordEncoder = passwordEncoder;
    this.tiposCadastro = tiposCadastro;
  }

  @Override
  @Transactional 
  public CadastroUsuarioComumOutput executar(String nomeCompleto, String email, String senha, LocalDate dataNascimento, String cpf,
      String cep, String numero, String complemento) {
    
    validarDataNascimento(dataNascimento);
    String emaiLimpo = email.trim().toLowerCase();
    String cpfLimpo = cpf.replaceAll("\\D","");

    if (usuarioGateway.existePorEmail(emaiLimpo)) {
      throw new AlreadyExistsException ("EMAIL_ALREADY_EXISTS", "Email já cadastrado");
    }
    if (usuarioGateway.existePorCpf(cpfLimpo)) {
      throw new AlreadyExistsException("CPF_ALREADY_EXISTS", "CPF já cadastrado");
    }
    Endereco enderecoCep = cepGateway.buscarEnderecoPorCep(cep);
    Endereco enderecoSalvo = enderecoGateway.salvar(new Endereco(
      null, enderecoCep.getCep(), enderecoCep.getEstado(), enderecoCep.getCidade(),
      enderecoCep.getLogradouro(), numero,complemento
    ));

    String senhaHash = passwordEncoder.encode(senha);
    Usuario usuario = new Usuario(
      null, nomeCompleto, emaiLimpo, senhaHash, dataNascimento, cpfLimpo,
      null,true,OffsetDateTime.now(), tiposCadastro.usuarioComum(), enderecoSalvo.getIdEndereco());
    Usuario usuarioSalvo = usuarioGateway.salvar(usuario);
    
    return new CadastroUsuarioComumOutput(usuarioSalvo, enderecoSalvo);
  }

  private void validarDataNascimento(LocalDate data){
    if (data.isAfter(LocalDate.now())) {
      throw new BusinessException("VALIDATION_ERROR", "Data de nascimento não pode ser futura");
    }
    if (Period.between(data, LocalDate.now()).getYears()<10) {
      throw new BusinessException("VALIDATION_ERROR", "Idade mínima para cadastro é de 10 anos");
    }
  }

  

  
}
