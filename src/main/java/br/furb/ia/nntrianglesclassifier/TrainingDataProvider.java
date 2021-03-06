package br.furb.ia.nntrianglesclassifier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fornece os exemplos de treinamento para a rede neural.
 * Entende-se "exemplos de treinamento" como a boundingBox de cada imagem de triangulo calculado e vinculado ao seu tipo ({@link TriangleTypes}).
 */
public class TrainingDataProvider {

    private static final Logger LOGGER = LogManager.getLogger();
    private final ImageProcessor imgProcessor;

    public TrainingDataProvider(ImageProcessor ip) {
        this.imgProcessor = ip;
    }

    public Map<TriangleTypes, List<TrianglePrincipalPoints>> processAndGetExamples(Map<TriangleTypes, List<File>> e, boolean debug) {
        LOGGER.info("Iniciando carregamento e preparação dos exemplos para treinamento");
        int totalTrainingExamples = 0;

        Map<TriangleTypes, List<TrianglePrincipalPoints>> examples = new HashMap<>();
        for (Map.Entry<TriangleTypes, List<File>> entry : e.entrySet()) {
            LOGGER.info("Carregando e processando exemplos do tipo " + entry.getKey().toString());
            imgProcessor.loadImages(entry.getValue().toArray(new File[0]));
            imgProcessor.convertAllTo8BitGrayScale();
            imgProcessor.binarizeImage();
            imgProcessor.convertToEdges();
            imgProcessor.cropImagesToBBox();
            imgProcessor.saveImages(Application.getTriangleImageOutputDirByType(entry.getKey())); //salva as imagens processadas pelo ImageProcessor, interessante para depuração..
            examples.put(entry.getKey(), imgProcessor.getPrincipalPoints(debug));
            totalTrainingExamples += examples.get(entry.getKey()).size();
        }

        LOGGER.info(totalTrainingExamples + " exemplos de treinamento carregados.");
        return examples;
    }


}
