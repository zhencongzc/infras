package com.cmbc.infras.dto.rpc;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SpotValDto {
    /**
     * 测点ids
     */
    private List<SpotVal> resources = new ArrayList<>();

    @Data
    public static class SpotVal {
        /**
         * 测点id
         */
        private String resource_id;
    }
}
