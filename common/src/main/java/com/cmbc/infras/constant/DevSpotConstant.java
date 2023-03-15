package com.cmbc.infras.constant;


/**
 * 设备-测点常量
 * 设备-DeviceTypeEnum
 * 测点-SpotTypeEnum
 */
public class DevSpotConstant {

    /**
     * 设备-DeviceTypeEnum
     */
    public static final int DEV_UPS = 10;
    public static final int DEV_AIR = 20;
    public static final int DEV_HUM = 30;
    public static final int DEV_PUE = 40;
    public static final int DEV_CHAI_FA = 50;
    /**
     * 测点类型ID-UPS
     */
    public static final int SPOT_UPS_POWER = 11;
    public static final int SPOT_UPS_LOAD = 12;
    public static final int SPOT_UPS_TIME = 13;
    public static final int SPOT_UPS_ON_OFF = 14;
    /**
     * 测点类型ID-空调
     */
    public static final int SPOT_BACK_TEMPER = 21; //回风温度
    public static final int SPOT_BACK_HUMIDITY = 22; //回风湿度
    /**
     * 测点类型ID-温湿度
     */
    public static final int SPOT_HUM_TEMP = 31;
    public static final int SPOT_HUM_HUMI = 32;
    /**
     * PUE
     */
    public static final int PUE_REAL_TIME = 41;
    public static final int PUE_TOTAL_POWER = 42;
    public static final int PUE_IT_POWER = 43;
    /**
     * 柴发测点ID
     */
    public static final int CHAI_FA_TIME = 51;


}
