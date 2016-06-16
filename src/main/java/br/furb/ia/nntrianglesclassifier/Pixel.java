package br.furb.ia.nntrianglesclassifier;

/**
 * Created by Thomas.Adriano on 13/06/2016.
 */
public class Pixel implements Comparable<Pixel> {
    public final int x;
    public final int y;
    public final int val;

    public Pixel(int x, int y, int val) {
        this.x = x;
        this.y = y;
        this.val = val;
    }

    public int[] values(){
        return new int[]{x, y};
    }

    @Override
    public String toString() {
        return "Pixel{" +
                "x=" + x +
                ", y=" + y +
                ", val=" + val +
                '}';
    }

    @Override
    public int compareTo(Pixel o) {
        return (this.x + this.y) - (o.x + o.y);
    }

    public int compareToReverse(Pixel o) {
        return (o.x + o.y) - (this.x + this.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pixel pixel = (Pixel) o;

        if (x != pixel.x) return false;
        if (y != pixel.y) return false;
        return val == pixel.val;

    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + val;
        return result;
    }
}
