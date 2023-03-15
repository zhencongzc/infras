package com.cmbc.infras.system.domain;

import lombok.Data;

@Data
public class FlowFormBankParam {

    private String bankId;

    private String bankName;

    private String parentId;

    private String level;

    private Integer page;

    private Integer size;

}
