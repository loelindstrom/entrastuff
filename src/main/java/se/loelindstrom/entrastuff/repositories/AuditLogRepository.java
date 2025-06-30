package se.loelindstrom.entrastuff.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import se.loelindstrom.entrastuff.entities.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

}