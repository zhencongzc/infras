package com.cmbc.infras.constant;


/**
 * 恢复状态
 */
public enum RecoveryEnum {

    NOT_RECOVERY(0, "未恢复"),
    RECOVERY(1, "已恢复");

    private Integer code;
    private String desc;

    RecoveryEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDesc(Integer code) {
        if (code == null) {
            return "";
        }
        for (RecoveryEnum el : RecoveryEnum.values()) {
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
