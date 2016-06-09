package br.furb.ia.nntrianglesclassifier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// - A pasta main/java/resources/images contém as imagens exemplo origem. Qualquer adição deve ser armazenada neste diretorio.
// - A pasta main/java/resources/processedImages contém as imagens origem processadas para facilitar o entendimento do que
//acontece no processamento delas. Para criar mais deve-se usar o metodo ImageProcessor.saveImages();


//TODO: criar mais arquivos de imagem exemplos (nao precisam ser 28x28, seria até melhor que fossem de outras dimensões)
//TODO: desenvolver a rede neural e alimenta-la com os dados do treinamento (TrainingDataProvider.processAndGetExamples)
//TODO: (depende do todo anterior) calibrar a rede neural para ter uma boa taxa de acertividade
public class Application {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final File IMAGES_OUTPUT_DIR = new File("src/main/resources/processedImages");
    public static final String IMAGES_PATH = "/images";

    public static void main(String[] args) {
        long init = System.currentTimeMillis();
        LOGGER.info("Iniciando execução...");

        Map<TriangleTypes, List<BBox>> trainingData = loadTrainingData();

//        NeuralNetwork nn = new NeuralNetwork();
//        nn.doTheMagic(ip.getImagePixelArray());


        double elapsed = (System.currentTimeMillis() - init) / 1000;
        LOGGER.info("Tempo de execução total: " + elapsed + " segundos.");
    }

    public static Map<TriangleTypes, List<BBox>> loadTrainingData() {
        TrainingDataProvider tp = new TrainingDataProvider(new ImageProcessor());

        List<File> eqTriangles = new ArrayList<>();
        List<File> isoTriangles = new ArrayList<>();
        List<File> scaTriangles = new ArrayList<>();

        eqTriangles.add(ResourceLoader.getResource(IMAGES_PATH + "/equilateral_1_28x28.jpg"));
        isoTriangles.add(ResourceLoader.getResource(IMAGES_PATH + "/isosceles_1_28x28.jpg"));
        scaTriangles.add(ResourceLoader.getResource(IMAGES_PATH + "/scalene_1_28x28.jpg"));

        Map<TriangleTypes, List<File>> trainintFiles = new HashMap<>();
        trainintFiles.put(TriangleTypes.EQUILATERAL, eqTriangles);
        trainintFiles.put(TriangleTypes.ISOSCELES, isoTriangles);
        trainintFiles.put(TriangleTypes.SCALENE, scaTriangles);

        return tp.processAndGetExamples(trainintFiles);
    }

}
