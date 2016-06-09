package br.furb.ia.nntrianglesclassifier;

/**
 * Created by Thomas.Adriano on 09/06/2016.
 */
public class BBox {
    private final int maxX;
    private final int minX;
    private final int maxY;
    private final int minY;

    public BBox(int maxX, int minX, int maxY, int minY) {
        this.maxX = maxX;
        this.minX = minX;
        this.maxY = maxY;
        this.minY = minY;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMinY() {
        return minY;
    }

    @Override
    public String toString() {
        return "BBox{" +
                "maxX=" + maxX +
                ", minX=" + minX +
                ", maxY=" + maxY +
                ", minY=" + minY +
                '}';
    }
}
