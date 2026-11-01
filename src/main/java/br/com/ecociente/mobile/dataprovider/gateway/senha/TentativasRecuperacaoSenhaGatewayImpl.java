package br.com.ecociente.mobile.dataprovider.gateway.senha;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.gateway.senha.TentativasRecuperacaoSenhaGateway;

/** Contador no Redis. A chave leva o SHA-256 do e-mail, para não guardar dado pessoal no Redis. */
@Component
public class TentativasRecuperacaoSenhaGatewayImpl implements TentativasRecuperacaoSenhaGateway {

  private static final String PREFIXO = "senha:recuperacao:falhas:";

  private final StringRedisTemplate redis;

  public TentativasRecuperacaoSenhaGatewayImpl(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public int falhas(String email) {
    String valor = redis.opsForValue().get(chave(email));
    return valor == null ? 0 : Integer.parseInt(valor);
  }

  @Override
  public int registrarFalha(String email, Duration janela) {
    String chave = chave(email);
    // SET NX EX cria a chave já com prazo; o INCR seguinte preserva o prazo.
    // Assim a chave nunca fica sem expiração, mesmo se a aplicação cair entre os dois comandos.
    redis.opsForValue().setIfAbsent(chave, "0", janela);
    Long total = redis.opsForValue().increment(chave);
    return total == null ? 0 : total.intValue();
  }

  @Override
  public void limpar(String email) {
    redis.delete(chave(email));
  }

  private static String chave(String email) {
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256")
          .digest(email.getBytes(StandardCharsets.UTF_8));
      return PREFIXO + HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException erro) {
      throw new IllegalStateException("SHA-256 indisponível", erro);
    }
  }
}
