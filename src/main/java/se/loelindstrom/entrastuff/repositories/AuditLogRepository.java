package se.loelindstrom.entrastuff.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import se.loelindstrom.entrastuff.entities.Backup;

public interface AuditLogRepository extends JpaRepository<Backup, Long> {

}