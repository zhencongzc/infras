package com.cmbc.infras.health.contant;

public enum AlarmStateEnum {

    UNPROCESSED(0, "未处理"),
    PROCESSIND(1, "处理中"),
    PROCESSED(2, "已处理");

    private Integer code;
    private String desc;

    AlarmStateEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 返回类型的描述
     */
    public static String getDesc(int code) {
        AlarmStateEnum[] values = AlarmStateEnum.values();
        for (AlarmStateEnum value : values) {
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
