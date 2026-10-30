package br.com.ecociente.mobile.dataprovider.gateway.autenticacao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.autenticacao.Autenticacao;
import br.com.ecociente.mobile.core.gateway.autenticacao.AutenticacaoGateway;
import br.com.ecociente.mobile.dataprovider.entity.autenticacao.AutenticacaoEntity;
import br.com.ecociente.mobile.dataprovider.mapper.autenticacao.AutenticacaoPersistenceMapper;
import br.com.ecociente.mobile.dataprovider.repository.autenticacao.AutenticacaoRepository;
import lombok.AllArgsConstructor;

@Component 
@AllArgsConstructor 
public class AutenticacaoGatewayImpl implements AutenticacaoGateway {
  private final AutenticacaoRepository autenticacaoRepository;
  private final AutenticacaoPersistenceMapper mapper;

  @Override
  public Autenticacao salvar(Autenticacao autenticacao) {
    AutenticacaoEntity entity = mapper.toEntity(autenticacao);
    entity.setToken(hashToken(entity.getToken()));
    AutenticacaoEntity salvar = autenticacaoRepository.save(entity);
    return mapper.toDomain(salvar);
  }

  private String hashToken(String token){
    try{
      var digest = MessageDigest.getInstance("SHA-256");
      var hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      throw new IllegalStateException("Erro ao gerar hash do token", e);
    }
  }
}
