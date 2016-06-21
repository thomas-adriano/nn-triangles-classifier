package br.furb.ia.nntrianglesclassifier;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

// - A pasta main/java/resources/images contém as imagens exemplo origem. Para adicionar novas imagens basta copiá-las
//para o subdiretorio respectivo (equilateral, etc..).
// - A pasta main/java/resources/processedImages contém as imagens origem processadas para facilitar o entendimento do que
//acontece no processamento delas. Para criar mais deve-se usar o metodo ImageProcessor.saveImages();


//TODO: se a acertividade da rede neural estiver baixa, calibra-la para ter uma boa taxa de acertividade
public class Application {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final File TRAINING_IMAGES_OUTPUT_DIR = new File("src/main/resources/training/processedImages");
    public static final File TRAINING_EQUILATERAL_TRIANGLES_OUTPUT_DIR = new File(TRAINING_IMAGES_OUTPUT_DIR, "equilateral");
    public static final File TRAINING_SCALENE_TRIANGLES_OUTPUT_DIR = new File(TRAINING_IMAGES_OUTPUT_DIR, "scalene");
    public static final File TRAINING_ISOSCELES_TRIANGLES_OUTPUT_DIR = new File(TRAINING_IMAGES_OUTPUT_DIR, "isosceles");
    public static final File CLASSIFICATION_IMAGES_OUTPUT_DIR = new File("src/main/resources/training/processedImages");
    public static final File CLASSIFICATION_EQUILATERAL_TRIANGLES_OUTPUT_DIR = new File(TRAINING_IMAGES_OUTPUT_DIR, "equilateral");
    public static final File CLASSIFICATION_SCALENE_TRIANGLES_OUTPUT_DIR = new File(TRAINING_IMAGES_OUTPUT_DIR, "scalene");
    public static final File CLASSIFICATION_ISOSCELES_TRIANGLES_OUTPUT_DIR = new File(TRAINING_IMAGES_OUTPUT_DIR, "isosceles");

    public static final String TRAINING_IMAGES_PATH = "/training/images";
    public static final String TRAINING_EQUILATERAL_TRIANGLES_IMAGE_PATH = TRAINING_IMAGES_PATH + "/equilateral";
    public static final String TRAINING_ISOSCELES_TRIANGLES_IMAGE_PATH = TRAINING_IMAGES_PATH + "/isosceles";
    public static final String TRAINING_SCALENE_TRIANGLES_IMAGE_PATH = TRAINING_IMAGES_PATH + "/scalene";
    public static final String CLASSIFICATION_IMAGES_PATH = "/classification/images";
    public static final String CLASSIFICATION_EQUILATERAL_TRIANGLES_IMAGE_PATH = TRAINING_IMAGES_PATH + "/equilateral";
    public static final String CLASSIFICATION_ISOSCELES_TRIANGLES_IMAGE_PATH = TRAINING_IMAGES_PATH + "/isosceles";
    public static final String CLASSIFICATION_SCALENE_TRIANGLES_IMAGE_PATH = TRAINING_IMAGES_PATH + "/scalene";

    public static final File TRAINING_CSV = new File("training.csv");
    public static final File CLASSIFICATION_CSV = new File("classification.csv");

    public static void main(String[] args) {
        long init = System.currentTimeMillis();
        LOGGER.info("Iniciando execução...");

        createCSVFiles();  //linha comentada pq os arquivos CSV de treinamento já estão criados

        try (NeuralNetwork nn = new NeuralNetwork()) {
            nn.train(TRAINING_CSV);
            nn.predict(CLASSIFICATION_CSV);
        } catch (Exception e) {
            e.printStackTrace();
        }


        double elapsed = (System.currentTimeMillis() - init) / 1000;
        LOGGER.info("Tempo de execução total: " + elapsed + " segundos.");
    }

    private enum DataType {
        TRAINING, CLASSIFICATION
    }

    private static void createCSVFiles() {
        Map<TriangleTypes, List<TrianglePrincipalPoints>> trainingData = loadData(DataType.TRAINING);
        writeToCSV(trainingData, TRAINING_CSV);

//        Map<TriangleTypes, List<TrianglePrincipalPoints>> classificationData = loadData(DataType.CLASSIFICATION);
//        writeToCSV(classificationData, CLASSIFICATION_CSV);
    }


    private static Map<TriangleTypes, List<TrianglePrincipalPoints>> loadData(DataType dt) {
        TrainingDataProvider tp = new TrainingDataProvider(new ImageProcessor());

        List<File> eqTriangles = new ArrayList<>();
        List<File> isoTriangles = new ArrayList<>();
        List<File> scaTriangles = new ArrayList<>();

        //carrega todos os arquivos presentes nas pastas de imagens e adiciona em sua respectiva colecao
        eqTriangles.addAll(Arrays.asList(ResourceLoader.getResources(getTriangleImageOriginDirByType(TriangleTypes.EQUILATERAL, dt))));
        isoTriangles.addAll(Arrays.asList(ResourceLoader.getResources(getTriangleImageOriginDirByType(TriangleTypes.ISOSCELES, dt))));
        scaTriangles.addAll(Arrays.asList(ResourceLoader.getResources(getTriangleImageOriginDirByType(TriangleTypes.SCALENE, dt))));

        Map<TriangleTypes, List<File>> trainintFiles = new HashMap<>();
        trainintFiles.put(TriangleTypes.EQUILATERAL, eqTriangles);
        trainintFiles.put(TriangleTypes.ISOSCELES, isoTriangles);
        trainintFiles.put(TriangleTypes.SCALENE, scaTriangles);

        return tp.processAndGetExamples(trainintFiles, true /*loga ascii art representando os três pontos extraídos de cada uma das imagens*/);
    }

    public static final File getTriangleImageOutputDirByType(TriangleTypes t) {
        switch (t) {
            case EQUILATERAL:
                return TRAINING_EQUILATERAL_TRIANGLES_OUTPUT_DIR;
            case ISOSCELES:
                return TRAINING_ISOSCELES_TRIANGLES_OUTPUT_DIR;
            case SCALENE:
                return TRAINING_SCALENE_TRIANGLES_OUTPUT_DIR;
            default:
                return null;
        }
    }

    public static final String getTriangleImageOriginDirByType(TriangleTypes t, DataType dt) {
        switch (t) {
            case EQUILATERAL:
                return dt == DataType.TRAINING ? TRAINING_EQUILATERAL_TRIANGLES_IMAGE_PATH : CLASSIFICATION_EQUILATERAL_TRIANGLES_IMAGE_PATH;
            case ISOSCELES:
                return dt == DataType.TRAINING ? TRAINING_ISOSCELES_TRIANGLES_IMAGE_PATH : CLASSIFICATION_ISOSCELES_TRIANGLES_IMAGE_PATH;
            case SCALENE:
                return dt == DataType.TRAINING ? TRAINING_SCALENE_TRIANGLES_IMAGE_PATH : CLASSIFICATION_SCALENE_TRIANGLES_IMAGE_PATH;
            default:
                return null;
        }
    }

    public static void writeToCSV(Map<TriangleTypes, List<TrianglePrincipalPoints>> trainData, File csvDestPath) {
        String[] csvHeaders = new String[]{"p1x", "p1y", "p2x", "p2y", "p3x", "p3y", "type"};

        Collection<String[]> csvEntries = new ArrayList<>();

        for (Map.Entry<TriangleTypes, List<TrianglePrincipalPoints>> e : trainData.entrySet()) {
            for (TrianglePrincipalPoints p : e.getValue()) {
                int actualIndex = 0;
                String[] line = new String[7];
                for (Pixel pixel : p.pixels()) {
                    if (pixel != null) {
                        for (int coord : pixel.values()) {
                            line[actualIndex] = String.valueOf(coord);
                            actualIndex++;
                        }
                    }
                }
                line[6] = String.valueOf(e.getKey().getCharValue());
                csvEntries.add(line);
            }
        }

        try (FileWriter fw = new FileWriter(csvDestPath)) {
            try (CSVPrinter p = new CSVPrinter(fw, CSVFormat.DEFAULT.withHeader(csvHeaders))) {
                p.printRecords(csvEntries);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
