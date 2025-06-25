package se.loelindstrom.entrastuff.db.entities;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "backups")
@Data
public class BackupRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "data_type", nullable = false)
    private String dataType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "backup_data", columnDefinition = "jsonb", nullable = false)
    private JsonNode backupData;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "backup_type", nullable = false)
    private String backupType;
}
