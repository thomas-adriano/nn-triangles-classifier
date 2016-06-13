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


//TODO: criar mais arquivos de imagem exemplos (nao precisam ser 28x28, seria até melhor que fossem de outras dimensões)
//TODO: se a acertividade da rede neural estiver baixa, calibra-la para ter uma boa taxa de acertividade
//TODO: melhorar algoritmo de extração de dados das imagnes para ao invés de simplesmente extrair a bbox, extraia os
//pontos mais significativos (que seriam as coordenadas de cada angulo do triangulo, ou seja, 3 coordenadas sempre).
//Mais sobre o pq dessa necessidade está descrito na classe ImageProcessor.
public class Application {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final File IMAGES_OUTPUT_DIR = new File("src/main/resources/processedImages");
    public static final File EQUILATERAL_TRIANGLES_OUTPUT_DIR = new File(IMAGES_OUTPUT_DIR, "equilateral");
    public static final File SCALENE_TRIANGLES_OUTPUT_DIR = new File(IMAGES_OUTPUT_DIR, "scalene");
    public static final File ISOSCELES_TRIANGLES_OUTPUT_DIR = new File(IMAGES_OUTPUT_DIR, "isosceles");
    public static final String IMAGES_PATH = "/images";
    public static final String EQUILATERAL_TRIANGLES_IMAGE_PATH = IMAGES_PATH + "/equilateral";
    public static final String ISOSCELES_TRIANGLES_IMAGE_PATH = IMAGES_PATH + "/isosceles";
    public static final String SCALENE_TRIANGLES_IMAGE_PATH = IMAGES_PATH + "/scalene";

    public static void main(String[] args) {
        long init = System.currentTimeMillis();
        LOGGER.info("Iniciando execução...");

        Map<TriangleTypes, List<BBox>> trainingData = loadTrainingData();
        File csvFile = new File("out.csv");
        writeToCSV(trainingData, csvFile);
        NeuralNetwork nn = new NeuralNetwork();
        nn.doTheMagic(csvFile);


        double elapsed = (System.currentTimeMillis() - init) / 1000;
        LOGGER.info("Tempo de execução total: " + elapsed + " segundos.");
    }

    public static Map<TriangleTypes, List<BBox>> loadTrainingData() {
        TrainingDataProvider tp = new TrainingDataProvider(new ImageProcessor());

        List<File> eqTriangles = new ArrayList<>();
        List<File> isoTriangles = new ArrayList<>();
        List<File> scaTriangles = new ArrayList<>();

        //carrega todos os arquivos presentes nas pastas de imagens e adiciona em sua respectiva colecao
        eqTriangles.addAll(Arrays.asList(ResourceLoader.getResources(getTriangleImageOriginDirByType(TriangleTypes.EQUILATERAL))));
        isoTriangles.addAll(Arrays.asList(ResourceLoader.getResources(getTriangleImageOriginDirByType(TriangleTypes.ISOSCELES))));
        scaTriangles.addAll(Arrays.asList(ResourceLoader.getResources(getTriangleImageOriginDirByType(TriangleTypes.SCALENE))));

        Map<TriangleTypes, List<File>> trainintFiles = new HashMap<>();
        trainintFiles.put(TriangleTypes.EQUILATERAL, eqTriangles);
        trainintFiles.put(TriangleTypes.ISOSCELES, isoTriangles);
        trainintFiles.put(TriangleTypes.SCALENE, scaTriangles);

        return tp.processAndGetExamples(trainintFiles);
    }

    public static final File getTriangleImageOutputDirByType(TriangleTypes t) {
        switch (t) {
            case EQUILATERAL:
                return EQUILATERAL_TRIANGLES_OUTPUT_DIR;
            case ISOSCELES:
                return ISOSCELES_TRIANGLES_OUTPUT_DIR;
            case SCALENE:
                return SCALENE_TRIANGLES_OUTPUT_DIR;
            default:
                return null;
        }
    }

    public static final String getTriangleImageOriginDirByType(TriangleTypes t) {
        switch (t) {
            case EQUILATERAL:
                return EQUILATERAL_TRIANGLES_IMAGE_PATH;
            case ISOSCELES:
                return ISOSCELES_TRIANGLES_IMAGE_PATH;
            case SCALENE:
                return SCALENE_TRIANGLES_IMAGE_PATH;
            default:
                return null;
        }
    }

    public static void writeToCSV(Map<TriangleTypes, List<BBox>> trainData, File csvDestPath) {
        String[] csvHeaders = new String[]{"maxx", "minx", "maxy", "miny", "type"};

        Collection<String[]> csvEntries = new ArrayList<>();

        for (Map.Entry<TriangleTypes, List<BBox>> e : trainData.entrySet()) {
            String[] line = new String[5];
            for (BBox b : e.getValue()) {
                for (int i = 0; i < b.values().size(); i++) {
                    line[i] = String.valueOf(b.values().get(i));
                }
                line[4] = String.valueOf(e.getKey().getCharValue());
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
