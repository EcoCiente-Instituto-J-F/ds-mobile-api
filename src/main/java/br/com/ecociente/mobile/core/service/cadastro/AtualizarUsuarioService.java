package br.com.ecociente.mobile.core.service.cadastro;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.cadastro.AtualizacaoCondominio;
import br.com.ecociente.mobile.core.domain.cadastro.AtualizacaoDados;
import br.com.ecociente.mobile.core.domain.cadastro.Endereco;
import br.com.ecociente.mobile.core.domain.cadastro.UsuarioEdicao;
import br.com.ecociente.mobile.core.exception.cadastro.AlreadyExistsException;
import br.com.ecociente.mobile.core.exception.cadastro.AtualizacaoNaoPermitidaException;
import br.com.ecociente.mobile.core.exception.cadastro.BusinessException;
import br.com.ecociente.mobile.core.gateway.cadastro.AtualizacaoUsuarioGateway;
import br.com.ecociente.mobile.core.gateway.cadastro.CepGateway;

@Service
public class AtualizarUsuarioService {

  private final AtualizacaoUsuarioGateway gateway;
  private final CepGateway cepGateway;

  public AtualizarUsuarioService(
      AtualizacaoUsuarioGateway gateway,
      CepGateway cepGateway) {
    this.gateway = gateway;
    this.cepGateway = cepGateway;
  }

  @Transactional
  public void atualizarDados(
      Integer usuarioId,
      AtualizacaoDados entrada) {
    UsuarioEdicao usuario = buscarUsuario(usuarioId);

    if (entrada.nome() == null
        && entrada.email() == null
        && entrada.dataNascimento() == null
        && entrada.urlAvatar() == null
        && entrada.telefone() == null) {
      throw new BusinessException(
          "EMPTY_UPDATE",
          "Informe pelo menos um campo para alterar");
    }

    String email = entrada.email() == null
        ? null
        : entrada.email().trim().toLowerCase(Locale.ROOT);

    if (email != null
        && gateway.emailPertenceAOutroUsuario(email, usuarioId)) {
      throw new AlreadyExistsException(
          "EMAIL_ALREADY_EXISTS",
          "E-mail já pertence a outro usuário");
    }

    validarNascimento(usuario, entrada.dataNascimento());

    if (entrada.telefoneId() != null
        && entrada.telefone() == null) {
      throw new BusinessException(
          "PHONE_REQUIRED",
          "Informe o telefone que deseja alterar");
    }

    if (entrada.telefone() != null) {
      atualizarTelefone(usuarioId, entrada);
    }

    gateway.atualizarDados(
        usuarioId,
        new AtualizacaoDados(
            entrada.nome() == null ? null : entrada.nome().trim(),
            email,
            entrada.dataNascimento(),
            entrada.urlAvatar(),
            null,
            null));
  }

  @Transactional
  public void atualizarMeuEndereco(
      Integer usuarioId,
      String cep,
      String numero,
      String complemento) {
    UsuarioEdicao usuario = buscarUsuario(usuarioId);
    String tipo = normalizarTipo(usuario.tipo());
    Integer enderecoId;

    if (tipo.contains("cooperativa")) {
      enderecoId = gateway.buscarEnderecoCooperativa(usuarioId)
          .orElseThrow(() -> proibido(
              "Cooperativa vinculada ao usuário não encontrada"));

      if (!Objects.equals(enderecoId, usuario.enderecoId())) {
        throw new BusinessException(
            "ADDRESS_LINK_INCONSISTENT",
            "O endereço do usuário e da cooperativa está divergente");
      }

    } else if (ehComum(tipo)) {
      enderecoId = usuario.enderecoId();

      if (enderecoId == null
          || !gateway.enderecoExclusivoDoUsuario(
              usuarioId, enderecoId)) {
        throw proibido(
            "O endereço está compartilhado e não pode ser alterado por este endpoint");
      }

    } else {
      throw proibido(
          "Este usuário não pode alterar o endereço por este endpoint");
    }

    salvarEndereco(enderecoId, cep, numero, complemento);
  }

  @Transactional
  public void atualizarEnderecoCondominio(
      Integer usuarioId,
      Integer condominioId,
      String cep,
      String numero,
      String complemento) {
    UsuarioEdicao usuario = buscarUsuario(usuarioId);
    String tipo = normalizarTipo(usuario.tipo());

    if (!tipo.contains("sindico")) {
      throw proibido(
          "Somente o síndico responsável pode alterar o endereço do condomínio");
    }

    Integer enderecoId = gateway
        .buscarEnderecoCondominioDoSindico(
            usuarioId,
            condominioId)
        .orElseThrow(() -> proibido(
            "Você não é responsável por este condomínio ou ele não possui endereço válido"));

    salvarEndereco(enderecoId, cep, numero, complemento);
  }

  @Transactional
  public void atualizarCondominio(
      Integer usuarioId,
      Integer condominioId,
      AtualizacaoCondominio entrada) {
    UsuarioEdicao usuario = buscarUsuario(usuarioId);
    String tipo = normalizarTipo(usuario.tipo());

    if (!tipo.contains("sindico")) {
      throw proibido(
          "Somente o síndico responsável pode alterar o condomínio");
    }

    if (!gateway.condominioAtivoDoSindico(usuarioId, condominioId)) {
      throw proibido(
          "Você não é responsável por este condomínio ou ele está inativo");
    }

    if (entrada.nome() == null
        && entrada.tipoCondominioId() == null
        && entrada.urlFoto() == null) {
      throw new BusinessException(
          "EMPTY_UPDATE",
          "Informe pelo menos um campo para alterar");
    }

    if (entrada.tipoCondominioId() != null
        && !gateway.tipoCondominioExiste(entrada.tipoCondominioId())) {
      throw new BusinessException(
          "CONDOMINIUM_TYPE_NOT_FOUND",
          "Tipo de condomínio não encontrado");
    }

    gateway.atualizarCondominio(
        condominioId,
        new AtualizacaoCondominio(
            entrada.nome() == null ? null : entrada.nome().trim(),
            entrada.tipoCondominioId(),
            entrada.urlFoto()));
  }

  private UsuarioEdicao buscarUsuario(Integer usuarioId) {
    return gateway.buscarUsuarioAtivo(usuarioId)
        .orElseThrow(() -> proibido(
            "Usuário não encontrado ou inativo"));
  }

  private void atualizarTelefone(
      Integer usuarioId,
      AtualizacaoDados dados) {
    var telefones = gateway.buscarTelefonesAtivos(usuarioId);
    Integer telefoneId = dados.telefoneId();

    if (telefoneId != null) {
      if (!telefones.contains(telefoneId)) {
        throw proibido(
            "Telefone não pertence ao usuário ou está inativo");
      }

      gateway.atualizarTelefone(
          usuarioId, telefoneId, dados.telefone());

    } else if (telefones.isEmpty()) {
      gateway.criarTelefone(usuarioId, dados.telefone());

    } else if (telefones.size() == 1) {
      gateway.atualizarTelefone(
          usuarioId, telefones.get(0), dados.telefone());

    } else {
      throw new BusinessException(
          "PHONE_ID_REQUIRED",
          "Há mais de um telefone. Informe telefoneId.");
    }
  }

  private void validarNascimento(
      UsuarioEdicao usuario,
      LocalDate nascimento) {
    if (nascimento == null) {
      return;
    }

    if (!nascimento.isBefore(LocalDate.now())) {
      throw new BusinessException(
          "BIRTH_DATE_INVALID",
          "Data de nascimento deve ser anterior a hoje");
    }

    String tipo = normalizarTipo(usuario.tipo());
    int idade = Period.between(
        nascimento,
        LocalDate.now()).getYears();

    if (ehComum(tipo) && idade < 18) {
      throw new BusinessException(
          "BIRTH_DATE_INVALID",
          "Usuário comum deve ter pelo menos 18 anos");
    }

    if (tipo.contains("morador") && idade < 10) {
      throw new BusinessException(
          "BIRTH_DATE_INVALID",
          "Morador deve ter pelo menos 10 anos");
    }
  }

  private void salvarEndereco(
      Integer enderecoId,
      String cep,
      String numero,
      String complemento) {
    Endereco consultado = cepGateway.buscarEnderecoPorCep(
        cep.replace("-", ""));

    gateway.atualizarEndereco(
        new Endereco(
            enderecoId,
            consultado.getCep(),
            consultado.getEstado(),
            consultado.getCidade(),
            consultado.getLogradouro(),
            numero.trim(),
            complemento));
  }

  private boolean ehComum(String tipo) {
    return tipo.equals("comum")
        || tipo.equals("usuario comum");
  }

  private String normalizarTipo(String tipo) {
    return Normalizer.normalize(
        tipo == null ? "" : tipo,
        Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .replace('_', ' ')
        .trim()
        .toLowerCase(Locale.ROOT);
  }

  private AtualizacaoNaoPermitidaException proibido(
      String mensagem) {
    return new AtualizacaoNaoPermitidaException(mensagem);
  }

  // Exclusão de conta: desativa o usuário (a ds-autenticacao-api recusa o login de quem está inativo).
  @Transactional
  public void desativar(Integer usuarioId) {
    gateway.desativarUsuario(usuarioId);
  }
}