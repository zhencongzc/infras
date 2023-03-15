package com.cmbc.infras.dto.rpc;

import lombok.Data;

@Data
public class ResourceItem {

    private String resource_id;

    public ResourceItem(String resource_id) {
        this.resource_id = resource_id;
    }
}
