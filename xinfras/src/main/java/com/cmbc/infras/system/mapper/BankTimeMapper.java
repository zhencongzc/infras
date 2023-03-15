package com.cmbc.infras.system.mapper;

import com.cmbc.infras.dto.ops.BankTime;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BankTimeMapper {

    BankTime getBankTime(String bankId);

}
