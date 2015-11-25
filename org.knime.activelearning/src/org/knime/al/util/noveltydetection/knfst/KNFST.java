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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.knime.al.util.noveltydetection.kernel.KernelCalculator;
import org.knime.core.data.DataRow;

public abstract class KNFST implements Externalizable {
    protected KernelCalculator m_kernel;
    protected RealMatrix m_projection;
    protected RealMatrix m_targetPoints;
    protected double[] m_betweenClassDistances;

    public KNFST() {

    }

    public KNFST(final KernelCalculator kernel) {
        m_kernel = kernel;
    }

    // public abstract NoveltyScores scoreTestData(BufferedDataTable test);

    public abstract NoveltyScores scoreTestData(DataRow testInstance);

    // public abstract NoveltyScores scoreTestData(double[][] test);

    public abstract NoveltyScores scoreTestData(RealMatrix kernelMatrix);

    public static RealMatrix projection(final RealMatrix kernelMatrix,
            final String[] labels) throws KNFSTException {

        final ClassWrapper[] classes = ClassWrapper.classes(labels);

        // check labels
        if (classes.length == 1) {
            throw new IllegalArgumentException(
                    "not able to calculate a nullspace from data of a single class using KNFST (input variable \"labels\" only contains a single value)");
        }

        // check kernel matrix
        if (!kernelMatrix.isSquare()) {
            throw new IllegalArgumentException(
                    "The KernelMatrix must be quadratic!");
        }

        // calculate weights of orthonormal basis in kernel space
        final RealMatrix centeredK = centerKernelMatrix(kernelMatrix);

        final EigenDecomposition eig = new EigenDecomposition(centeredK);
        final double[] eigVals = eig.getRealEigenvalues();
        final ArrayList<Integer> nonZeroEigValIndices =
                new ArrayList<Integer>();
        for (int i = 0; i < eigVals.length; i++) {
            if (eigVals[i] > 1e-12) {
                nonZeroEigValIndices.add(i);
            }
        }

        int eigIterator = 0;
        final RealMatrix eigVecs = eig.getV();
        RealMatrix basisvecs = null;
        try {
            basisvecs = MatrixUtils.createRealMatrix(eigVecs.getRowDimension(),
                    nonZeroEigValIndices.size());
        } catch (final Exception e) {
            throw new KNFSTException(
                    "Something went wrong. Try different parameters or a different kernel.");
        }

        for (final Integer index : nonZeroEigValIndices) {
            final double normalizer = 1 / Math.sqrt(eigVals[index]);
            final RealVector basisVec = eigVecs.getColumnVector(eigIterator)
                    .mapMultiply(normalizer);
            basisvecs.setColumnVector(eigIterator++, basisVec);
        }

        // calculate transformation T of within class scatter Sw:
        // T= B'*K*(I-L) and L a block matrix
        final RealMatrix L =
                kernelMatrix.createMatrix(kernelMatrix.getRowDimension(),
                        kernelMatrix.getColumnDimension());
        int start = 0;
        for (final ClassWrapper cl : classes) {
            final int count = cl.getCount();
            L.setSubMatrix(
                    MatrixFunctions.ones(count, count)
                            .scalarMultiply(1.0 / count).getData(),
                    start, start);
            start += count;
        }

        // need Matrix M with all entries 1/m to modify basisvecs which allows
        // usage of
        // uncentered kernel values (eye(size(M)).M)*basisvecs
        final RealMatrix M = MatrixFunctions
                .ones(kernelMatrix.getColumnDimension(),
                        kernelMatrix.getColumnDimension())
                .scalarMultiply(1.0 / kernelMatrix.getColumnDimension());
        final RealMatrix I =
                MatrixUtils.createRealIdentityMatrix(M.getColumnDimension());

        // compute helper matrix H
        final RealMatrix H = ((I.subtract(M)).multiply(basisvecs)).transpose()
                .multiply(kernelMatrix).multiply(I.subtract(L));

        // T = H*H' = B'*Sw*B with B=basisvecs
        final RealMatrix T = H.multiply(H.transpose());

        // calculate weights for null space
        RealMatrix eigenvecs = MatrixFunctions.nullspace(T);

        if (eigenvecs == null) {
            final EigenDecomposition eigenComp = new EigenDecomposition(T);
            final double[] eigenvals = eigenComp.getRealEigenvalues();
            eigenvecs = eigenComp.getV();
            final int minId =
                    MatrixFunctions.argmin(MatrixFunctions.abs(eigenvals));
            final double[] eigenvecsData = eigenvecs.getColumn(minId);
            eigenvecs = MatrixUtils.createColumnRealMatrix(eigenvecsData);
        }

        // System.out.println("eigenvecs:");
        // test.printMatrix(eigenvecs);

        // calculate null space projection
        final RealMatrix proj =
                ((I.subtract(M)).multiply(basisvecs)).multiply(eigenvecs);

        return proj;
    }

    private static RealMatrix
            centerKernelMatrix(final RealMatrix kernelMatrix) {
        // get size of kernelMatrix
        final int n = kernelMatrix.getRowDimension();

        // get mean values for each row/column
        final RealVector columnMeans =
                MatrixFunctions.columnMeans(kernelMatrix);
        final double matrixMean = MatrixFunctions.mean(kernelMatrix);

        RealMatrix centeredKernelMatrix = kernelMatrix.copy();

        for (int k = 0; k < n; k++) {
            centeredKernelMatrix.setRowVector(k,
                    centeredKernelMatrix.getRowVector(k).subtract(columnMeans));
            centeredKernelMatrix.setColumnVector(k, centeredKernelMatrix
                    .getColumnVector(k).subtract(columnMeans));
        }

        centeredKernelMatrix = centeredKernelMatrix.scalarAdd(matrixMean);

        return centeredKernelMatrix;
    }

    // Methods of Externalizable interface

    @Override
    public void readExternal(final ObjectInput arg0)
            throws IOException, ClassNotFoundException {

        try {
            // read kernel
            m_kernel = (KernelCalculator) Class.forName(arg0.readUTF())
                    .newInstance();
            m_kernel.readExternal(arg0);

            // read projection
            // rows
            final int rowsProj = arg0.readInt();
            // columns
            final int colsProj = arg0.readInt();
            // data
            final double[][] projData = new double[rowsProj][colsProj];
            for (int r = 0; r < rowsProj; r++) {
                for (int c = 0; c < colsProj; c++) {
                    projData[r][c] = arg0.readDouble();
                }
            }
            // Matrix construction
            m_projection = MatrixUtils.createRealMatrix(projData);

            // read targetPoints
            // rows
            final int rowsTar = arg0.readInt();
            // columns
            final int colsTar = arg0.readInt();
            // data
            final double[][] tarData = new double[rowsTar][colsTar];
            for (int r = 0; r < rowsTar; r++) {
                for (int c = 0; c < colsTar; c++) {
                    tarData[r][c] = arg0.readDouble();
                }
            }
            // Matrix construction
            m_targetPoints = MatrixUtils.createRealMatrix(tarData);

            // read betweenClassDistances
            final double[] betweenClassDistances = new double[arg0.readInt()];
            for (int i = 0; i < betweenClassDistances.length; i++) {
                betweenClassDistances[i] = arg0.readDouble();
            }
            m_betweenClassDistances = betweenClassDistances;

        } catch (InstantiationException | IllegalAccessException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void writeExternal(final ObjectOutput arg0) throws IOException {
        // write kernel
        arg0.writeUTF(m_kernel.getClass().getName());
        m_kernel.writeExternal(arg0);

        // write projection
        // rows
        arg0.writeInt(m_projection.getRowDimension());
        // columns
        arg0.writeInt(m_projection.getColumnDimension());
        // data
        final double[][] projData = m_projection.getData();
        for (final double[] row : projData) {
            for (final double cell : row) {
                arg0.writeDouble(cell);
            }
        }

        // write targetPoints
        // rows
        arg0.writeInt(m_targetPoints.getRowDimension());
        // columns
        arg0.writeInt(m_targetPoints.getColumnDimension());
        // data
        final double[][] tarData = m_targetPoints.getData();
        for (final double[] row : tarData) {
            for (final double cell : row) {
                arg0.writeDouble(cell);
            }
        }

        // write betweenClassDistances
        // length
        arg0.writeInt(m_betweenClassDistances.length);
        // data
        for (final double dist : m_betweenClassDistances) {
            arg0.writeDouble(dist);
        }
    }

    public double[] getBetweenClassDistances() {
        return m_betweenClassDistances;
    }

    public KernelCalculator getKernel() {
        return m_kernel;
    }

    public RealMatrix getProjection() {
        return m_projection;
    }

    public double[][] getTargetPoints() {
        return m_targetPoints.getData();
    }

    public int getNullspaceDimension() {
        return m_targetPoints.getColumnDimension();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(m_betweenClassDistances);
        result = prime * result
                + ((m_kernel == null) ? 0 : m_kernel.hashCode());
        result = prime * result
                + ((m_projection == null) ? 0 : m_projection.hashCode());
        result = prime * result
                + ((m_targetPoints == null) ? 0 : m_targetPoints.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof KNFST)) {
            return false;
        }
        final KNFST other = (KNFST) obj;
        if (!Arrays.equals(m_betweenClassDistances,
                other.m_betweenClassDistances)) {
            return false;
        }
        if (m_kernel == null) {
            if (other.m_kernel != null) {
                return false;
            }
        } else if (!m_kernel.equals(other.m_kernel)) {
            return false;
        }
        if (m_projection == null) {
            if (other.m_projection != null) {
                return false;
            }
        } else if (!m_projection.equals(other.m_projection)) {
            return false;
        }
        if (m_targetPoints == null) {
            if (other.m_targetPoints != null) {
                return false;
            }
        } else if (!m_targetPoints.equals(other.m_targetPoints)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "KNFST [m_kernel=" + m_kernel + ", m_projection=" + m_projection
                + ", m_targetPoints=" + m_targetPoints
                + ", m_betweenClassDistances="
                + Arrays.toString(m_betweenClassDistances) + "]";
    }
}
