package com.cmbc.infras.dto.event;

import com.cmbc.infras.constant.EventLevelEnum;
import com.cmbc.infras.dto.Device;
import lombok.Data;

@Data
public class EventInfo extends Device {

    private String resourceId;

    private int level;

    private String eventTime;

    private String content;

    public EventInfo() { }

    public EventInfo(Device dev) {
        super(dev);
    }

    public EventInfo(Device dev, String resourceId, int level, String time, String content) {
        super(dev);
        this.resourceId = resourceId;
        this.level = level;
        this.eventTime = time;
        this.content = content;
    }

    public String getLevelName() {
        return EventLevelEnum.getDesc(level);
    }

}
