package br.furb.ia.nntrianglesclassifier;

import org.encog.ConsoleStatusReportable;
import org.encog.Encog;
import org.encog.ml.MLRegression;
import org.encog.ml.data.MLData;
import org.encog.ml.data.versatile.NormalizationHelper;
import org.encog.ml.data.versatile.VersatileMLDataSet;
import org.encog.ml.data.versatile.columns.ColumnDefinition;
import org.encog.ml.data.versatile.columns.ColumnType;
import org.encog.ml.data.versatile.sources.CSVDataSource;
import org.encog.ml.data.versatile.sources.VersatileDataSource;
import org.encog.ml.factory.MLMethodFactory;
import org.encog.ml.model.EncogModel;
import org.encog.util.csv.CSVFormat;
import org.encog.util.csv.ReadCSV;
import org.encog.util.simple.EncogUtility;

import java.io.File;
import java.util.Arrays;

/**
 * Created by Thomas.Adriano on 09/06/2016.
 */
public class NeuralNetwork implements AutoCloseable {

    private VersatileMLDataSet data;
    private MLRegression bestMethod;

    public void train(final File trainingData) {
        VersatileDataSource ds = new CSVDataSource(trainingData, true, CSVFormat.DECIMAL_POINT);
        // Define the format of the data file.
        // This area will change, depending on the columns and
        // format of the file that you are trying to model.
        data = new VersatileMLDataSet(ds);
        data.defineSourceColumn("p1x", 0, ColumnType.continuous);
        data.defineSourceColumn("p1y", 1, ColumnType.continuous);
        data.defineSourceColumn("p2x", 2, ColumnType.continuous);
        data.defineSourceColumn("p2y", 3, ColumnType.continuous);
        data.defineSourceColumn("p3x", 4, ColumnType.continuous);
        data.defineSourceColumn("p3y", 5, ColumnType.continuous);

        // Define the column that we are trying to predict.
        ColumnDefinition outputColumn = data.defineSourceColumn("type", 6,
                ColumnType.nominal);

        // Analyze the data, determine the min/max/mean/sd of every column.
        data.analyze();

        // Map the prediction column to the output of the model, and all
        // other columns to the input.
        data.defineSingleOutputOthersInput(outputColumn);

        // Create feedforward neural network as the model type. MLMethodFactory.TYPE_FEEDFORWARD.
        // You could also other model types, such as:
        // MLMethodFactory.SVM:  Support Vector Machine (SVM)
        // MLMethodFactory.TYPE_RBFNETWORK: RBF Neural Network
        // MLMethodFactor.TYPE_NEAT: NEAT Neural Network
        // MLMethodFactor.TYPE_PNN: Probabilistic Neural Network
        EncogModel model = new EncogModel(data);
        model.selectMethod(data, MLMethodFactory.TYPE_FEEDFORWARD);

        // Send any output to the console.
        model.setReport(new ConsoleStatusReportable());

        // Now normalize the data.  Encog will automatically determine the correct normalization
        // type based on the model you chose in the last step.
        data.normalize();

        // Hold back some data for a final validation.
        // Shuffle the data into a random ordering.
        // Use a seed of 1001 so that we always use the same holdback and will get more consistent results.
        model.holdBackValidation(0.3, true, 1001);

        // Choose whatever is the default training type for this model.
        model.selectTrainingType(data);

        // Use a 5-fold cross-validated train.  Return the best method found.
        bestMethod = (MLRegression) model.crossvalidate(5, true);

        // Display the training and validation errors.
        System.out.println("Training error: " + EncogUtility.calculateRegressionError(bestMethod, model.getTrainingDataset()));
        System.out.println("Validation error: " + EncogUtility.calculateRegressionError(bestMethod, model.getValidationDataset()));

        // Display our normalization parameters.
        NormalizationHelper helper = data.getNormHelper();
        System.out.println(helper.toString());

        // Display the final model.
        System.out.println("Final model: " + bestMethod);
    }

    public void predict(File predictData) {
        // Loop over the entire, original, dataset and feed it through the model.
        // This also shows how you would process new data, that was not part of your
        // training set.  You do not need to retrain, simply use the NormalizationHelper
        // class.  After you train, you can save the NormalizationHelper to later
        // normalize and denormalize your data.

        ReadCSV csv = new ReadCSV(predictData, false, CSVFormat.DECIMAL_POINT);
        String[] line = new String[4];
        NormalizationHelper helper = data.getNormHelper();
        MLData input = helper.allocateInputVector();

        while (csv.next()) {
            StringBuilder result = new StringBuilder();
            line[0] = csv.get(0);
            line[1] = csv.get(1);
            line[2] = csv.get(2);
            line[3] = csv.get(3);
            String correct = csv.get(4);
            helper.normalizeInputVector(line, input.getData(), false);
            MLData output = bestMethod.compute(input);
            String irisChosen = helper.denormalizeOutputVectorToString(output)[0];

            result.append(Arrays.toString(line));
            result.append(" -> predicted: ");
            result.append(irisChosen);
            result.append("(correct: ");
            result.append(correct);
            result.append(")");

            System.out.println(result.toString());
        }
    }

    @Override
    public void close() throws Exception {
        Encog.getInstance().shutdown();
    }
}
