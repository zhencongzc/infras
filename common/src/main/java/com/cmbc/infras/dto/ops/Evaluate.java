package com.cmbc.infras.dto.ops;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class Evaluate implements Serializable {
    private static final long serialVersionUID = -5016464731182124409L;

    private String bankName;
    private List<EvaluateItem> list;
}
