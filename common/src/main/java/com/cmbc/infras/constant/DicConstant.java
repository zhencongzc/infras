package com.cmbc.infras.constant;

/**
 * 状态字典类
 * -StateEnum
 * -EventLevelEnum
 */
public class DicConstant {

    //告警状态-未处理
    public static final int EVENT_NOT_PROCESS = 0;
    //告警状态-处理中
    public static final int EVENT_PROCESSING = 1;
    //告警状态-已处理
    public static final int EVENT_PROCESSED = 2;

    //告警等级-紧急
    public static final int EVENT_LEVEL_URGENCY = 1;
    //告警等级-严重
    public static final int EVENT_LEVEL_SERIOUS = 2;
    //告警等级-重要
    public static final int EVENT_LEVEL_IMPORTANT = 3;
    //告警等级-次要的
    public static final int EVENT_LEVEL_SECONDARY = 4;
    //告警等级-预警
    public static final int EVENT_LEVEL_WARNING = 5;

    //空间-总容量
    public static final int ENERGY_AREA_SUM = 71;
    //空间-使用容量
    public static final int ENERGY_AREA_USE = 72;
    //空间-占比
    public static final int ENERGY_AREA_PER = 73;
    //配电-总容量
    public static final int ENERGY_ELEC_SUM = 81;
    //配电-使用容量
    public static final int ENERGY_ELEC_USE = 82;
    //配电-占比
    public static final int ENERGY_ELEC_PER = 83;
    //制冷-总容量
    public static final int ENERGY_COOL_SUM = 91;
    //制冷-使用容量
    public static final int ENERGY_COOL_USE = 92;
    //制冷-占比
    public static final int ENERGY_COOL_PER = 93;

}
