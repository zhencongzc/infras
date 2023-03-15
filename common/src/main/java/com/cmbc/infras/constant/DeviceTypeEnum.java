package com.cmbc.infras.constant;

/**
 * 测点类型
 */
public enum DeviceTypeEnum {

    UPS(10, "UPS"),
    AIR(20, "空调"),
    HUM(30, "温湿度"),
    PUE(40, "PUE"),
    CHAI_FA(50, "柴发"),
//    BATTERY(60, "电池"),
    //主机房-空间,配电,制冷-为了能耗管理展示建成设备了
    AREA(70, "空间"),
    ELEC(80, "配电"),
    COOL(90, "制冷");


    private int type;
    private String name;

    DeviceTypeEnum(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public static String getTypeName(Integer type) {
        if (type == null) {
            return "";
        }
        for (DeviceTypeEnum e : DeviceTypeEnum.values()) {
            if (type.equals(e.type)) {
                return e.name;
            }
        }
        return "";
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
