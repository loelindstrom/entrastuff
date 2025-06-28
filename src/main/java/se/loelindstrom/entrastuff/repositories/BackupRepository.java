package se.loelindstrom.entrastuff.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import se.loelindstrom.entrastuff.entities.Backup;

public interface BackupRepository extends JpaRepository<Backup, Long> {
}