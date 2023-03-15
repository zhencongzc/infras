package com.cmbc.infras.dto.rpc.spot;

import com.cmbc.infras.dto.rpc.event.QueryCondition;
import lombok.Data;

import java.util.List;

@Data
public class WhereDto {
    private List<QueryCondition> terms;
}
