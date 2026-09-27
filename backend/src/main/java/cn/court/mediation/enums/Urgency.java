package cn.court.mediation.enums;

/** 紧急程度 */
public enum Urgency {
    NORMAL("普通"),
    URGENT("加急"),
    EMERGENCY("特急");

    private final String label;

    Urgency(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
