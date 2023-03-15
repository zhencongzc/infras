package com.cmbc.infras.health.contant;

public enum AlarmTypeEnum {

    INTERRUPT(0, "通信中断"),
    RECOVER(1, "告警恢复"),
    OVERTOP(2, "过高报警"),
    UNUSUAL(3, "不正常值"),
    ULTRALOW(4, "过低报警"),
    ERRDATA(5, "错误数据"),
    AFFIRM(6, "确认事件"),
    EVENT(7, "事件"),
    BREAKDOWN(21, "故障"),
    STOP_COLLECT(30, "停止采集");

    private Integer code;
    private String desc;

    AlarmTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }


    /**
     * 返回类型的描述
     */
    public static String getDesc(int code) {
        AlarmTypeEnum[] values = AlarmTypeEnum.values();
        for (AlarmTypeEnum value : values) {
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

