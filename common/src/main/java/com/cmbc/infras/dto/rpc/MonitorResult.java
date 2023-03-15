package com.cmbc.infras.dto.rpc;

import lombok.Data;

import java.util.List;

@Data
public class MonitorResult {

    private int total;

    private List<Monitor> resources;

}