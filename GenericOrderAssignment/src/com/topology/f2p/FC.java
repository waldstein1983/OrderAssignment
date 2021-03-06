package com.topology.f2p;

import java.util.HashMap;
import java.util.Map;

public class FC {
    String id;
    Map<ASIN, Integer> storages = new HashMap<>();
    double x;
    double y;
    Sortable sortable;
    double unitProcessingCost;
    double packagingCost;
    double unitStorageCost;
    int storageCapacity;
    boolean virtual = true;

    public FC(String id, double x, double y, Sortable sortable, double unitProcessingCost,
              double packagingCost, double unitStorageCost, int storageCapacity) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.sortable = sortable;
        this.unitProcessingCost = unitProcessingCost;
        this.packagingCost = packagingCost;
        this.unitStorageCost = unitStorageCost;
        this.storageCapacity = storageCapacity;
    }

    @Override
    public String toString() {
        return "FC{" +
                id + '\'' +
                '}';
    }

    public int getTotalInventory() {
        int total = 0;
        for (ASIN asin : storages.keySet()) {
            total += storages.get(asin);
        }
        return total;
    }
}
