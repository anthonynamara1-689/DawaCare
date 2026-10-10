package com.mycompany.dawacare.model;

public record CategoryOption(int categoryId, String name) {
    @Override
    public String toString() {
        return name;
    }
}