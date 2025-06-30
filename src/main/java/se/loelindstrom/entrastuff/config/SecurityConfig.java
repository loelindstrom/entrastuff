package se.loelindstrom.entrastuff.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
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
//        // Define Microsoft IP ranges
//        List<String> microsoftGraphIpRanges = Arrays.asList(
//                "20.20.32.0/19",
//                "20.190.128.0/18",
//                "20.231.128.0/19",
//                "40.126.0.0/18"
//        );
//
//        // Create RequestMatcher for /api/webhook with IP whitelisting
//        RequestMatcher webhookMatcher = new RequestMatcher() {
//            private final String path = "/api/webhook";
//            private final List<IpAddressMatcher> ipMatchers = microsoftGraphIpRanges.stream()
//                    .map(IpAddressMatcher::new)
//                    .toList();
//
//            @Override
//            public boolean matches(HttpServletRequest request) {
//                // Check if the request is for /api/webhook
//                if (!request.getRequestURI().startsWith(path)) {
//                    return false;
//                }
//
//                logger.info("Received request to path: {}", path);
//
//                // Check if the request's IP is in the allowed ranges
//                final String remoteAddr = request.getRemoteAddr();
//                logger.info("Request coming from address: {}", remoteAddr);
//                return ipMatchers.stream().anyMatch(matcher -> matcher.matches(remoteAddr));
//            }
//        };

        // Configure security
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(authorize -> authorize
//                        .requestMatchers(webhookMatcher).permitAll() // Allow whitelisted IPs for /api/webhook
                        .requestMatchers("/api/webhook").permitAll() // Allow all calls to /api/webhookku - use clientState to authenticate
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