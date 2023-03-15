package com.cmbc.infras.dto.rpc.event;

import lombok.Data;

@Data
public class EventGroupParam extends EventParam {
    private String group;

    public EventGroupParam() {
        super();
    }

    public EventGroupParam(EventParam param) {
        super(param.getWhere().getAnd().get(0).getAnd(), param.getSorts(), param.getPage());
    }

}
