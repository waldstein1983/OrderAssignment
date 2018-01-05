package com.amazon.topology;

import java.util.HashMap;
import java.util.Map;

public class SplitContext {
    Shipment originalShipment;
    Map<ASIN, Integer> content;
    FC newFC;

    public SplitContext(Shipment originalShipment, Map<ASIN, Integer> content, FC newFC) {
        this.originalShipment = originalShipment;
        this.content = content;
        this.newFC = newFC;
    }
}
