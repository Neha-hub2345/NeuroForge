package com.nexus.NeuroForge.config;

// Ensure this import matches wherever you placed the CustomRoleConverter
import com.nexus.NeuroForge.config.CustomRoleConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Required to use @PreAuthorize
public class SecurityConfig {

    @Autowired
    private  CorsConfig corsConfig;
    @Autowired
    private  UserSyncFilter userSyncFilter;
    @Autowired
    private CustomRoleConverter customRoleConverter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // 1. Initialize the converter with your custom role logic
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(customRoleConverter);

        http
                // 2. Disable CSRF (standard for stateless REST APIs using Bearer tokens)
                .csrf(csrf -> csrf.disable())

                // 3. Apply your exact CORS configuration
                .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))

                // 4. Secure all endpoints
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )

                // 5. Wire up the OAuth2 Resource Server with the custom JWT converter
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                )

                // 6. Inject the Just-In-Time database sync filter after token validation
                .addFilterAfter(userSyncFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }
}