package com.cmbc.infras.system.constant;

public enum BankLevelEnum {

    ZONG_HANG(0, "总行"),
    FEN_HANG(1, "分行"),
    ERJI_FENHANG(2, "二级分行"),
    ZHI_HANG(3, "支行"),
    CUNZHEN_YINHANG(4, "村镇银行");

    private Integer code;
    private String desc;

    BankLevelEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 返回类型的code
     */
    public static int getCode(String desc) {
        BankLevelEnum[] values = BankLevelEnum.values();
        for (BankLevelEnum value : values) {
            if (value.desc.equals(desc)) return value.getCode();
        }
        return -1;
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
