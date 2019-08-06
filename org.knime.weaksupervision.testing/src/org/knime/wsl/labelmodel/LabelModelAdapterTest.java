package org.knime.wsl.labelmodel;

import static org.junit.Assert.assertEquals;
import static org.knime.wsl.testing.TestUtil.assertMatrixEquals;
import static org.knime.wsl.testing.TestUtil.findInPlugin;
import static org.knime.wsl.testing.TestUtil.readFloatMatrixFromCSV;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knime.wsl.testing.TestUtil;

public class LabelModelAdapterTest {

    private static final String LABEL_MODEL_PATH = getLabelModelPath();

    private static String getLabelModelPath() {
        try {
            return TestUtil.findInPlugin("/resources/labelmodel/labelModel").getAbsolutePath();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private LabelModelAdapter m_testInstance;

    private static float[][] AUGMENTED_LABEL_MATRIX;

    private static float[] PREC_INIT;

    private static float[] CLASS_BALANCE;

    private static boolean[][] MASK;

    @BeforeClass
    public static void initClass() throws FileNotFoundException, IOException {
        AUGMENTED_LABEL_MATRIX = oneHotEncode(readFloatMatrixFromCSV(findInPlugin("/files/test_files/l_test.csv")), 5);
        PREC_INIT = new float[10];
        Arrays.fill(PREC_INIT, 0.7f);
        CLASS_BALANCE = new float[5];
        Arrays.fill(CLASS_BALANCE, 0.2f);
        MASK = createMask(10, 5);
    }

    @Before
    public void init() {
        m_testInstance = new LabelModelAdapter(LABEL_MODEL_PATH);
    }

    @After
    public void shutdown() throws Exception {
        m_testInstance.close();
    }

    @Test
    public void testInitialize() throws Exception {
        float[][] muInit = readFloatMatrixFromCSV(findInPlugin("/files/test_files/mu_init.csv"));

        float lr = 0.03f;
        boolean[][] mask = createMask(10, 5);
        m_testInstance.initialize(AUGMENTED_LABEL_MATRIX, mask, CLASS_BALANCE, PREC_INIT, lr);

        assertEquals(lr, m_testInstance.getLearningRate(), 0);

        assertMatrixEquals(muInit, m_testInstance.getMu(), 1e-5f);
    }

    @Test
    public void testTrainStep() throws Exception {
        float lr = 0.01f;
        m_testInstance.initialize(AUGMENTED_LABEL_MATRIX, MASK, CLASS_BALANCE, PREC_INIT, lr);
        assertEquals(0.297f, m_testInstance.trainStep(), 0.001);
    }

    @Test
    public void testTrain() throws Exception {
        train();
        float[][] muFinal = readFloatMatrixFromCSV(findInPlugin("/files/test_files/mu_final.csv"));
        assertMatrixEquals(muFinal, m_testInstance.getMu(), 1e-5f);
    }

    @Test
    public void testGetProbabilities() throws Exception {
        train();
        float[][] conditionalProbabilities =
            readFloatMatrixFromCSV(findInPlugin("files/test_files/probabilistic_labels.csv"));
        assertMatrixEquals(conditionalProbabilities, m_testInstance.getProbabilities(AUGMENTED_LABEL_MATRIX), 1e-5f);
    }

    private void train() {
        m_testInstance.initialize(AUGMENTED_LABEL_MATRIX, MASK, CLASS_BALANCE, PREC_INIT, 0.01f);
        for (int i = 0; i < 100; i++) {
            m_testInstance.trainStep();
        }
    }

    private static float[][] oneHotEncode(final float[][] labelMatrix, final int cardinality) {
        final int numSources = labelMatrix[0].length;
        final float[][] augmented = new float[labelMatrix.length][numSources * cardinality];
        for (int i = 0; i < augmented.length; i++) {
            for (int j = 0; j < numSources; j++) {
                int label = (int)labelMatrix[i][j];
                if (label > 0) {
                    augmented[i][cardinality * j + label - 1] = 1;
                }
            }
        }
        return augmented;
    }

    private static boolean[][] createMask(final int numSources, final int cardinality) {
        final int dim = numSources * cardinality;
        final boolean[][] mask = allTrueSquareMatrix(dim);
        for (int i = 0; i < numSources; i++) {
            int pos = i * cardinality;
            for (int j = 0; j < cardinality; j++) {
                Arrays.fill(mask[pos + j], pos, (i + 1) * cardinality, false);
            }
        }
        return mask;
    }

    private static boolean[][] allTrueSquareMatrix(final int dim) {
        final boolean[][] mask = new boolean[dim][dim];
        for (int i = 0; i < dim; i++) {
            Arrays.fill(mask[i], true);
        }
        return mask;
    }
}
