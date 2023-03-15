package com.cmbc.infras.system.mapper;

import com.cmbc.infras.dto.Label;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LabelMapper {

    List<Label> getLabels(String account);

    Label getLabelsChecked(String account);

    Label getLabel(int id);

    int editLabel(Label label);

    int addLabel(Label label);

    int delLabel(int id);

    int unCheck(String account);

    int setCheck(Integer id);

}
