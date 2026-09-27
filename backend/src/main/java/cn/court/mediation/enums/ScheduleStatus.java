package cn.court.mediation.enums;

/** 排期状态 */
public enum ScheduleStatus {
    PLANNED("已排期"),
    RESCHEDULED("已改期"),
    FINISHED("已完成"),
    CANCELLED("已取消");

    private final String label;

    ScheduleStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
