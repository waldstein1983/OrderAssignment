package com.topology.f2p;

public class ShippingCost {
    public static double EclidDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    public static double cost(double weight, double distance) {
        return 0.1 * weight + distance * 0.001 + 0.3;
    }
}
