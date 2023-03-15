package com.cmbc.infras.constant;

public enum EventLevelEnum {

    URGENCY(1, "紧急"),
    SERIOUS(2, "严重"),
    IMPORTANT(3, "重要"),
    SECONDARY(4, "次要"),
    WARNING(5, "预警");

    private Integer code;
    private String desc;

    EventLevelEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDesc(Integer code) {
        if (code == null) {
            return "";
        }
        for (EventLevelEnum el : EventLevelEnum.values()) {
            if (el.code.equals(code)) {
                return el.desc;
            }
        }
        return "--";
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
