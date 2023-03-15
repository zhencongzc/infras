package com.cmbc.infras.constant;

/**
 * 告警状态-DicConstant
 */
public enum StateEnum {

    NOT_PROCESS(0, "未处理"),
    PROCESSING(1, "处理中"),
    PROCESSED(2, "已处理");

    private Integer code;
    private String desc;

    StateEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDesc(Integer code) {
        if (code == null) {
            return "";
        }
        for (StateEnum el : StateEnum.values()) {
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
