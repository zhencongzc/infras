package com.cmbc.infras.health.contant;

public enum SpotTypeEnum {

    TEMPERATURE("温度", 1),
    HUMIDITY("湿度", 2),
    PUE("实时PUE", 3),
    ALL_POWER("总功率", 4),
    IT_POWER("IT设施总功率", 5),
    UPS("UPS负载率", 6);

    private String desc;
    private int id;

    SpotTypeEnum(String desc, int id) {
        this.desc = desc;
        this.id = id;
    }

    /**
     * 判断传入的参数是否存在
     */
    public static boolean belongSpotType(String name) {
        for (SpotTypeEnum type : SpotTypeEnum.values()) {
            if (type.desc.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 返回类型的id
     */
    public static int getId(String desc) {
        SpotTypeEnum[] values = SpotTypeEnum.values();
        for (SpotTypeEnum value : values) {
            if (value.desc.equals(desc)) return value.getId();
        }
        return -1;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
