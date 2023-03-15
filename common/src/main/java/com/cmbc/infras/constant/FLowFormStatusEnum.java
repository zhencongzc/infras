package com.cmbc.infras.constant;

/**
 * 流程表单状态对应
 * 生产测试环境配置相同
 * 如果不同需修改
 */
public enum FLowFormStatusEnum {
    /**
     * id是业务标识
     * 查询时用value,label
     */
    DRAFT(10,"1d09d9df7bcc4654bd47f5b47c96fe88", "草搞"),
    GENERATE(20,"de5890e0198a4534a84e07201d67b6e9", "已生成"),
    TO_AUDIT(30, "72024d4041e845f694db0d675b947550", "待审核"),
    TO_CHECK(40,"e4810cfe9eb54beaa7d2951dfae32ce5", "待核验"),
    TO_CONFIRM(50,"188f310a52824e86a67115255c1bb1f5", "待确认"),
    CLOSED(60,"043afea56cdd4939b0b72a462c282cd9", "已关闭");

    private Integer id;
    private String value;
    private String label;

    FLowFormStatusEnum(Integer id, String value, String label) {
        this.id = id;
        this.value = value;
        this.label = label;
    }

    public static String getValue(Integer id) {
        if (id == null) {
            return "";
        }
        for (FLowFormStatusEnum iterm : FLowFormStatusEnum.values()) {
            if (iterm.id.equals(id)) {
                return iterm.value;
            }
        }
        return "--";
    }

    public static String getLabel(Integer id) {
        if (id == null) {
            return "";
        }
        for (FLowFormStatusEnum iterm : FLowFormStatusEnum.values()) {
            if (iterm.id.equals(id)) {
                return iterm.label;
            }
        }
        return "--";
    }

    public Integer getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}
