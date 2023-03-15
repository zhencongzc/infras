package com.cmbc.infras.dto.ops;

import lombok.Data;

import java.io.Serializable;

@Data
public class EvaluateItem implements Serializable {
    private static final long serialVersionUID = -5010464731182124409L;

    private int id;
    private String name;
    private float score;
}
