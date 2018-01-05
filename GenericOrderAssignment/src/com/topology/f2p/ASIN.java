package com.topology.f2p;

public class ASIN implements Comparable<ASIN> {
    String id;
    Sortable sortable;
    double length;
    double width;
    double height;
    double weight;
    double volume;

    double score;

    public ASIN(String id, Sortable sortable, double weight) {
        this.id = id;
        this.sortable = sortable;
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "ASIN{" + id + '\'' +
                '}';
    }


    @Override
    public int compareTo(ASIN o) {
        return Double.compare(this.score, o.score);
    }
}
