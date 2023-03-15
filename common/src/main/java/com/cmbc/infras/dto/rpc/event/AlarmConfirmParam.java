package com.cmbc.infras.dto.rpc.event;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 告警受理参数
 */
@Data
public class AlarmConfirmParam implements Serializable {
    private static final long serialVersionUID = -7692867473520476761L;

    /**
     * 使用
     * Event.guid
     * Event.resource_id
     */
    private List<Event> event_list;

    private String confirm_by;
    private String confirm_description;
    /**
     * 1:真实告警
     *  --三方页面默认传1
     *  --移动端页面可选参
     * 2:测试告警
     * 3:误告警
     */
    private int confirm_type = 1;

}
