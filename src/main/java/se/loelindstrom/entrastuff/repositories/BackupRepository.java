package se.loelindstrom.entrastuff.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import se.loelindstrom.entrastuff.dtos.BackupDTO;
import se.loelindstrom.entrastuff.entities.Backup;

import java.util.List;

public interface BackupRepository extends JpaRepository<Backup, Long> {
    @Query("SELECT new se.loelindstrom.entrastuff.dtos.BackupDTO(b.id, b.tenantId, b.dataType, b.createdAt, b.backupType) FROM Backup b")
    List<BackupDTO> findAllExcludingBackupData();
}