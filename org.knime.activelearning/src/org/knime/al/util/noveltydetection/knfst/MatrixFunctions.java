/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 */

package org.knime.al.util.noveltydetection.knfst;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;

public class MatrixFunctions {

    public static RealVector columnMeans(final RealMatrix matrix) {
        final double[] columnMeans = new double[matrix.getColumnDimension()];
        for (int c = 0; c < matrix.getColumnDimension(); c++) {
            final double[] column = matrix.getColumn(c);
            double sum = 0;
            for (int r = 0; r < column.length; r++) {
                sum += column[r];
            }
            columnMeans[c] = sum / column.length;
        }

        return MatrixUtils.createRealVector(columnMeans);
    }

    public static double mean(final RealMatrix matrix) {
        double mean = 0;
        final double[] columnMeans = columnMeans(matrix).toArray();
        for (final double c : columnMeans) {
            mean += c;
        }
        mean /= columnMeans.length;
        return mean;
    }

    public static RealMatrix ones(final int rowCount, final int columnCount) {
        final RealMatrix ones =
                MatrixUtils.createRealMatrix(rowCount, columnCount);
        return ones.scalarAdd(1);
    }

    public static RealMatrix nullspace(final RealMatrix matrix) {
        final SingularValueDecomposition svd =
                new SingularValueDecomposition(matrix);
        final int rank = svd.getRank();
        final RealMatrix V = svd.getV();
        RealMatrix nullspace = null;
        if (rank < matrix.getColumnDimension()) {
            nullspace = V.getSubMatrix(0, V.getRowDimension() - 1, rank,
                    V.getColumnDimension() - 1);
        }

        return nullspace;
    }

    public static double[] abs(final double[] array) {
        final double[] abs = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            abs[i] = Math.abs(array[i]);
        }
        return abs;
    }

    public static int argmin(final double[] array) {
        int index = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] < array[index]) {
                index = i;
            }
        }
        return index;
    }

    public static RealVector rowMins(final RealMatrix matrix) {
        final RealVector rowMins = MatrixUtils
                .createRealVector(new double[matrix.getRowDimension()]);

        for (int r = 0; r < matrix.getRowDimension(); r++) {
            rowMins.setEntry(r, matrix.getRowVector(r).getMinValue());
        }

        return rowMins;
    }

    public static RealMatrix sqrt(final RealMatrix matrix) {
        final double[][] data = matrix.getData();
        for (int r = 0; r < matrix.getRowDimension(); r++) {
            for (int c = 0; c < matrix.getColumnDimension(); c++) {
                data[r][c] = Math.sqrt(data[r][c]);
            }
        }
        return MatrixUtils.createRealMatrix(data);
    }

    public static RealVector sqrt(final RealVector vector) {
        final RealVector result = vector.copy();
        for (int e = 0; e < result.getDimension(); e++) {
            result.setEntry(e, Math.sqrt(result.getEntry(e)));
        }
        return result;
    }

    public static RealMatrix multiplyElementWise(final RealMatrix matrix1,
            final RealMatrix matrix2) {
        if (matrix1.getRowDimension() != matrix2.getRowDimension() || matrix1
                .getColumnDimension() != matrix2.getColumnDimension()) {
            throw new IllegalArgumentException(
                    "The matrices must be of the same dimensions!");
        }

        final RealMatrix result = matrix1.createMatrix(
                matrix1.getRowDimension(), matrix1.getColumnDimension());

        for (int r = 0; r < matrix1.getRowDimension(); r++) {
            for (int c = 0; c < matrix1.getColumnDimension(); c++) {
                result.setEntry(r, c,
                        matrix1.getEntry(r, c) * matrix2.getEntry(r, c));
            }
        }

        return result;
    }

    public static RealVector rowSums(final RealMatrix matrix) {
        final RealVector rowSums = MatrixUtils
                .createRealVector(new double[matrix.getRowDimension()]);
        for (int r = 0; r < matrix.getRowDimension(); r++) {
            final double[] row = matrix.getRow(r);
            for (final double cell : row) {
                rowSums.addToEntry(r, cell);
            }
        }
        return rowSums;
    }

    public static RealMatrix pow(final RealMatrix matrix, final double power) {
        final RealMatrix result = matrix.createMatrix(matrix.getRowDimension(),
                matrix.getColumnDimension());
        for (int r = 0; r < result.getRowDimension(); r++) {
            for (int c = 0; c < result.getColumnDimension(); c++) {
                result.setEntry(r, c, Math.pow(matrix.getEntry(r, c), power));
            }
        }
        return result;
    }

    public static RealMatrix concatHorizontally(final RealMatrix left,
            final RealMatrix right) {
        if (left.getRowDimension() != right.getRowDimension()) {
            throw new IllegalArgumentException(
                    "The matrices must have the same row dimension!");
        }

        final double[][] result =
                new double[left.getRowDimension()][left.getColumnDimension()
                        + right.getColumnDimension()];

        final int lc = left.getColumnDimension();

        for (int r = 0; r < left.getRowDimension(); r++) {
            for (int c = 0; c < left.getColumnDimension(); c++) {
                result[r][c] = left.getEntry(r, c);
            }
            for (int c = 0; c < right.getColumnDimension(); c++) {
                result[r][lc + c] = right.getEntry(r, c);
            }
        }

        return MatrixUtils.createRealMatrix(result);
    }

    public static RealMatrix concatVertically(final RealMatrix top,
            final RealMatrix bottom) {
        if (top.getColumnDimension() != bottom.getColumnDimension()) {
            throw new IllegalArgumentException(
                    "The matrices must have the same column dimension!");
        }

        final double[][] result = new double[top.getRowDimension()
                + bottom.getRowDimension()][top.getColumnDimension()];

        final int tr = top.getRowDimension();

        for (int c = 0; c < top.getColumnDimension(); c++) {
            for (int r = 0; r < top.getRowDimension(); r++) {
                result[r][c] = top.getEntry(r, c);
            }
            for (int r = 0; r < bottom.getRowDimension(); r++) {
                result[tr + r][c] = bottom.getEntry(r, c);
            }
        }

        return MatrixUtils.createRealMatrix(result);
    }

    public static double[]
            calculateRowVectorDistances(final RealMatrix matrix) {
        final double[] distances = new double[matrix.getRowDimension()
                * (matrix.getRowDimension() - 1) / 2];
        int count = 1;
        int iterator = 0;
        for (int r1 = 0; r1 < matrix.getRowDimension(); r1++) {
            for (int r2 = count; r2 < matrix.getRowDimension(); r2++) {
                distances[iterator++] = matrix.getRowVector(r1)
                        .getDistance(matrix.getRowVector(r2));
            }
            count++;
        }

        return distances;
    }
}
