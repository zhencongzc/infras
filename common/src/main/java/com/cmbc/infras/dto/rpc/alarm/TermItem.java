package com.cmbc.infras.dto.rpc.alarm;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class TermItem implements Serializable {
    private static final long serialVersionUID = 1829597325493145074L;

    private List<FieldItem> terms;

    public TermItem add(FieldItem fieldItem) {
        if (this.terms == null) {
            terms = new ArrayList<>();
        }
        this.terms.add(fieldItem);
        return this;
    }

}
