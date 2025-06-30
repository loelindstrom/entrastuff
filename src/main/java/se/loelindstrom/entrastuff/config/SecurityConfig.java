package se.loelindstrom.entrastuff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final String username;
    private final String password;

    public SecurityConfig(
            @Value("${auth.username}") String username,
            @Value("${auth.password}") String password
    ) {
        this.username = username;
        this.password = password;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Don't protect webhook endpoint if validation token is sent:
        RequestMatcher validationTokenMatcher = request ->
                request.getRequestURI().startsWith("/api/webhook") &&
                        request.getParameter("validationToken") != null;

        // Protect all endpoints by default (except when validation token):
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(validationTokenMatcher).permitAll() // Allow validation requests
                        .requestMatchers("/api/**").authenticated() // Require auth for all other /api/**
                        .anyRequest().denyAll()
                )
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/webhook", "/api/backup-users", "/api/restore-users/**", "/api/create-subscription")
                );
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}