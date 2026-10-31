package br.com.ecociente.mobile.config.notificacoes;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

@Configuration
public class PushFirebaseConfig {
    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseAuth firebaseAuth) {
        FirebaseApp app = FirebaseApp.getApps().stream()
                .filter(instancia -> "ecociente-mobile".equals(instancia.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Firebase não inicializado"));
        return FirebaseMessaging.getInstance(app);
    }
}
