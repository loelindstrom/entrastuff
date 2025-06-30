package se.loelindstrom.entrastuff.entities;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit-logs")
@Data
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event", columnDefinition = "jsonb", nullable = false)
    private JsonNode eventData;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
