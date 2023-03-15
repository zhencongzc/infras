package com.cmbc.infras.constant;

/**
 * 告警类型
 * 空调状态、温湿度状态
 */
public enum AirStateEnum {

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

    private int state;
    private String desc;


    AirStateEnum(int state, String desc) {
        this.state = state;
        this.desc = desc;
    }

    /**
     * 0:通信中断
     * 1:正常
     * 2~21:告警
     * 30:停止采集
     */
    public static String getDesc(Integer id) {
        if (id == 0) return "通信中断";
        if (id == 1) return "正常";
        if (1 < id && id <= 21) return "告警";
        if (id == 30) return "停止采集";
        return "--";
    }

    public static String getDescription(Integer state) {
        if (state == null) return "";
        for (AirStateEnum el : AirStateEnum.values()) {
            if (el.state == state) {
                return el.desc;
            }
        }
        return "--";
    }
}
