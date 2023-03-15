package com.cmbc.infras.dto.rpc;

import lombok.Data;

@Data
public class Monitor {

    private int status;
    private String server_time;
    private String resource_id;
    private String real_value;
    private String event_count;
    private String alias;
    private String save_time;

}
