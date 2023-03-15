package com.cmbc.infras.constant;

public enum AlarmStatisEnum {

    MAIN(1, "主机房"),
    BRANCH(2,"二级分行"),
    SUB(3, "支行"),
    TOWN(4, "村镇");

    private Integer code;
    private String desc;

    AlarmStatisEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDesc(Integer code) {
        if (code == null) {
            return "";
        }
        for (AlarmStatisEnum el : AlarmStatisEnum.values()) {
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
