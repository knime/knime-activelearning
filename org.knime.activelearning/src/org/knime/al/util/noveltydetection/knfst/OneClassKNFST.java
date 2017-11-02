/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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

import java.util.Arrays;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.knime.al.util.noveltydetection.kernel.KernelCalculator;
import org.knime.core.data.DataRow;
import org.knime.core.node.ExecutionMonitor;

public class OneClassKNFST extends KNFST {

    public OneClassKNFST() {

    }

    public OneClassKNFST(final KernelCalculator kernel,
            final ExecutionMonitor progMon) throws Exception {
        super(kernel);

        final ExecutionMonitor kernelProgMon = progMon.createSubProgress(0.3);
        final ExecutionMonitor nullspaceProgMon =
                progMon.createSubProgress(0.7);

        // get number of training samples
        final RealMatrix kernelMatrix = m_kernel.kernelize(kernelProgMon);
        final int n = kernelMatrix.getRowDimension();

        // include dot products of training samples and the origin in feature
        // space (these dot products are always zero!)
        final RealMatrix k = MatrixFunctions.concatVertically(
                MatrixFunctions.concatHorizontally(kernelMatrix,
                        MatrixUtils.createRealMatrix(
                                kernelMatrix.getRowDimension(), 1)),
                MatrixUtils.createRealMatrix(1,
                        kernelMatrix.getColumnDimension() + 1));

        // create one-class labels + a different label for the origin
        final String[] labels = new String[n + 1];
        for (int l = 0; l <= n; l++) {
            labels[l] = (l == n) ? "0" : "1";
        }

        // get model parameters
        nullspaceProgMon.setMessage("Calculating nullspace projection");
        final RealMatrix projection = projection(k, labels);
        nullspaceProgMon.setProgress(1.0,
                "Finished calculating nullspace projection");
        final int[] indices = new int[n];
        for (int i = 0; i < n; i++) {
            indices[i] = i;
        }
        m_targetPoints =
                MatrixUtils
                        .createRowRealMatrix(
                                MatrixFunctions
                                        .columnMeans(k
                                                .getSubMatrix(0, n - 1, 0,
                                                        k.getColumnDimension()
                                                                - 1)
                                        .multiply(projection)).toArray());
        m_projection = projection.getSubMatrix(0, n - 1, 0,
                projection.getColumnDimension() - 1);
        m_betweenClassDistances =
                new double[] { Math.abs(m_targetPoints.getEntry(0, 0)) };
    }

    public OneClassKNFST(final RealMatrix kernelMatrix) throws KNFSTException {
        final int n = kernelMatrix.getRowDimension();

        // include dot products of training samples and the origin in feature
        // space (these dot products are always zero!)
        final RealMatrix k = MatrixFunctions.concatVertically(
                MatrixFunctions.concatHorizontally(kernelMatrix,
                        MatrixUtils.createRealMatrix(
                                kernelMatrix.getRowDimension(), 1)),
                MatrixUtils.createRealMatrix(1,
                        kernelMatrix.getColumnDimension() + 1));

        // create one-class labels + a different label for the origin
        final String[] labels = new String[n + 1];
        for (int l = 0; l <= n; l++) {
            labels[l] = (l == n) ? "0" : "1";
        }

        // get model parameters
        final RealMatrix projection = projection(k, labels);
        final int[] indices = new int[n];
        for (int i = 0; i < n; i++) {
            indices[i] = i;
        }
        m_targetPoints =
                MatrixUtils
                        .createRowRealMatrix(
                                MatrixFunctions
                                        .columnMeans(k
                                                .getSubMatrix(0, n - 1, 0,
                                                        k.getColumnDimension()
                                                                - 1)
                                        .multiply(projection)).toArray());
        m_projection = projection.getSubMatrix(0, n - 1, 0,
                projection.getColumnDimension() - 1);
        m_betweenClassDistances =
                new double[] { Math.abs(m_targetPoints.getEntry(0, 0)) };
    }

    @Override
    public NoveltyScores scoreTestData(final RealMatrix kernelMatrix) {
        return score(kernelMatrix);
    }

    @Override
    public NoveltyScores scoreTestData(final DataRow testInstance) {
        final RealMatrix kernelMatrix = m_kernel.kernelize(testInstance);
        return score(kernelMatrix);
    }

    private NoveltyScores score(final RealMatrix kernelMatrix) {
        // projected test samples:
        final RealMatrix projectionVectors =
                kernelMatrix.transpose().multiply(m_projection);

        // differences to the target value:
        final RealMatrix diff = projectionVectors.subtract(
                MatrixFunctions.ones(kernelMatrix.getColumnDimension(), 1)
                        .scalarMultiply(m_targetPoints.getEntry(0, 0)));

        // distances to the target value:
        final RealVector scoresVector = MatrixFunctions.sqrt(MatrixFunctions
                .rowSums(MatrixFunctions.multiplyElementWise(diff, diff)));

        return new NoveltyScores(scoresVector.toArray(), projectionVectors);
    }

    @Override
    public String toString() {
        final int maxLen = 10;
        return "OneClassKNFST [m_kernel=" + m_kernel + ", m_projection="
                + m_projection + ", m_targetPoints=" + m_targetPoints
                + ", m_betweenClassDistances="
                + (m_betweenClassDistances != null ? Arrays
                        .toString(Arrays.copyOf(m_betweenClassDistances, Math
                                .min(m_betweenClassDistances.length, maxLen)))
                        : null)
                + "]";
    }
}
