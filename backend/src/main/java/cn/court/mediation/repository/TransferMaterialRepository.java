package cn.court.mediation.repository;

import cn.court.mediation.entity.TransferMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferMaterialRepository extends JpaRepository<TransferMaterial, Long> {
    Optional<TransferMaterial> findByCaseId(Long caseId);
}
