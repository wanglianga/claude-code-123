package cn.court.mediation.dto;

import cn.court.mediation.enums.CaseType;
import cn.court.mediation.enums.Urgency;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CaseCreateRequest {
    private CaseType caseType;
    private Urgency urgency = Urgency.NORMAL;
    private String applicantName;
    private String applicantPhone;
    private Long applicantUserId;
    private String respondentName;
    private String respondentPhone;
    private String respondentAddress;
    private Boolean respondentOutOfTown = false;
    private BigDecimal claimAmount;
    private String claimSummary;
    private String evidenceList;
    private Boolean onlineWilling = true;
}
