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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.knime.al.util.noveltydetection.kernel.KernelCalculator;
import org.knime.core.data.DataRow;
import org.knime.core.node.ExecutionMonitor;

public class MultiClassKNFST extends KNFST {

    private String[] m_labels;

    public MultiClassKNFST() {

    }

    public MultiClassKNFST(final KernelCalculator kernel, final String[] labels,
            final ExecutionMonitor progMon) throws Exception {
        super(kernel);
        m_labels = labels;

        final ExecutionMonitor kernelProgMon = progMon.createSubProgress(0.3);
        final ExecutionMonitor nullspaceProgMon =
                progMon.createSubProgress(0.7);
        final RealMatrix kernelMatrix = kernel.kernelize(kernelProgMon);

        // obtain unique class labels
        final ClassWrapper[] classes = ClassWrapper.classes(labels);

        // calculate projection of KNFST
        nullspaceProgMon.setMessage("Calculating nullspace projection");
        m_projection = projection(kernelMatrix, labels);

        nullspaceProgMon.setProgress(1.0,
                "Finished calculating nullspace projection");

        // calculate target points ( = projections of training data into the
        // null space)
        m_targetPoints = MatrixUtils.createRealMatrix(classes.length,
                m_projection.getColumnDimension());
        int n = 0;
        int nOld = 0;
        for (int c = 0; c < classes.length; c++) {
            n += classes[c].getCount();
            m_targetPoints
                    .setRowVector(c,
                            MatrixFunctions
                                    .columnMeans(kernelMatrix
                                            .getSubMatrix(nOld, n - 1, 0,
                                                    kernelMatrix
                                                            .getColumnDimension()
                                                            - 1)
                                    .multiply(m_projection)));
            nOld = n;
        }

        // set betweenClassDistances
        m_betweenClassDistances =
                MatrixFunctions.calculateRowVectorDistances(m_targetPoints);

    }

    public MultiClassKNFST(final RealMatrix kernelMatrix, final String[] labels)
            throws KNFSTException {
        m_labels = labels;
        // obtain unique class labels
        final ClassWrapper[] classes = ClassWrapper.classes(labels);

        // calculate projection of KNFST
        m_projection = projection(kernelMatrix, labels);

        // calculate target points ( = projections of training data into the
        // null space)
        m_targetPoints = MatrixUtils.createRealMatrix(classes.length,
                m_projection.getColumnDimension());
        int n = 0;
        int nOld = 0;
        for (int c = 0; c < classes.length; c++) {
            n += classes[c].getCount();
            m_targetPoints
                    .setRowVector(c,
                            MatrixFunctions
                                    .columnMeans(kernelMatrix
                                            .getSubMatrix(nOld, n - 1, 0,
                                                    kernelMatrix
                                                            .getColumnDimension()
                                                            - 1)
                                    .multiply(m_projection)));
            nOld = n;
        }

        // set betweenClassDistances
        m_betweenClassDistances =
                MatrixFunctions.calculateRowVectorDistances(m_targetPoints);
    }

    // @Override
    // public NoveltyScores scoreTestData(BufferedDataTable test, ) {
    // // calculate nxm kernel matrix containing similarities between n training
    // samples and m test samples
    // RealMatrix kernelMatrix = m_kernel.kernelize(test);
    //
    // return score(kernelMatrix);
    // }

    // @Override
    // public NoveltyScores scoreTestData(double[][] test) {
    // // calculate nxm kernel matrix containing similarities between n training
    // samples and m test samples
    // RealMatrix kernelMatrix = m_kernel.kernelize(test);
    //
    // return score(kernelMatrix);
    // }

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
        final RealMatrix projectionVectors =
                kernelMatrix.transpose().multiply(m_projection);

        // squared euclidean distances to target points:
        final RealMatrix squared_distances =
                squared_euclidean_distances(projectionVectors, m_targetPoints);

        // novelty scores as minimum distance to one of the target points
        final RealVector scoreVector = MatrixFunctions
                .sqrt(MatrixFunctions.rowMins(squared_distances));
        return new NoveltyScores(scoreVector.toArray(), projectionVectors);
    }

    private RealMatrix squared_euclidean_distances(final RealMatrix x,
            final RealMatrix y) {
        final RealMatrix distmat = MatrixUtils
                .createRealMatrix(x.getRowDimension(), y.getRowDimension());

        for (int i = 0; i < x.getRowDimension(); i++) {
            for (int j = 0; j < y.getRowDimension(); j++) {
                final RealVector buff =
                        x.getRowVector(i).subtract(y.getRowVector(j));
                distmat.setEntry(i, j, buff.dotProduct(buff));
            }
        }

        return distmat;
    }

    @Override
    public void readExternal(final ObjectInput arg0)
            throws IOException, ClassNotFoundException {
        // call super method
        super.readExternal(arg0);

        // read labels
        m_labels = new String[arg0.readInt()];
        for (int l = 0; l < m_labels.length; l++) {
            m_labels[l] = arg0.readUTF();
        }

    }

    @Override
    public void writeExternal(final ObjectOutput arg0) throws IOException {
        // call super method
        super.writeExternal(arg0);

        // write labels
        arg0.writeInt(m_labels.length);
        for (final String label : m_labels) {
            arg0.writeUTF(label);
        }

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(m_labels);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof MultiClassKNFST)) {
            return false;
        }
        final MultiClassKNFST other = (MultiClassKNFST) obj;
        if (!Arrays.equals(m_labels, other.m_labels)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        final int maxLen = 10;
        return "MultiClassKNFST [m_labels="
                + (m_labels != null ? Arrays.asList(m_labels).subList(0,
                        Math.min(m_labels.length, maxLen)) : null)
                + ", m_kernel=" + m_kernel + ", m_projection=" + m_projection
                + ", m_targetPoints=" + m_targetPoints
                + ", m_betweenClassDistances="
                + (m_betweenClassDistances != null ? Arrays
                        .toString(Arrays.copyOf(m_betweenClassDistances, Math
                                .min(m_betweenClassDistances.length, maxLen)))
                        : null)
                + "]";
    }
}
