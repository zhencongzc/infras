package com.cmbc.infras.util;


import com.cmbc.infras.dto.monitor.Spot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * ResourceId集合
 * 简化工具
 */
public class SpotIds {
    List<String> resourceIds = new ArrayList<>();

    public void addAll(List<Spot> spots) {
        for (Spot spot : spots) {
            resourceIds.add(spot.getSpotId());
        }
    }

    public void addAll(Collection<String> ids) {
        resourceIds.addAll(ids);
    }

    public List<String> getResourceIds() {
        return resourceIds;
    }
}
