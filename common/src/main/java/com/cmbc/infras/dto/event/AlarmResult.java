package com.cmbc.infras.dto.event;

import com.cmbc.infras.dto.rpc.event.Event;
import lombok.Data;

import java.util.List;

@Data
public class AlarmResult {
    private Page page;
    private List<Event> event_list;
}
