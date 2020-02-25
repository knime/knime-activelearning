package org.knime.wsl.weaklabelmodel;

import static org.junit.Assert.*;
import static org.knime.wsl.testing.TestUtil.assertMatrixEquals;
import static org.knime.wsl.testing.TestUtil.findInPlugin;
import static org.knime.wsl.testing.TestUtil.fa;
import static org.knime.wsl.testing.TestUtil.fm;
import static org.knime.wsl.testing.TestUtil.*;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class SavedModelAdapterTest {

    private static final String TAG = "test_model";

    private static final String FLOAT_VAR = "float_var";

    private static final String FLOAT_ASSIGN = "float_assign";

    private static final String FLOAT_PLACEHOLDER = "float_placeholder";

    private static final String TEST_MODEL_PATH = getTestModelPath();

    private SavedModelAdapter m_testInstance;

    private static String getTestModelPath() {
        try {
            return findInPlugin("/files/test_model").getAbsolutePath();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Before
    public void init() throws IOException {
        m_testInstance = new SavedModelAdapter(TEST_MODEL_PATH, TAG);
    }

    @After
    public void shutdown() throws Exception {
        m_testInstance.close();
    }

    @Test
    public void testSetGetFloatScalar() throws Exception {
        m_testInstance.set(FLOAT_PLACEHOLDER, FLOAT_ASSIGN, 3.0f);
        final float fetched = m_testInstance.getFloatScalar(FLOAT_VAR);
        assertEquals(3.0f, fetched, 0);
    }

    @Test
    public void testSetGetFloatVector() throws Exception {
        final float[] array = fa(1, 2, 3);
        m_testInstance.set(FLOAT_PLACEHOLDER, FLOAT_ASSIGN, array);
        final float[] fetched = m_testInstance.getFloatVector(FLOAT_VAR);
        assertArrayEquals(array, fetched, 0);
    }

    @Test
    public void testSetGetFloatMatrix() throws Exception {
        final float[][] matrix = fm(fa(1, 2, 3), fa(4, 5, 6), fa(7, 8, 9));
        m_testInstance.set(FLOAT_PLACEHOLDER, FLOAT_ASSIGN, matrix);
        final float[][] fetched = m_testInstance.getFloatMatrix(FLOAT_VAR);
        assertMatrixEquals(matrix, fetched, 0);
    }
    
    @Test
    public void testFeedGetFloatMatrix() throws Exception {
        final float[][] matrix = fm(fa(1, 2, 3), fa(4, 5, 6), fa(7, 8, 9));
        final float[][] result = m_testInstance.getFloatMatrix(FLOAT_PLACEHOLDER, matrix, FLOAT_ASSIGN);
        assertMatrixEquals(matrix, result, 0);
    }

    @Test
    public void testSetGetBooleanMatrix() throws Exception {
        final boolean[][] matrix =
            bm(ba(true, false, true), ba(false, false, false), ba(false, true, false));
        m_testInstance.set("boolean_placeholder", "boolean_assign", matrix);
        final boolean[][] fetched = m_testInstance.getBooleanMatrix("boolean_var");
        assertMatrixEquals(matrix, fetched);
    }
    
    @Test (expected = IllegalStateException.class)
    public void testSetScalarGetVector() throws Exception {
        m_testInstance.set(FLOAT_PLACEHOLDER, FLOAT_ASSIGN, 3.0f);
        m_testInstance.getFloatVector(FLOAT_VAR);
    }
    
    @Test (expected = IllegalStateException.class)
    public void testSetScalarGetMatrix() throws Exception {
        m_testInstance.set(FLOAT_PLACEHOLDER, FLOAT_ASSIGN, 3.0f);
        m_testInstance.getFloatMatrix(FLOAT_VAR);
    }
    
    @Test (expected = IllegalStateException.class)
    public void testSetVectorGetScalar() throws Exception {
        m_testInstance.set(FLOAT_PLACEHOLDER, FLOAT_ASSIGN, fa(1, 2, 3));
        m_testInstance.getFloatScalar(FLOAT_VAR);
    }
}
