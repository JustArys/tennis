package com.example.tennis.kz.model;

public enum Category {
    SINGLES_MALE,
    SINGLES_FEMALE,
    SINGLES_ALL,
    DOUBLE_MALE,
    DOUBLE_FEMALE,
    DOUBLE_MIXED,
    DOUBLE_ALL;
    public boolean isDoubles() {
        return this == DOUBLE_MALE || this == DOUBLE_FEMALE || this == DOUBLE_MIXED || this == DOUBLE_ALL;
    }
    public boolean isSingles() {
        return !isDoubles();
    }
}
