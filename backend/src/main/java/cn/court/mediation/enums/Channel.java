package cn.court.mediation.enums;

/** 调解渠道 */
public enum Channel {
    ONLINE("线上调解"),
    OFFLINE("线下调解");

    private final String label;

    Channel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
