package org.knime.al.util.novelty.knfst;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.junit.Test;
import org.knime.al.util.noveltydetection.knfst.MatrixFunctions;

public class MatrixFunctionsTest {

    RealMatrix matrixA = MatrixUtils.createRealMatrix(
            new double[][] { { 1, 2, 3 }, { 1, 2, 3 }, { 1, 2, 3 } });
    RealMatrix matrixI = MatrixUtils.createRealIdentityMatrix(3);
    double[] testArray = new double[] { -1, 1, 0, -2000 };
    RealVector vector =
            MatrixUtils.createRealVector(new double[] { 1, 2, 3, 4 });

    @Test
    public void testMatrixEquality() {
        final RealMatrix m1 = MatrixUtils
                .createRealMatrix(new double[][] { { 1, 2 }, { 1, 2 } });
        final RealMatrix m2 = m1.copy();
        assertEquals(m1, m2);
    }

    @Test
    public void testColumnMeans() {
        final RealVector expected =
                MatrixUtils.createRealVector(new double[] { 1, 2, 3 });
        assertEquals(expected, MatrixFunctions.columnMeans(matrixA));
    }

    @Test
    public void testMean() {
        final double expected = 2;
        assertEquals(expected, MatrixFunctions.mean(matrixA), 0);
    }

    @Test
    public void testOnes() {
        final RealMatrix expected1 = MatrixUtils.createRealMatrix(
                new double[][] { { 1, 1, 1 }, { 1, 1, 1 }, { 1, 1, 1 } });
        assertEquals(expected1, MatrixFunctions.ones(3, 3));
        final RealMatrix expected2 = MatrixUtils.createRealMatrix(
                new double[][] { { 1, 1 }, { 1, 1 }, { 1, 1 } });
        assertEquals(expected2, MatrixFunctions.ones(3, 2));
        final RealMatrix expected3 = MatrixUtils
                .createRealMatrix(new double[][] { { 1, 1, 1 }, { 1, 1, 1 } });
        assertEquals(expected3, MatrixFunctions.ones(2, 3));
    }

    @Test
    public void testNullspace() {
        final double expected = 0;
        final RealMatrix nullspace = MatrixFunctions.nullspace(matrixA);
        final RealMatrix result = matrixA.multiply(nullspace);
        final double[][] resultData = result.getData();
        for (final double[] row : resultData) {
            for (final double cell : row) {
                assertEquals(expected, cell, 1e-12);
            }
        }

        assertNull(MatrixFunctions.nullspace(matrixI));
    }

    @Test
    public void testAbs() {
        final double[] expected = new double[] { 1, 1, 0, 2000 };
        assertArrayEquals(expected, MatrixFunctions.abs(testArray), 0);
    }

    @Test
    public void testArgmin() {
        final int expected = 3;
        assertEquals(expected, MatrixFunctions.argmin(testArray));
    }

    @Test
    public void testRowMins() {
        final RealVector expected =
                MatrixUtils.createRealVector(new double[] { 1, 1, 1 });
        assertEquals(expected, MatrixFunctions.rowMins(matrixA));
    }

    @Test
    public void testSqrtMatrix() {
        final RealMatrix result = MatrixFunctions.sqrt(matrixA);
        for (int r = 0; r < result.getRowDimension(); r++) {
            for (int c = 0; c < result.getColumnDimension(); c++) {
                assertEquals(Math.sqrt(matrixA.getEntry(r, c)),
                        result.getEntry(r, c), 0);
            }
        }
    }

    @Test
    public void testSqrtVector() {
        final RealVector result = MatrixFunctions.sqrt(vector);
        for (int i = 0; i < result.getDimension(); i++) {
            assertEquals(Math.sqrt(vector.getEntry(i)), result.getEntry(i), 0);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExceptionMultiplyElementWise() {
        final RealMatrix m1 = MatrixUtils
                .createRealMatrix(new double[][] { { 1, 2, 3 }, { 1, 2, 3 } });
        MatrixFunctions.multiplyElementWise(m1, matrixA);
    }

    @Test
    public void testMultiplyElementWise() {
        final RealMatrix expected = MatrixUtils.createRealMatrix(
                new double[][] { { 1, 0, 0 }, { 0, 2, 0 }, { 0, 0, 3 } });
        final RealMatrix result =
                MatrixFunctions.multiplyElementWise(matrixI, matrixA);
        assertEquals(expected, result);
    }

    @Test
    public void testRowSums() {
        final RealVector expected =
                MatrixUtils.createRealVector(new double[] { 6, 6, 6 });
        assertEquals(expected, MatrixFunctions.rowSums(matrixA));
    }

    @Test
    public void testPow() {
        final RealMatrix result = MatrixFunctions.pow(matrixA, 2);
        for (int r = 0; r < result.getRowDimension(); r++) {
            for (int c = 0; c < result.getColumnDimension(); c++) {
                assertEquals(Math.pow(matrixA.getEntry(r, c), 2),
                        result.getEntry(r, c), 0);
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExceptionConcatHorizontally() {
        final RealMatrix m1 = MatrixUtils
                .createRealMatrix(new double[][] { { 1, 2, 3 }, { 1, 2, 3 } });
        MatrixFunctions.concatHorizontally(m1, matrixA);
    }

    @Test
    public void testConcatHorizontally() {
        final RealMatrix expected = MatrixUtils
                .createRealMatrix(new double[][] { { 1, 0, 0, 1, 2, 3 },
                        { 0, 1, 0, 1, 2, 3 }, { 0, 0, 1, 1, 2, 3 } });
        assertEquals(expected,
                MatrixFunctions.concatHorizontally(matrixI, matrixA));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExceptionConcatVertically() {
        final RealMatrix m1 = MatrixUtils
                .createRealMatrix(new double[][] { { 1, 2 }, { 1, 2 } });
        MatrixFunctions.concatVertically(m1, matrixA);
    }

    @Test
    public void testConcatVertically() {
        final RealMatrix expected = MatrixUtils
                .createRealMatrix(new double[][] { { 1, 0, 0 }, { 0, 1, 0 },
                        { 0, 0, 1 }, { 1, 2, 3 }, { 1, 2, 3 }, { 1, 2, 3 } });

        assertEquals(expected, MatrixFunctions.concatVertically(matrixI, matrixA));
    }

    @Test
    public void testCalculateRowVectorDistances() {
        final RealMatrix m1 = MatrixUtils.createRealMatrix(
                new double[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
        final double[] expected =
                new double[] { Math.sqrt(27), Math.sqrt(108), Math.sqrt(27) };
        final double[] result = MatrixFunctions.calculateRowVectorDistances(m1);

        for (int i = 0; i < result.length; i++) {
            assertEquals(expected[i], result[i], 1e-12);
        }
    }
}
