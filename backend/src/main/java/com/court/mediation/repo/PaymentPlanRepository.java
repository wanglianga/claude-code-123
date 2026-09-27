package com.court.mediation.repo;

import com.court.mediation.entity.PaymentPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PaymentPlanRepository extends JpaRepository<PaymentPlan, Long> {
    List<PaymentPlan> findByCaseIdOrderByPeriodNoAsc(Long caseId);

    List<PaymentPlan> findByAgreementIdOrderByPeriodNoAsc(Long agreementId);

    List<PaymentPlan> findByStatusAndDueDateBefore(String status, LocalDate date);
}
