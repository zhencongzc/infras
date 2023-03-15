package com.cmbc.infras.dto.rpc;

import lombok.Data;

import java.util.List;

@Data
public class MonitorRpcParam {

    private List<ResourceItem> resources;

    public MonitorRpcParam(List<ResourceItem> resources) {
        this.resources = resources;
    }
}


