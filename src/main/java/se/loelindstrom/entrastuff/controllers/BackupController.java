package se.loelindstrom.entrastuff.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.loelindstrom.entrastuff.config.TokenStore;

@RestController
@RequestMapping("/api")
public class BackupController {
    private static final Logger logger = LoggerFactory.getLogger(BackupController.class);
    private final TokenStore tokenStore;

    public BackupController(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @GetMapping("/backup-users")
    public ResponseEntity<String> backupUsers() {
        logger.info("Will fetch token.");
        String token = tokenStore.getAccessToken();
        logger.info("Fetched token.");

        // Call out in a for loop (one call per page) and fetch all users in entra via Graph API
        // add code...

        // Persist users in with package se.loelindstrom.entrastuff.entities.Backup entity and package se.loelindstrom.entrastuff.repositories.BackupRepository
        // add code...

        return ResponseEntity.ok("Token fetched successfully: " + token);
    }
}