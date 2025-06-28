package se.loelindstrom.entrastuff.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import se.loelindstrom.entrastuff.config.TokenStore;
import se.loelindstrom.entrastuff.dtos.BackupDTO;
import se.loelindstrom.entrastuff.entities.Backup;
import se.loelindstrom.entrastuff.repositories.BackupRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BackupController {
    private static final Logger logger = LoggerFactory.getLogger(BackupController.class);
    private final TokenStore tokenStore;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final BackupRepository backupRepository;
    private final String tenantId;

    public BackupController(
            TokenStore tokenStore,
            BackupRepository backupRepository,
            @Value("${entra.tenant-id}") String tenantId
    ) {
        this.tokenStore = tokenStore;
        this.backupRepository = backupRepository;
        this.tenantId = tenantId;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @PostMapping("/backup-users")
    public ResponseEntity<String> backupUsers() {
        logger.info("Will fetch token.");
        String token = tokenStore.getAccessToken();
        logger.info("Fetched token.");

        try {
            logger.info("Will fetch all users in Entra.");
            List<JsonNode> allUsers = fetchAllUsers(token);
            logger.info("Fetched {} users from Microsoft Graph.", allUsers.size());

            logger.info("Saving users to database.");
            saveUsersToDatabase(allUsers);
            logger.info("Saved users to database.");

            return ResponseEntity.ok(objectMapper.writeValueAsString(allUsers));
        } catch (Exception e) {
            logger.error("Failed to process users: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Failed to process users: " + e.getMessage());
        }
    }

    @GetMapping("/backups")
    public ResponseEntity<String> getBackups() {
        try {
            List<BackupDTO> backups = backupRepository.findAllExcludingBackupData();
            return ResponseEntity.ok(objectMapper.writeValueAsString(backups));
        } catch (Exception e) {
            logger.error("Failed to fetch backups: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Failed to fetch backups.");
        }
    }

    private List<JsonNode> fetchAllUsers(String token) throws Exception {
        List<JsonNode> allUsers = new ArrayList<>();
        String url = "https://graph.microsoft.com/v1.0/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> request = new HttpEntity<>(headers);

        int count = 1;
        while (url != null) {
            logger.debug("Call number {} to users API.", count);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            logger.trace("Graph API request status: {}", response.getStatusCode());
            logger.trace("Graph API response body: {}", response.getBody());

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Graph API request failed with status: " + response.getStatusCode());
            }

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            JsonNode users = jsonResponse.get("value");
            if (users != null && users.isArray()) {
                for (JsonNode user : users) {
                    allUsers.add(user);
                }
            }

            url = jsonResponse.has("@odata.nextLink") ? jsonResponse.get("@odata.nextLink").asText() : null;
            String beginMsg = url != null ? "Next page url: " : "No more pages.";
            logger.debug("{} {}", beginMsg, url != null ? url : "");
            count++;
        }

        return allUsers;
    }

    private void saveUsersToDatabase(List<JsonNode> users) {
        ArrayNode usersArray = objectMapper.createArrayNode();
        users.forEach(usersArray::add);

        Backup backup = new Backup();
        backup.setTenantId(tenantId);
        backup.setDataType("user");
        backup.setBackupData(usersArray);
        backup.setCreatedAt(LocalDateTime.now());
        backup.setBackupType("entra");

        backupRepository.save(backup);
    }
}