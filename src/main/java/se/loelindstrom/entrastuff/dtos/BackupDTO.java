package se.loelindstrom.entrastuff.dtos;

import java.time.LocalDateTime;

public class BackupDTO {
    private final Long id;
    private final String tenantId;
    private final String dataType;
    private final LocalDateTime createdAt;
    private final String backupType;

    public BackupDTO(Long id, String tenantId, String dataType, LocalDateTime createdAt, String backupType) {
        this.id = id;
        this.tenantId = tenantId;
        this.dataType = dataType;
        this.createdAt = createdAt;
        this.backupType = backupType;
    }

    public Long getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getDataType() {
        return dataType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getBackupType() {
        return backupType;
    }
}