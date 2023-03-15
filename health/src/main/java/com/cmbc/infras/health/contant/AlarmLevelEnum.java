package com.cmbc.infras.health.contant;

public enum AlarmLevelEnum {

    URGENCY(1, "紧急告警"),
    SERIOUS(2, "严重告警"),
    IMPORTANT(3, "重要告警"),
    SECONDARY(4, "次要告警"),
    WARNING(5, "预警告警");

    private Integer code;
    private String desc;

    AlarmLevelEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 判断传入的参数是否存在
     *
     * @param name
     * @return
     */
    public static boolean belongAlarmType(String name) {
        for (AlarmLevelEnum type : AlarmLevelEnum.values()) {
            if (type.desc.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 返回类型的描述
     */
    public static String getDesc(int code) {
        AlarmLevelEnum[] values = AlarmLevelEnum.values();
        for (AlarmLevelEnum value : values) {
            if (value.code == code) return value.getDesc();
        }
        return "";
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
