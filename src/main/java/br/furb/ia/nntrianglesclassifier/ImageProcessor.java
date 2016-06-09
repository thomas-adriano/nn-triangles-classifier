package br.furb.ia.nntrianglesclassifier;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Provê metodos para carregar imagens (tiff, dicom, fits, pgm, jpeg, bmp, gif, lut, roi), aplicar transformações
 * relevantes para a extração de bounding box e para efetivamente extrair bounding boxes de imagens.
 * É importante evidenciar que a bounding box extraída atualmente tem uma pequena taxa de erro nas coordenadas Y (maxY/minY)
 * de aproximadamente 3.5%
 */
public class ImageProcessor {

    private static final Logger LOGGER = LogManager.getLogger();
    private final List<File> images = new ArrayList<>();
    private final List<ij.process.ImageProcessor> ip = new ArrayList<>();
    private final List<ImageConverter> ic = new ArrayList<>();

    public void loadImages(File[] imgs) {
        assert imgs != null;
        images.clear();
        ip.clear();
        ic.clear();

        int counter = 0;
        for (File f : imgs) {
            counter++;
            if (!f.exists()) {
                throw new RuntimeException("Arquivo " + f.getAbsolutePath() + " não encontrado.");
            }
            LOGGER.debug("Carregando imagem " + f.getName() + " - " + counter + " de " + imgs.length);
            ImagePlus imgP = IJ.openImage(f.getAbsolutePath());
            images.add(f);
            ip.add(imgP.getProcessor());
            ic.add(new ImageConverter(imgP));
        }
        LOGGER.info(imgs.length + " imagens carregas");
        logImagesContents();
    }

    public void resizeTo28x28() {
        boolean hadSome = false;
        for (int i = 0; i < ip.size(); i++) {
            ij.process.ImageProcessor p = ip.get(i);
            if (p.getHeight() > 28 || p.getWidth() > 28) {
                LOGGER.debug("Redimensionando imagem " + images.get(i).getName() + " de " + p.getWidth() + " x " + p.getHeight() + " para 28 x 28...");
                ip.add(i, p.resize(28));
                hadSome = true;
            }
        }
        if (hadSome) {
            LOGGER.info(ic.size() + " imagens redimensionadas para 28 x 28");
            logImagesContents();
        } else {
            LOGGER.info("Imagens com dimensões maiores que 28 x 28 não foram encontradas, por isso nenhum processo de redimensionamento foi executado.");
        }
    }

    public void convertAllTo8BitGrayScale() {
        for (int i = 0; i < ic.size(); i++) {
            LOGGER.debug("Convertendo imagem " + images.get(i).getName() + " para 8bit gray scale - " + (i + 1) + " de " + ic.size());
            ic.get(i).convertToGray8();
        }
        LOGGER.info(ic.size() + " imagens convertidas para 8-bit gray scale");
        logImagesContents();
    }

    public void convertToEdges() {
        for (int i = 0; i < ip.size(); i++) {
            ij.process.ImageProcessor p = ip.get(i);
            LOGGER.debug("Convertendo imagem " + images.get(i).getName() + " para edges - " + (i + 1) + " de " + ic.size());
            p.findEdges();
        }
        LOGGER.info(ip.size() + " imagens convertidas para edges");
        logImagesContents();
    }

    private void logImagesContents() {
        for (int i = 0; i < ip.size(); i++) {
            ij.process.ImageProcessor p = ip.get(i);
            LOGGER.debug("  Imagem \"" + images.get(i).getName() + "\" (" + p.getWidth() + " colunas e " + p.getHeight() + " linhas): " + Arrays.toString((byte[]) p.getPixels()));
        }
    }

    public void saveImages(File destDir) {
        for (int i = 0; i < ip.size(); i++) {
            Image img = ip.get(i).createImage();
            File originImage = images.get(i);
            BufferedImage bufferedImage = ip.get(i).getBufferedImage();
            try {
                ImageIO.write(bufferedImage, "jpg", new File(destDir, originImage.getName()));
            } catch (IOException e) {
                throw new RuntimeException("Não foi possível salvar a processada referente a imagem origem " + originImage);
            }
        }

    }

    /**
     * Calcula e retorna as bounding boxes.
     * É importante evidenciar que a bounding box extraída atualmente tem uma pequena taxa de erro nas coordenadas Y (maxY/minY)
     * de aproximadamente 3.5%.
     *
     * @return {@link BBox} de todas as imagens carregadas através do metodo {@link #loadImages(File[])}
     */
    public List<BBox> getBoundingBoxes() {
        List<BBox> res = new ArrayList<>();
        //para cada imagem...
        for (int i = 0; i < ip.size(); i++) {
            ij.process.ImageProcessor p = ip.get(i);
            Map<Integer, List<Integer>> edgesCoords = getEdgesCoordinates(p);
            int minX = edgesCoords.keySet().stream().sorted().findFirst().get();
            int maxX = edgesCoords.keySet().stream().sorted((o1, o2) -> o2 - o1).findFirst().get();

            //encontra o Y
            List<Integer> yArray = new ArrayList<>();
            for (List<Integer> l : edgesCoords.values()) {
                yArray.addAll(l);
            }
            int minY = yArray.stream().sorted().findFirst().get();
            int maxY = yArray.stream().sorted((o1, o2) -> o2 - o1).findFirst().get();

            LOGGER.debug(images.get(i).getName() + " minX: " + minX + " / maxX: " + maxX + " / maxY: " + maxY + " / minY: " + minY);
            res.add(new BBox(maxX, minX, maxY, minY));
        }
        return res;
    }

    /**
     * Carrega um mapa com as coordenadas (x,y) de pixels relevantes (a.k.a pertencentes ao contorno do triangulo) da imagem.
     * Ou seja, todos as coordenadas presentes no mapa resultante são referentes a um pixel pertencente ao contorno do triângulo.
     */
    private Map<Integer, List<Integer>> getEdgesCoordinates(ij.process.ImageProcessor p) {
        LOGGER.debug("Extraindo coordenadas dos pixels mais relevantes (referentes ao contorno do triangulo)");
        Map<Integer, List<Integer>> mappedCoords = new HashMap<>();

        //para cada coluna (eixo x) da imagem atual...
        for (int x = 0; x < p.getWidth(); x++) {
            //para cada linha (eixo y) da imagem atual...
            for (int y = 0; y < p.getHeight(); y++) {
                int pixelVal = p.get(x, y);
                // se entrar neste if, significa que este pixel faz parte de um contorno
                //(parte branca da imagem tratada - olhe uma imagem tratada na pasta de imagens tratadas para mais entendimento)
                if (pixelVal >= (p.getMax() - 5)) {
                    List<Integer> yCoords = mappedCoords.get(x);
                    if (yCoords == null) {
                        yCoords = new ArrayList<>();
                    } else {
                        yCoords.add(y);
                    }
                    mappedCoords.put(x, yCoords);
                }
            }
        }
        return mappedCoords;
    }

}
