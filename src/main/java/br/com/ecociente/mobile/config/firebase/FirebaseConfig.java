package br.com.ecociente.mobile.config.firebase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;

@Configuration
public class FirebaseConfig {

  private static final String NOME_APP = "ecociente-mobile";

  @Bean
  public FirebaseAuth firebaseAuth(
      @Value("${app.firebase.credenciais}") String credenciais)
      throws IOException {

    var existente = FirebaseApp.getApps().stream()
        .filter(app -> NOME_APP.equals(app.getName()))
        .findFirst();

    if (existente.isPresent()) {
      return FirebaseAuth.getInstance(existente.get());
    }

    try (var entrada = new ByteArrayInputStream(
        credenciais.getBytes(StandardCharsets.UTF_8))) {

      var options = FirebaseOptions.builder()
          .setCredentials(
              GoogleCredentials.fromStream(entrada))
          .build();

      var app = FirebaseApp.initializeApp(options, NOME_APP);

      return FirebaseAuth.getInstance(app);
    }
  }
}