package com.cmbc.infras.health.contant;

/**
 * 评分方式
 */
public enum AssessmentTypeEnum {

    SINGLE(1, "single", "单项选择", "人工提交"),
    DEDUCT(2, "deduct", "累计扣分", "人工提交"),
    MONITOR(3, "monitor", "运行监控", "系统统计"),
    ANALYSIS(4, "analysis", "统计分析", "系统统计");

    private int id;
    private String name;//名称
    private String desc;//描述
    private String type;//统计形式

    AssessmentTypeEnum(int id, String name, String desc, String type) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.type = type;
    }

    /**
     * 根据名称返回描述
     */
    public static String getDesc(String name) {
        AssessmentTypeEnum[] values = AssessmentTypeEnum.values();
        for (AssessmentTypeEnum value : values) {
            if (value.name.equals(name)) return value.getDesc();
        }
        return null;
    }

    /**
     * 根据名称返回统计形式
     */
    public static String getType(String name) {
        AssessmentTypeEnum[] values = AssessmentTypeEnum.values();
        for (AssessmentTypeEnum value : values) {
            if (value.name.equals(name)) return value.getType();
        }
        return null;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
