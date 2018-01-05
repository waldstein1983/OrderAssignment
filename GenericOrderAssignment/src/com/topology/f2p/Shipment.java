package com.amazon.topology;

import java.util.Map;

public class Shipment {
    String orderId;
    String shipmentId;
    //    ShipOption shipOption;
    Map<ASIN, Integer> units;
    FC fc;
    //    double x;
//    double y;
    double score;

    public Shipment(String orderId, String shipmentId, Map<ASIN, Integer> units, FC fc) {
        this.orderId = orderId;
        this.shipmentId = shipmentId;
        this.units = units;
        this.fc = fc;
    }
}
