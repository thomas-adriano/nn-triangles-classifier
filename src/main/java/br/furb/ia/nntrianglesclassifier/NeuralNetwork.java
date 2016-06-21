package br.furb.ia.nntrianglesclassifier;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.encog.Encog;
import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataPair;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Thomas.Adriano on 09/06/2016.
 */
public class NeuralNetwork implements AutoCloseable {

    private BasicNetwork network;
    private static final Logger LOGGER = LogManager.getLogger();

    private BasicMLDataSet readCSV(File f) {
        List<MLDataPair> dataPairs = new ArrayList<>();
        try (CSVParser p = CSVParser.parse(f, StandardCharsets.UTF_8, org.apache.commons.csv.CSVFormat.DEFAULT)) {
            for (CSVRecord rec : p.getRecords()) {
                double[] inputs = new double[4];
                TriangleTypes t = TriangleTypes.fromChar(rec.get(3).charAt(0));
                double[] ideal = new double[]{t.getDoubleValue()};

                for (int i = 0; i < rec.size() - 1 /* subtrai a coluna resultado*/; i++) {
                    inputs[i] =Double.valueOf(rec.get(i));
                }

                BasicMLData inp = new BasicMLData(inputs);
                BasicMLData ide = new BasicMLData(ideal);
                dataPairs.add(new BasicMLDataPair(inp, ide));
            }

        } catch (IOException | NumberFormatException e) {
            throw new RuntimeException("Não foi possível parsear o conteudo do arquivo csv " + f, e);
        }
        return new BasicMLDataSet(dataPairs);
    }

    public void train(final File trainingData) {
        MLDataSet data = readCSV(trainingData);
        // multilayered perceptron
        network = new BasicNetwork();
        network.addLayer(new BasicLayer(null, true, data.getInputSize()));
        network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 8));
        network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 8));
        network.addLayer(new BasicLayer(new ActivationSigmoid(), false, 1));
        network.getStructure().finalizeStructure();
        network.reset();

        ResilientPropagation train = new ResilientPropagation(network, data);

        int epoch = 1;

        do {
            train.iteration();
            LOGGER.debug(("Epoch #" + epoch + " Error:" + train.getError()));
            epoch++;
        } while (epoch < 500);

        // test the neural network
        LOGGER.debug(("Neural Network Results:"));
        for (MLDataPair pair : data) {
            final MLData output = network.compute(pair.getInput());
            LOGGER.debug((pair.getInput().getData(0) + "," + pair.getInput().getData(1) + "," + pair.getInput().getData(2)
                    + ", actual=" + output.getData(0) + ",ideal=" + pair.getIdeal().getData(0)));
        }

        LOGGER.debug(("Quantidade de épocas: " + epoch));
        LOGGER.debug(("Taxa de erro atingida: " + train.getError()));
        LOGGER.debug(("Quantidade de imagens utilizadas no treino: " + data.size()));
    }

    public void predict(File predictData) {
        MLDataSet data = readCSV(predictData);
        for (MLDataPair pair : data) {
            MLData output = network.compute(pair.getInput());
            LOGGER.debug("expected: " + pair.getIdeal().getData(0) + ", predicted: " + output.getData(0));
        }

    }

    @Override
    public void close() throws Exception {
        Encog.getInstance().shutdown();
    }
}
