package com.court.mediation.dto;

import com.court.mediation.entity.*;

import java.util.List;

/** 案件详情聚合：同一案件各环节材料一次性返回给前端 */
public record CaseDetail(
        CaseEntity caseInfo,
        User mediator,
        List<Evidence> evidences,
        List<Schedule> schedules,
        List<MediationRecord> mediationRecords,
        Agreement activeAgreement,
        List<Agreement> agreements,
        List<PaymentPlan> paymentPlans,
        List<Signature> signatures,
        List<ServiceRecord> services,
        List<VisitRecord> visits,
        List<TransferRequest> transfers,
        List<JudicialConfirm> judicialConfirms,
        List<TimelineEvent> timeline
) {
}
