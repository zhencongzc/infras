package com.cmbc.infras.system.service;

import com.cmbc.infras.dto.BaseResult;

import java.util.List;

public interface AriService {

    BaseResult<List<String>> getUserAirSpot(String bankId);

}
