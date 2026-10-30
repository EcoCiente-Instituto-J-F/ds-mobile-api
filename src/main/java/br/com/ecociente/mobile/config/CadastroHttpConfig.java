package br.com.ecociente.mobile.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CadastroHttpConfig {

  @Bean
  @ConditionalOnMissingBean(RestTemplate.class)
  public RestTemplate restTemplate() {
    var factory = new SimpleClientHttpRequestFactory();

    factory.setConnectTimeout(3000);
    factory.setReadTimeout(5000);

    return new RestTemplate(factory);
  }
}