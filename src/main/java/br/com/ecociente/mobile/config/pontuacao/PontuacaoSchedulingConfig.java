package br.com.ecociente.mobile.config.pontuacao;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class PontuacaoSchedulingConfig {

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("pontuacao-job-");

        return scheduler;
    }
}