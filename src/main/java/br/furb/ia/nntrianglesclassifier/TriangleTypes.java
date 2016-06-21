package br.furb.ia.nntrianglesclassifier;

import java.util.Arrays;

/**
 * Created by Thomas.Adriano on 09/06/2016.
 */
public enum TriangleTypes {

    EQUILATERAL('e', 1d), ISOSCELES('i', 0.5d), SCALENE('s', 0d);

    private final char charValue;
    private final double doubleValue;

    TriangleTypes(char c, double doubleValue) {
        this.charValue = c;
        this.doubleValue = doubleValue;
    }

    public char getCharValue() {
        return charValue;
    }

    public double getDoubleValue() {
        return doubleValue;
    }

    public static final TriangleTypes fromChar(char c) {
        for (TriangleTypes t : values()) {
            if (t.charValue == c) {
                return t;
            }
        }
        throw new IllegalArgumentException("char value " + c + " não é um tipo de triangulo válido. Tipos válidos são " + Arrays.toString(values()));
    }
}
