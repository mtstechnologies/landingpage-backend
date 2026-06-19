# SPEC-007 — Segurança Backend: CORS + Headers + Credenciais por env var

## Contexto
Análise do código real identificou três gaps de segurança no backend:

1. `SecurityConfig.java` sem CORS configurado — qualquer origem pode chamar endpoints admin
2. Sem security headers HTTP (X-Frame-Options, X-Content-Type-Options, etc.)
3. `application.yml` com `username: postgres` e `password: postgres` hardcoded —
   credenciais vão para o repositório Git em texto plano

## Pré-requisito
SPEC-006 executada (GET /tecnologias adicionado).

---

## Arquivos a modificar

### 1. `src/main/resources/application.yml`

#### Substituição exata — datasource

Localizar:
```yaml
  datasource:
    url: jdbc:postgresql://localhost:5432/portfolio_db
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
```

Substituir por:
```yaml
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/portfolio_db}
    username: ${SPRING_DATASOURCE_USERNAME:postgres}
    password: ${SPRING_DATASOURCE_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
```

#### Substituição exata — redis

Localizar:
```yaml
  data:
    redis:
      host: localhost
      port: 6379
```

Substituir por:
```yaml
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:localhost}
      port: ${SPRING_DATA_REDIS_PORT:6379}
```

#### Adicionar ao final do arquivo — configuração de segurança e CORS

```yaml
# Segurança
portfolio:
  security:
    cors:
      allowed-origins: ${PORTFOLIO_CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173}
  admin:
    username: ${PORTFOLIO_ADMIN_USER:admin}
    password: ${PORTFOLIO_ADMIN_PASSWORD:troque-em-producao}

# Actuator (preparação para SPEC-009)
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: never
```

---

### 2. `src/main/java/com/portfolio/backend/config/SecurityConfig.java`

Substituir o arquivo inteiro pelo seguinte:

```java
package com.portfolio.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${portfolio.security.cors.allowed-origins:http://localhost:3000}")
    private String allowedOriginsRaw;

    @Value("${portfolio.admin.username:admin}")
    private String adminUsername;

    @Value("${portfolio.admin.password:troque-em-producao}")
    private String adminPassword;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(content -> {})
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
                .referrerPolicy(referrer ->
                    referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/portfolio/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/v1/admin/portfolio/**").authenticated()
                .anyRequest().permitAll()
            )
            .httpBasic(httpBasic -> {});

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.asList(allowedOriginsRaw.split(","));
        config.setAllowedOrigins(origins.stream().map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "Idempotency-Key",
            "X-Trace-Id"
        ));
        config.setExposedHeaders(List.of("Location", "X-Trace-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails admin = User.builder()
            .username(adminUsername)
            .password(encoder.encode(adminPassword))
            .authorities("PORTFOLIO_ESCRITA")
            .build();
        return new InMemoryUserDetailsManager(admin);
    }
}
```

---

### 3. Atualizar `.env.example` — adicionar variáveis novas

Adicionar ao final do arquivo existente:

```dotenv
# CORS — origens permitidas (separadas por vírgula)
PORTFOLIO_CORS_ALLOWED_ORIGINS=http://localhost:3000,https://seudominio.com
```

---

## Comportamento esperado após execução

```bash
# 1. Credenciais por env var — app sobe sem credenciais hardcoded
./mvnw spring-boot:run
# Conecta normalmente usando os defaults do application.yml

# 2. CORS bloqueando origem não autorizada
curl -H "Origin: https://atacante.com" \
     -H "Access-Control-Request-Method: POST" \
     -X OPTIONS \
     http://localhost:8080/api/v1/admin/portfolio/projetos -v
# → Sem header Access-Control-Allow-Origin na resposta (origem bloqueada)

# 3. CORS permitindo origem do frontend
curl -H "Origin: http://localhost:3000" \
     -H "Access-Control-Request-Method: GET" \
     -X OPTIONS \
     http://localhost:8080/api/v1/portfolio/perfil -v
# → Access-Control-Allow-Origin: http://localhost:3000

# 4. Security headers presentes
curl -I http://localhost:8080/api/v1/portfolio/perfil
# → X-Content-Type-Options: nosniff
# → X-Frame-Options: DENY
# → Strict-Transport-Security: max-age=31536000 ; includeSubDomains

# 5. Autenticação ainda funciona
curl -u "admin:troque-em-producao" \
     -H "Idempotency-Key: test-123" \
     http://localhost:8080/api/v1/admin/portfolio/tecnologias
# → 200 OK
```

## O que NÃO fazer
- Não adicionar rate limiting nesta spec — complexidade desnecessária agora
- Não migrar para JWT — HTTP Basic com HTTPS é suficiente para este caso de uso
- Não alterar `IdempotencyInterceptor.java` ou `MdcFilter.java`
- Não remover `@PreAuthorize` dos controllers — manter a defesa em profundidade
- Não alterar nenhum arquivo de teste
