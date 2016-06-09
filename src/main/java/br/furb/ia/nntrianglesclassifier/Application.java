package br.furb.ia.nntrianglesclassifier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Created by Thomas.Adriano on 09/06/2016.
 */
public class Application {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) {
        long init = System.currentTimeMillis();
        LOGGER.info("Iniciando execução...");

        File imgPath = new File("");

        ImageProcessor ip = new ImageProcessor();
        NeuralNetwork nn = new NeuralNetwork();
        ip.loadImage(imgPath);
        ip.convertTo8BitGrayScale();
        nn.doTheMagic(ip.getImagePixelArray());


        double elapsed = (System.currentTimeMillis() - init) / 1000;
        LOGGER.info("Tempo de execução total: "+elapsed+" segundos.");
    }

}
