package com.cmbc.infras.dto.ops;

import lombok.Data;

import java.io.Serializable;

@Data
public class Capacity implements Serializable {
    private static final long serialVersionUID = -5016464731185124409L;
    //空间
    private String spaceUsed;
    private String spaceTotal;
    private String spaceRate;
    //配电
    private String powerUsed;
    private String powerTotal;
    private String powerRate;
    //制冷
    private String coolUsed;
    private String coolTotal;
    private String coolRate;

    public Capacity() {}

    public Capacity kong() {
        this.spaceUsed = "";
        this.spaceTotal = "";
        this.spaceRate = "";
        this.powerUsed = "";
        this.powerTotal = "";
        this.powerRate = "";
        this.coolUsed = "";
        this.coolTotal = "";
        this.coolRate = "";
        return this;
    }

}
