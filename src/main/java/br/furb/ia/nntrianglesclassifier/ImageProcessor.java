package br.furb.ia.nntrianglesclassifier;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;


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
    private static final Dimension BASE_DIMENSIONS = new Dimension(50, 50);
    private static final int WHITE_PIXEL_VAL = -1;
    private static final int BLACK_PIXEL_VAL = 255;

    private static final class Dimension {
        public final int X;
        public final int Y;

        public Dimension(int x, int y) {
            X = x;
            Y = y;
        }
    }

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

    /**
     * Redimensiona a imagem mantendo a proporção, e.g.: para 28 x Y onde Y = tamanho necessário para manter a proporção da imagem.
     */
    public void resizeToWidth28() {
        int resized = 0;
        for (int i = 0; i < ip.size(); i++) {
            ij.process.ImageProcessor p = ip.get(i);
            if (p.getHeight() > 28 || p.getWidth() > 28) {
                LOGGER.debug("Redimensionando imagem " + images.get(i).getName() + " de " + p.getWidth() + " x " + p.getHeight() + " para 28 x 28...");
                ip.set(i, p.resize(28));
                resized++;
            }
        }
        if (resized > 0) {
            LOGGER.info(resized + " imagens redimensionadas para 28 x 28");
            logImagesContents();
        } else {
            LOGGER.info("Imagens com dimensões maiores que 28 x 28 não foram encontradas, por isso nenhum processo de redimensionamento foi executado.");
        }
    }

    public void convertAllTo8BitGrayScale() {
        for (int i = 0; i < ic.size(); i++) {
            LOGGER.debug("Convertendo imagem " + images.get(i).getName() + " para 8bit gray scale - " + (i + 1) + " de " + ic.size());
            ic.get(i).convertToRGB();
            ic.get(i).convertToGray8();
        }
        LOGGER.info(ic.size() + " imagens convertidas para 8-bit gray scale");
        logImagesContents();
    }

    public void binarizeImage() {
        for (int i = 0; i < ip.size(); i++) {
            ij.process.ImageProcessor p = ip.get(i);
            LOGGER.debug("Binarizando imagem " + images.get(i).getName() + " - " + (i + 1) + " de " + ic.size());
            p.autoThreshold();
        }
        LOGGER.info(ic.size() + " imagens binarizadas");
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

    public void cropImagesToBBox() {
        List<ij.process.ImageProcessor> cropped = new ArrayList<>();
        for (int i = 0; i < ip.size(); i++) {
            ij.process.ImageProcessor p = ip.get(i);
            int oldWidth = p.getWidth();
            int oldHeight = p.getHeight();
            try {
                BBox box = getBoundingBox(p);
                LOGGER.debug("BBox extraida de figura " + images.get(i).getName() + " (width: " + oldWidth + " height: " + oldHeight + "): " + box);
                int newWidth = box.getMaxX() - box.getMinX();
                int newHeigth = (box.getMaxY() - box.getMinY()) + 5 /*evita que deixe parte do triangulo de fora*/;
                p.setRoi(new Rectangle(newWidth, newHeigth));
                cropped.add(p.crop());

                LOGGER.debug("  Imagem \"" + images.get(i).getName() + "\" \"cropeada\" de width/height: " + oldWidth + "/" + oldHeight + " para width/height: " + newWidth + "/" + newHeigth);
            } catch (ImageIncompatibleException e) {
                LOGGER.warn("Não foi possível extrair BBox de imagem " + images.get(i).getName());
                ColorModel m = p.getCurrentColorModel();
            }
        }
        if (!cropped.isEmpty()) {
            ip.clear();
            ip.addAll(cropped);
        }
    }

    private void logImagesContents() {
        for (int i = 0; i < ip.size(); i++) {
            ij.process.ImageProcessor p = ip.get(i);
            LOGGER.debug("  Imagem \"" + images.get(i).getName() + "\" (" + p.getWidth() + " colunas e " + p.getHeight() + " linhas)");
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

    private void debugContours(List<Pixel> points, int originalWidth, int originalHeight, ColorModel m) {
        int[][] arr = new int[originalWidth][originalHeight];

        Map<Pixel, Integer> pixelMap = new HashMap<>();
        for (Pixel pix : points) {
            pixelMap.put(new Pixel(pix.x, pix.y, -1), pix.val);
        }

        for (int x = 0; x < originalWidth; x++) {
            for (int y = 0; y < originalHeight; y++) {
                Pixel pix = new Pixel(x, y, -1);
                if (pixelMap.containsKey(pix)) {
                    arr[x][y] = pixelMap.get(pix);
                } else {
                    arr[x][y] = 0;
                }
            }
        }

        LOGGER.debug("Pixels : " + points);
        debugContours(arr, m);
    }

    private void debugContours(int[][] arr, ColorModel m) {
        int y = 0;
        int x = 0;
        String[] line = new String[arr.length];
        while (x < arr.length && y < arr[x].length) {
            int pixel = arr[x][y];

            if (isPixelBlackOrWhite(pixel)) {
                line[x] = "O";
            } else {
                line[x] = " ";
            }

            x++;
            if (x == arr.length) {
                x = 0;
                y++;
                LOGGER.debug(Arrays.toString(line).replace(",", ""));
            }
        }
    }

    public List<TrianglePrincipalPoints> getPrincipalPoints(boolean debug) {
        List<TrianglePrincipalPoints> res = new ArrayList<>();

        for (int i = 0; i < ip.size(); i++) {
            ij.process.ImageProcessor imgProc = ip.get(i);
            List<Pixel> contourCoordinates = getEdgesPixels(imgProc);
            Set<Pixel> extractedPoints = new HashSet<>();

            BBox bbox = getBoundingBox(imgProc);

            List<Pixel> maxXPixels = contourCoordinates.stream().filter(p -> p.x >= bbox.getMaxX()).collect(Collectors.toList());
            List<Pixel> maxYPixels = contourCoordinates.stream().filter(p -> p.y >= bbox.getMaxY()).collect(Collectors.toList());
            List<Pixel> minXPixels = contourCoordinates.stream().filter(p -> p.x <= bbox.getMinX()).collect(Collectors.toList());
            List<Pixel> minYPixels = contourCoordinates.stream().filter(p -> p.y <= bbox.getMinY()).collect(Collectors.toList());

            double tolerancePercentage = 0.03;
            int tolerance = getDistanceTolerance(imgProc.getWidth(), imgProc.getHeight(), tolerancePercentage);

            Pixel maxXmaxYPixel = maxXPixels.stream().sorted((p1, p2) -> p2.y - p1.y).findFirst().orElseThrow(ImageIncompatibleException::new);
            if (maxXmaxYPixel != null) {
                extractedPoints.add(maxXmaxYPixel);
            }
            Pixel maxXminYPixel = maxXPixels.stream().sorted((p1, p2) -> p1.y - p2.y).findFirst().orElse(null);
            if (maxXminYPixel != null && getDistance(maxXmaxYPixel, maxXminYPixel) > tolerance) {
                extractedPoints.add(maxXminYPixel);
            }

            Pixel minXmaxYPixel = minXPixels.stream().sorted((p1, p2) -> p2.y - p1.y).findFirst().orElse(null);
            if (minXmaxYPixel != null) {
                extractedPoints.add(minXmaxYPixel);
            }
            Pixel minXminYPixel = minXPixels.stream().sorted((p1, p2) -> p1.y - p2.y).findFirst().orElse(null);
            if (minXminYPixel != null && getDistance(minXmaxYPixel, minXminYPixel) > tolerance) {
                extractedPoints.add(minXminYPixel);
            }

            Pixel maxYmaxXPixel = maxYPixels.stream().sorted((p1, p2) -> p2.x - p1.x).findFirst().orElse(null);
            if (maxYmaxXPixel != null && getDistance(maxYmaxXPixel, maxXmaxYPixel) > tolerance) {
                extractedPoints.add(maxYmaxXPixel);
            }
            Pixel maxYminXPixel = maxYPixels.stream().sorted((p1, p2) -> p1.x - p2.x).findFirst().orElse(null);
            if (maxYminXPixel != null && getDistance(maxYmaxXPixel, maxYminXPixel) > tolerance && getDistance(minXmaxYPixel, maxYminXPixel) > tolerance) {
                extractedPoints.add(maxYminXPixel);
            }

            Pixel minYmaxXPixel = minYPixels.stream().sorted((p1, p2) -> p2.x - p1.x).findFirst().orElse(null);
            if (minYmaxXPixel != null && getDistance(minYmaxXPixel, maxXminYPixel) > tolerance) {
                extractedPoints.add(minYmaxXPixel);
            }
            Pixel minYminXPixel = minYPixels.stream().sorted((p1, p2) -> p1.x - p2.x).findFirst().orElse(null);
            if (minYminXPixel != null && getDistance(minYmaxXPixel, minYminXPixel) > tolerance && getDistance(minYminXPixel, minXminYPixel) > tolerance) {
                extractedPoints.add(minYminXPixel);
            }

            //remove os pares proximos de pontos restantes
            List<Pixel> finalPixels = new ArrayList<>(extractedPoints);
            List<Pixel> extractedPointsList = new ArrayList<>(extractedPoints);
            while (finalPixels.size() > 3) {
                tolerance = getDistanceTolerance(imgProc.getWidth(), imgProc.getHeight(), tolerancePercentage);
                for (int u = 0; u < extractedPoints.size() && finalPixels.size() > 3; u++) {
                    Pixel p1 = extractedPointsList.get(u);
                    for (int q = 0; q < extractedPoints.size() && finalPixels.size() > 3; q++) {
                        Pixel p2 = extractedPointsList.get(q);
                        if (p1 == p2) {
                            continue;
                        }
                        if (getDistance(p1, p2) <= tolerance) {
                            finalPixels.remove(p2);
                        }
                    }
                }

                tolerancePercentage += 0.01;
            }

            int initialSize = contourCoordinates.size();
            int actualSize = finalPixels.size();

            if (actualSize != 3) {
                LOGGER.warn("Não foi possível extrair os 3 pontos principais da imagem " + images.get(i).getName() + ". Pontos encontrados: " + actualSize);
                continue;
            }

            LOGGER.info("Imagem " + images.get(i) + " diminuida de " + initialSize + " para " + actualSize);

            TrianglePrincipalPoints principalPoints = new TrianglePrincipalPoints();
            List<Pixel> normalizedPixels = new ArrayList<>();
            for (Pixel pixel : finalPixels) {
                Pixel normalized = normalizePixels(imgProc.getWidth(), imgProc.getHeight(), pixel);
                normalizedPixels.add(normalized);
                principalPoints.addPixel(normalized);
            }

            Dimension d = getNormalizedDimensions(imgProc.getWidth(), imgProc.getHeight());
            res.add(principalPoints);
            if (debug) {
                debugContours(normalizedPixels, d.X, d.Y, imgProc.getCurrentColorModel());
            }
        }
        return res;
    }

    private int getDistanceTolerance(int width, int height, double tolerancePercentage) {
        int toleranceX = (int) (width * tolerancePercentage);
        int toleranceY = (int) (height * tolerancePercentage);
        return toleranceX + toleranceY;
    }

    private Pixel normalizePixels(int originalWidth, int originalHeight, Pixel originalPixel) {
        Pixel res = null;
        if (originalWidth != BASE_DIMENSIONS.X) {
            Dimension d = getNormalizedDimensions(originalWidth, originalHeight);
            double x = (double) originalPixel.x;
            double y = (double) originalPixel.y;
            double ratioX = (double) d.X / (double) originalWidth;
            x *= ratioX;

            double ratioY = (double) d.Y / (double) originalHeight;
            y *= ratioY;
            res = new Pixel((int) x, (int) y, originalPixel.val);
        } else {
            res = originalPixel;
        }
        return res;
    }

    private Dimension getNormalizedDimensions(int originalWidth, int originalHeight) {
        double aspect = (double) originalHeight / (double) originalWidth;
        double newHeight = aspect * BASE_DIMENSIONS.X;
        return new Dimension(BASE_DIMENSIONS.X, (int) newHeight);
    }

    /**
     * Calcula a distancia entre dois pixels
     *
     * @param p1
     * @param p2
     * @return true se a distancia entre os dois pixels é menor ou igual a distancia minima, false caso contrário
     */
    private int getDistance(Pixel p1, Pixel p2) {
        int dx = p1.x - p2.x;
        int dy = p1.y - p2.y;
        int distance = dx + dy;

        return Math.abs(distance);
    }

    /**
     * Calcula e retorna as bounding boxes com os maxX/minX/maxY/minY normalizados tendo como base a dimensão 28x28
     * É importante evidenciar que a bounding box extraída atualmente tem uma pequena taxa de erro nas coordenadas Y (maxY/minY)
     * de aproximadamente 3.5%.
     *
     * @return {@link BBox} de todas as imagens carregadas através do metodo {@link #loadImages(File[])}
     */
    private BBox getBoundingBox(ij.process.ImageProcessor imgProc) {
        List<Pixel> edgesPixels = getEdgesPixels(imgProc);
        List<Integer> xList = edgesPixels.stream().map(p -> p.x).collect(Collectors.toList());
        List<Integer> yList = edgesPixels.stream().map(p -> p.y).collect(Collectors.toList());

        int minX = xList.stream().sorted().findFirst().orElseThrow(ImageIncompatibleException::new);
        int maxX = xList.stream().sorted((x1, x2) -> x2 - x1).findFirst().orElseThrow(ImageIncompatibleException::new);
        int minY = yList.stream().sorted().findFirst().orElseThrow(ImageIncompatibleException::new);
        int maxY = yList.stream().sorted((y1, y2) -> y2 - y1).findFirst().orElseThrow(ImageIncompatibleException::new);
        return new BBox(maxX, minX, maxY, minY);
    }

    /**
     * Carrega um mapa com as coordenadas (x,y) de pixels relevantes (a.k.a pertencentes ao contorno do triangulo) da imagem.
     * Ou seja, todos as coordenadas presentes no mapa resultante são referentes a um pixel pertencente ao contorno do triângulo.
     */
    private List<Pixel> getEdgesPixels(ij.process.ImageProcessor p) {
        LOGGER.debug("Extraindo coordenadas dos pixels mais relevantes (referentes ao contorno do triangulo)");
        List<Pixel> res = new ArrayList<>();
        //para cada coluna (eixo x) da imagem atual...
        for (int x = 0; x < p.getWidth(); x++) {
            //para cada linha (eixo y) da imagem atual...
            for (int y = 0; y < p.getHeight(); y++) {
                int pixelVal = p.get(x, y);
                // se entrar neste if, significa que este pixel faz parte de um contorno
                //(parte branca da imagem tratada - olhe uma imagem tratada na pasta de imagens tratadas para mais entendimento)
                if (isPixelBlackOrWhite(pixelVal)) {
                    res.add(new Pixel(x, y, pixelVal));
                }
            }
        }
        return res;
    }

    private boolean isPixelBlackOrWhite(int pixelVal) {
        return pixelVal == WHITE_PIXEL_VAL || pixelVal == BLACK_PIXEL_VAL;
    }

}
