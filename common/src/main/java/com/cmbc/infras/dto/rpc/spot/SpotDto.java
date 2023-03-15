package com.cmbc.infras.dto.rpc.spot;

import com.cmbc.infras.dto.rpc.event.PageCondition;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SpotDto {
    private String resource_id;

    private String relation_code;

    private List<WhereDto> where = new ArrayList<>();

    private PageCondition page;
}
