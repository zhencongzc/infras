package com.cmbc.infras.health.contant;

public enum ConfirmTypeEnum {

    EMPTY(0, ""),
    REAL(1, "真实告警"),
    TEST(1, "测试告警"),
    ERROR(2, "误告警");

    private Integer code;
    private String desc;

    ConfirmTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 返回类型的描述
     */
    public static String getDesc(int code) {
        ConfirmTypeEnum[] values = ConfirmTypeEnum.values();
        for (ConfirmTypeEnum value : values) {
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
