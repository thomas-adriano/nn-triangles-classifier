package br.furb.ia.nntrianglesclassifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Thomas.Adriano on 14/06/2016.
 */
public class TriangleSides {

    private List<Pixel> pixels = new ArrayList<>();
    private boolean calculated = false;
    private double one;
    private double two;
    private double three;

    public TriangleSides(Pixel one, Pixel two, Pixel three) {
        calculateDistances(one, two, three);
    }

    private void calculateDistances(Pixel one, Pixel two, Pixel three) {
        this.one = Math.sqrt(Math.pow((double) two.x - (double) one.x, 2) + Math.pow((double) two.y - (double) one.y, 2));
        this.two = Math.sqrt(Math.pow((double) three.x - (double) two.x, 2) + Math.pow((double) three.y - (double) two.y, 2));
        this.three = Math.sqrt(Math.pow((double) three.x - (double) one.x, 2) + Math.pow((double) three.y - (double) one.y, 2));
        calculated = true;
    }

    public List<Double> distances() {
        if (!calculated) {
            calculateDistances(pixels.get(0), pixels.get(1), pixels.get(2));
        }
        return Arrays.asList(one, two, three);
    }

    public TriangleSides() {
    }

    public void addPixel(Pixel p) {
        if (pixels.size() < 3) {
            pixels.add(p);
        }
    }

    public double getOne() {
        return one;
    }

    public double getTwo() {
        return two;
    }

    public double getThree() {
        return three;
    }

    @Override
    public String toString() {
        return "TriangleSides{" +
                "one=" + one +
                ", two=" + two +
                ", three=" + three +
                '}';
    }
}
