package cn.court.mediation.enums;

/** 系统角色 */
public enum Role {
    WINDOW("立案窗口"),
    MEDIATOR("调解员"),
    PARTY("当事人"),
    ASSISTANT("法官助理"),
    EXECUTION("执行联络"),
    ADMIN("管理员");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
