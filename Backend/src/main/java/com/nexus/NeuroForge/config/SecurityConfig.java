package com.nexus.NeuroForge.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Required to use @PreAuthorize
public class SecurityConfig {

    @Autowired
    private CorsConfig corsConfig;

    @Autowired
    private UserSyncFilter userSyncFilter;

    @Value("${app.keycloak.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${app.keycloak.issuer}")
    private String issuer;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // 1. Disable CSRF for stateless REST APIs
                .csrf(csrf -> csrf.disable())

                // 2. Apply CORS config
                .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))

                // 3. Secure all endpoints
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )

                // 4. Wire up OAuth2 with our custom Keycloak Role Converter
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(keycloakJwtConverter())
                                .decoder(jwtDecoder())
                        )
                )

                // 5. Inject User Sync Filter
                .addFilterAfter(userSyncFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    /**
     * THE FIX: This acts as the translator between Keycloak and Spring Boot.
     * It extracts roles from Keycloak's "realm_access" folder and adds the mandatory "ROLE_" prefix.
     */
    @Bean
    public JwtAuthenticationConverter keycloakJwtConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();

            // Read the "realm_access" map from the token
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

            if (realmAccess != null && realmAccess.containsKey("roles")) {
                @SuppressWarnings("unchecked")
                Collection<String> roles = (Collection<String>) realmAccess.get("roles");

                for (String role : roles) {
                    // Spring Security strictly requires the "ROLE_" prefix for hasRole() checks!
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                }
            }
            return authorities;
        });

        return converter;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }
}