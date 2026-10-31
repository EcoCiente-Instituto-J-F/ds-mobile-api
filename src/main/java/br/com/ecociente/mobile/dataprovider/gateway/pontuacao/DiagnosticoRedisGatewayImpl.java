package br.com.ecociente.mobile.dataprovider.gateway.pontuacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.gateway.pontuacao.DiagnosticoRedisGateway;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DiagnosticoRedisGatewayImpl
    implements DiagnosticoRedisGateway {

  private static final Logger log = LoggerFactory.getLogger(DiagnosticoRedisGatewayImpl.class);

  private final StringRedisTemplate redisTemplate;

  @Override
  public boolean estaDisponivel() {
    try {
      Boolean resposta = redisTemplate.execute(
          (RedisCallback<Boolean>) connection -> "PONG".equalsIgnoreCase(connection.ping()));

      return Boolean.TRUE.equals(resposta);
    } catch (RuntimeException exception) {
      log.warn(
          "Redis indisponível durante consulta de diagnóstico",
          exception);

      return false;
    }
  }
}