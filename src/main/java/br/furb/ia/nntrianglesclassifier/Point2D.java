package br.furb.ia.nntrianglesclassifier;

/**
 * Created by Thomas.Adriano on 13/06/2016.
 */
public class Point2D implements Comparable<Point2D> {
    public final int x;
    public final int y;

    public Point2D(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "Point2D{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }

    @Override
    public int compareTo(Point2D o) {
        return (this.x + this.y) - (o.x + o.y);
    }

    public int compareToReverse(Point2D o) {
        return (o.x + o.y) - (this.x + this.y);
    }

    public int minXminYComparator(Point2D o) {
        return (int) (this.x + (this.y*0.3)) - (int) (o.x + (o.y*0.3));
    }

}
