package cn.court.mediation.enums;

/** 风险预警类型 */
public enum RiskFlag {
    RESPONDENT_MISSING("被申请人失联"),
    EVIDENCE_MISSING("证据材料缺失"),
    REPEAT_RESCHEDULE("多次改期"),
    INSTALLMENT("分期履行"),
    OVERDUE("付款逾期"),
    TRANSFER_REQUESTED("一方申请转立案");

    private final String label;

    RiskFlag(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
