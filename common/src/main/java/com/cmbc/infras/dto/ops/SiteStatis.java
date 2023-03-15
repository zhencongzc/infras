package com.cmbc.infras.dto.ops;

import lombok.Data;

import java.io.Serializable;

@Data
public class SiteStatis implements Serializable {
    private static final long serialVersionUID = -2281851535930488757L;

    private int total;
    private int totalOn;

    private int level1Total;
    private int level1TotalOn;

    private int level2Total;
    private int level2TotalOn;

    private int level3Total;
    private int level3TotalOn;

    private int level4Total;
    private int level4TotalOn;

    public int getTotal() {
        return level1Total + level2Total
                + level3Total + level4Total;
    }

    public int getTotalOn() {
        return level1TotalOn + level2TotalOn
                + level3TotalOn + level4TotalOn;
    }

}
