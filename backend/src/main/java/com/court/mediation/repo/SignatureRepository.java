package com.court.mediation.repo;

import com.court.mediation.entity.Signature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SignatureRepository extends JpaRepository<Signature, Long> {
    List<Signature> findByAgreementIdOrderBySignedAtAsc(Long agreementId);

    long countByAgreementIdAndFaceVerifiedTrue(Long agreementId);
}
