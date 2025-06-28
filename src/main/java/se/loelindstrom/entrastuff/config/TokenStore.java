package se.loelindstrom.entrastuff.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.time.Instant;

@Component
public class TokenStore {
    private static final Logger logger = LoggerFactory.getLogger(TokenStore.class);
    private final String tenantId;
    private final String clientId;
    private final String clientSecret;
    private String accessToken;
    private Instant tokenExpiry;
    private final ObjectMapper objectMapper;

    public TokenStore(
            @Value("${entra.tenant-id}") String tenantId,
            @Value("${entra.client-id}") String clientId,
            @Value("${entra.client-secret}") String clientSecret
    ) {
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        logger.info("Fetching token for Entra as part of init. Will cache it.");
        refreshToken();
    }

    public synchronized String getAccessToken() {
        logger.debug("Getting access token");
        if (accessToken == null || tokenExpiry.isBefore(Instant.now())) {
            logger.debug("Need to refresh access token.");
            refreshToken();
        } else {
            logger.debug("Reusing cached access token.");
        }
        return accessToken;
    }

    private void refreshToken() {
        RestTemplate restTemplate = new RestTemplate();
        String tokenUrl = String.format("https://login.microsoftonline.com/%s/oauth2/v2.0/token", tenantId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "client_credentials");
        body.add("scope", "https://graph.microsoft.com/.default");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                this.accessToken = jsonNode.get("access_token").asText();
                this.tokenExpiry = Instant.now().plusSeconds(jsonNode.get("expires_in").asLong() - 300); // Refresh 5 min early
                logger.debug("Successfully retrieved an access token. It expires in {} seconds", this.tokenExpiry);
            } else {
                throw new RuntimeException("Token request failed with status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Failed to fetch Entra token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch Entra token", e);
        }
    }
}