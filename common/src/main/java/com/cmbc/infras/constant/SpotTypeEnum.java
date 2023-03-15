package com.cmbc.infras.constant;

import java.util.LinkedList;
import java.util.List;

public enum SpotTypeEnum {
    //UPS
    UPS_POWER(11, "功率"),
    UPS_LOAD(12, "负载率"),
    UPS_BACK_TIME(13, "电池后备时间"),
    UPS_RECTIFIER(14, "整流器"),
    UPS_DISCHARGE(15, "电池放电"),
    //空调
    AIR_OUT(21, "回风温度"),
    AIR_BACK(22, "回风湿度"),
    //温湿度
    HUM_TEMP(31, "温度"),
    HUM_HUMI(32, "湿度"),
    //PUE
    PUE_REAL_TIME(41, "实时PUE"),
    PUE_TOTAL_POWER(42, "总功率"),
    PUE_IT_POWER(43, "IT功率"),
    //柴发
    CHAI_FA_BACK_TIME(51, "柴发后备时间"),
    //空间下测点
    ENERGY_AREA_SUM(71, "总容量"),
    ENERGY_AREA_USE(72, "使用容量"),
    ENERGY_AREA_PER(73, "占比"),
    //配电下测点
    ENERGY_ELEC_SUM(81, "总容量"),
    ENERGY_ELEC_USE(82, "使用容量"),
    ENERGY_ELEC_PER(83, "占比"),
    //制冷下测点
    ENERGY_COOL_SUM(91, "总容量"),
    ENERGY_COOL_USE(92, "使用容量"),
    ENERGY_COOL_PER(93, "占比");


    private int type;
    private String name;

    SpotTypeEnum(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public static String getTypeName(Integer type) {
        if (type == null) {
            return "";
        }
        for (SpotTypeEnum e : SpotTypeEnum.values()) {
            if (type.equals(e.type)) {
                return e.name;
            }
        }
        return "";
    }

    public static List<SpotTypeEnum> getListByDeviceType(int type) {
        List<SpotTypeEnum> res = new LinkedList<>();
        switch (type) {
            case 10: {
                res.add(UPS_POWER);
                res.add(UPS_LOAD);
                res.add(UPS_BACK_TIME);
                res.add(UPS_RECTIFIER);
                res.add(UPS_DISCHARGE);
                break;
            }
            case 20: {
                res.add(AIR_OUT);
                res.add(AIR_BACK);
                break;
            }
            case 30: {
                res.add(HUM_TEMP);
                res.add(HUM_HUMI);
                break;
            }
            case 40: {
                res.add(PUE_REAL_TIME);
                res.add(PUE_TOTAL_POWER);
                res.add(PUE_IT_POWER);
                break;
            }
            case 50: {
                res.add(CHAI_FA_BACK_TIME);
                break;
            }
            case 70: {
                res.add(ENERGY_AREA_SUM);
                res.add(ENERGY_AREA_USE);
                res.add(ENERGY_AREA_PER);
                break;
            }
            case 80: {
                res.add(ENERGY_ELEC_SUM);
                res.add(ENERGY_ELEC_USE);
                res.add(ENERGY_ELEC_PER);
                break;
            }
            case 90: {
                res.add(ENERGY_COOL_SUM);
                res.add(ENERGY_COOL_USE);
                res.add(ENERGY_COOL_PER);
                break;
            }
            default:
        }
        return res;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
