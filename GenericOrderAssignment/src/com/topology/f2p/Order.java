package com.topology.f2p;

import java.time.LocalTime;
import java.util.Map;

//orders should be consolidated based on a cluster, from clustering algorithms
public class Order {
    Map<ASIN, Integer> units;
    String id;
    ShipOption shipOption;
    double x;
    double y;
    OrderStatus status  = OrderStatus.UNFULFILLED;

    boolean prime;

    LocalTime time;

    public Order(Map<ASIN, Integer> units, String id, ShipOption shipOption, double x, double y, LocalTime time) {
        this.units = units;
        this.id = id;
        this.shipOption = shipOption;
        this.x = x;
        this.y = y;
        this.time = time;
    }
}
