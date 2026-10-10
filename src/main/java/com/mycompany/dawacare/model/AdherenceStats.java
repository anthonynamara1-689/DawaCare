package com.mycompany.dawacare.model;

public record AdherenceStats(String medication, int taken, int missed, int skipped) {

    public int total() {
        return taken + missed + skipped;
    }

    /** Percentage taken, or null if there are no completed doses yet. */
    public Double percent() {
        return total() == 0 ? null : 100.0 * taken / total();
    }

    public String percentText() {
        Double p = percent();
        return p == null ? "-" : String.format("%.0f%%", p);
    }
}