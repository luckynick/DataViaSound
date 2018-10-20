package com.luckynick.shared.enums;

public enum DistanceUnit {

    Centimeter(1.0),
    LaptopWidth(38.0),
    ;

    private double centimetersInOneUnit;

    private DistanceUnit() {};

    DistanceUnit(double centimetersInOneUnit) {
        this.centimetersInOneUnit = centimetersInOneUnit;
    }

    public double getCentimetersInOneUnit() {
        return centimetersInOneUnit;
    }
}
