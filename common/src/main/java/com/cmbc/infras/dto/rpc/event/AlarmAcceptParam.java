package com.cmbc.infras.dto.rpc.event;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 告警受理参数
 */
@Data
public class AlarmAcceptParam implements Serializable {
    private static final long serialVersionUID = 2992882989750884519L;

    /**
     * 使用
     * Event.guid
     * Event.resource_id
     */
    private List<Event> event_list;

    private String accept_by;
    private String accept_description;

}
