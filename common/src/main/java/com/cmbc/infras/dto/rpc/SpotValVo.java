package com.cmbc.infras.dto.rpc;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SpotValVo {
    /**
     * 测点ids
     */
    private List<SpotVal> resources = new ArrayList<>();

    @Data
    public static class SpotVal {
        /**
         * 状态
         */
        private String status;
        /**
         * 服务器时间戳
         */
        private String server_time;
        /**
         * 测点id
         */
        private String resource_id;
        /**
         * 测点真实值
         */
        private String real_value;
        /**
         * 事件数
         */
        private String event_count;
        /**
         * 别名
         */
        private String alias;
        /**
         * 保存的时间戳
         */
        private String save_time;
    }
}
