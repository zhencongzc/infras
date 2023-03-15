package com.cmbc.infras.system.service.impl;

import com.cmbc.infras.dto.Label;
import com.cmbc.infras.system.mapper.LabelMapper;
import com.cmbc.infras.system.service.LabelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service
public class LabelServiceImpl implements LabelService {

    @Resource
    private LabelMapper labelMapper;

    @Override
    public Label getUserLabel(String account) {
        Label label = labelMapper.getLabelsChecked(account);
        if (label == null) {
            label = new Label("default", "1,2");
        }
        return label;
    }
}
