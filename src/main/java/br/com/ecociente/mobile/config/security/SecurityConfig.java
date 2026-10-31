package br.com.ecociente.mobile.config.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        JwtService jwtService,
                        AuthenticationProvider authenticationProvider,
                        CorsConfigurationSource corsConfigurationSource) throws Exception {

                return http
                                .csrf(AbstractHttpConfigurer::disable)
                                .formLogin(AbstractHttpConfigurer::disable)
                                .httpBasic(AbstractHttpConfigurer::disable)
                                .logout(AbstractHttpConfigurer::disable)
                                .requestCache(AbstractHttpConfigurer::disable)

                                .cors(cors -> cors.configurationSource(
                                                corsConfigurationSource))

                                .sessionManagement(session -> session.sessionCreationPolicy(
                                                SessionCreationPolicy.STATELESS))

                                .authenticationProvider(
                                                authenticationProvider)

                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/swagger-ui.html",
                                                                "/swagger-ui/**",
                                                                "/api-docs",
                                                                "/api-docs/**")
                                                .permitAll()
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/auth/login")
                                                .permitAll()

                                                .requestMatchers(
                                                                HttpMethod.OPTIONS,
                                                                "/**")
                                                .permitAll()

                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/auth/login",
                                                                "/api/cadastro/comum",
                                                                "/api/cadastro/morador",
                                                                "/api/cadastro/cooperativa",
                                                                "/senhas/esqueceu",
                                                                "/senhas/redefinir")
                                                .permitAll()

                                                .anyRequest().authenticated())

                                .exceptionHandling(errors -> errors
                                                .authenticationEntryPoint(
                                                                (request, response, exception) -> response.sendError(
                                                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                                                "Autenticação necessária"))
                                                .accessDeniedHandler(
                                                                (request, response, exception) -> response.sendError(
                                                                                HttpServletResponse.SC_FORBIDDEN,
                                                                                "Acesso negado")))

                                .addFilterBefore(
                                                new JwtAuthenticationFilter(jwtService),
                                                UsernamePasswordAuthenticationFilter.class)

                                .build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationProvider authenticationProvider(
                        UsuarioDetailsService usuarioDetailsService,
                        PasswordEncoder passwordEncoder) {
                var provider = new DaoAuthenticationProvider(
                                usuarioDetailsService);

                provider.setPasswordEncoder(passwordEncoder);

                return provider;
        }

        @Bean
        public AuthenticationManager authenticationManager(
                        AuthenticationProvider authenticationProvider) {
                return new ProviderManager(
                                List.of(authenticationProvider));
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource(
                        @Value("${app.cors.origens:http://localhost:5173}") String origens) {
                var configuration = new CorsConfiguration();

                configuration.setAllowedOrigins(
                                Arrays.stream(origens.split(","))
                                                .map(String::trim)
                                                .filter(origem -> !origem.isEmpty())
                                                .toList());

                configuration.setAllowedMethods(
                                List.of(
                                                "GET",
                                                "POST",
                                                "PUT",
                                                "PATCH",
                                                "DELETE",
                                                "OPTIONS"));

                configuration.setAllowedHeaders(
                                List.of(
                                                "Authorization",
                                                "Content-Type",
                                                "Accept",
                                                "Idempotency-Key"));

                configuration.setExposedHeaders(
                                List.of("Location"));

                configuration.setAllowCredentials(false);
                configuration.setMaxAge(3600L);

                var source = new UrlBasedCorsConfigurationSource();

                source.registerCorsConfiguration(
                                "/**",
                                configuration);

                return source;
        }
}