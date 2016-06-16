package br.furb.ia.nntrianglesclassifier;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Thomas.Adriano on 14/06/2016.
 */
public class TrianglePrincipalPoints {

    private Pixel one;
    private Pixel two;
    private Pixel three;

    public TrianglePrincipalPoints(Pixel one, Pixel two, Pixel three) {
        this.one = one;
        this.two = two;
        this.three = three;
    }

    public List<Pixel> pixels() {
        return Arrays.asList(one, two, three);
    }

    public TrianglePrincipalPoints() {
    }

    public void addPixel(Pixel p) {
        if (one == null) {
            one = p;
        } else if (two == null) {
            two = p;
        } else if (three == null) {
            three = p;
        }
    }

    public Pixel getOne() {
        return one;
    }

    public Pixel getTwo() {
        return two;
    }

    public Pixel getThree() {
        return three;
    }

    @Override
    public String toString() {
        return "TrianglePrincipalPoints{" +
                "one=" + one +
                ", two=" + two +
                ", three=" + three +
                '}';
    }
}
