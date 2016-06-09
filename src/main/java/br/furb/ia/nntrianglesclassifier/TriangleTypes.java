package br.furb.ia.nntrianglesclassifier;

/**
 * Created by Thomas.Adriano on 09/06/2016.
 */
public enum TriangleTypes {

    EQUILATERAL('e'), ISOSCELES('i'), SCALENE('s');

    private final char charValue;

    TriangleTypes(char c) {
        this.charValue = c;
    }

    public char getCharValue() {
        return charValue;
    }
}
