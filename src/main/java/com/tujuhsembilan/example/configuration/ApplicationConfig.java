package com.tujuhsembilan.example.configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.ResourceUtils;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.tujuhsembilan.example.configuration.property.AuthProp;

import lib.i18n.utility.MessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@EnableWebSecurity
public class ApplicationConfig {

  private final MessageUtil msg;

  @Bean
  public ApplicationRunner init() {
    return args -> {
      log.info(msg.get("application.init"));

      log.info(msg.get("application.done"));
    };
  }

  @Bean
  public ModelMapper modelMapper() {
    return new ModelMapper();
  }

  // --- Security Configuration

  @Bean
  public SecurityFilterChain securityConfig(HttpSecurity http, PasswordEncoder passwordEncoder, AuthProp prop)
      throws Exception {
    http
        // Access Control
        .authorizeHttpRequests(req -> req
            .requestMatchers(AntPathRequestMatcher.antMatcher("/auth/jwks.json")).permitAll()
            .requestMatchers(AntPathRequestMatcher.antMatcher("/auth/login")).permitAll()
            .anyRequest().authenticated())
        // Authorization (DEFAULT IN MEM)
        .userDetailsService(new InMemoryUserDetailsManager(
            User.builder()
                .username(prop.getSystemUsername())
                .password(prop.getSystemPassword())
                .build(),
            User.builder()
                .username("USER_A")
                .password(passwordEncoder.encode("USER_A"))
                .authorities("ROLE_A")
                .build(),
            User.builder()
                .username("USER_B")
                .password(passwordEncoder.encode("USER_B"))
                .authorities("ROLE_B")
                .build()))
        .httpBasic(Customizer.withDefaults())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Authentication
        .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
        // Miscellaneous
        .csrf(AbstractHttpConfigurer::disable);

    return http.build();
  }

  @Bean
  public ECKey ecJwk() throws IOException, ParseException {
    try (var in = new FileInputStream(ResourceUtils.getFile("classpath:key/ES512.json"))) {
      return ECKey.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
    }
  }

  @Bean
  public JWKSource<SecurityContext> jwkSource(ECKey jwk) {
    return new ImmutableJWKSet<>(new JWKSet(jwk));
  }

  // --- Authorization Resource Configuration

  @Bean
  public PasswordEncoder passwordEncoder(AuthProp prop) {
    return new BCryptPasswordEncoder(prop.getStrength());
  }

  @Bean
  public JwtEncoder jwtEncoder(JWKSource<SecurityContext> source) {
    return new NimbusJwtEncoder(source);
  }

  // --- OAuth2 Resource Server Configuration

  @Bean
  public JwtDecoder jwtDecoder(JWKSource<SecurityContext> source, AuthProp prop) {
    NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder) OAuth2AuthorizationServerConfiguration.jwtDecoder(source);

    jwtDecoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(prop.getUuid()),
            jwt -> OAuth2TokenValidatorResult.success()));

    return jwtDecoder;
  }

}
