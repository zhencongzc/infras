package com.cmbc.infras.system.service;

import com.cmbc.infras.dto.BaseParam;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.ops.*;

import java.util.List;

public interface BranchMainService {

    BaseResult<Energy> getEnergyData(BaseParam param);

    BaseResult<List<Humiture>> getHumiture(BaseParam param);

    BaseResult<DisposeRate> getDisposeRate(BaseParam param);

    BaseResult<GradeRate> getGradeRate(BaseParam param);

    BaseResult<Capacity> getCapacity(BaseParam param);

    BaseResult<List<BranchRadar>> getBranchRadar(BaseParam param);

    BaseResult<Maintain> getMaintainRate(BaseParam param);

}
