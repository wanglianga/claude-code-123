package cn.court.mediation.enums;

/** 案由类型 */
public enum CaseType {
    LOAN("民间借贷纠纷"),
    PROPERTY_FEE("物业费纠纷"),
    TRAFFIC("交通事故纠纷"),
    NEIGHBOR("邻里纠纷"),
    SALES("小额买卖合同纠纷");

    private final String label;

    CaseType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
