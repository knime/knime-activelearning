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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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

package org.knime.al.nodes.score.novelty.localnoveltyscorer;

import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.math3.linear.RealMatrix;
import org.knime.al.util.noveltydetection.knfst.KNFST;
import org.knime.al.util.noveltydetection.knfst.KNFSTException;
import org.knime.al.util.noveltydetection.knfst.MultiClassKNFST;
import org.knime.al.util.noveltydetection.knfst.OneClassKNFST;

public class ExecutionThread implements Runnable {
    private final RealMatrix m_globalKernelMatrix;
    private final RealMatrix m_trainingKernelMatrix;
    private final int m_numNeighbors;
    private final boolean m_normalize;
    private final String[] m_labels;
    private final ThreadController m_controller;

    public ExecutionThread(final ThreadController controller,
            final RealMatrix globalKernelMatrix,
            final RealMatrix trainingKernelMatrix, final String[] labels,
            final int numNeighbors, final boolean normalize) {
        m_controller = controller;
        m_globalKernelMatrix = globalKernelMatrix;
        m_trainingKernelMatrix = trainingKernelMatrix;
        m_labels = labels;
        m_numNeighbors = numNeighbors;
        m_normalize = normalize;
    }

    @Override
    public void run() {

        int testIndex = m_controller.getNextIndex();

        while (testIndex != -1) {

            // Sort training samples according to distance to current sample in
            // kernel feature space

            final ValueIndexPair[] distances =
                    ValueIndexPair.transformArray2ValueIndexPairArray(
                            m_globalKernelMatrix.getColumn(testIndex));
            Arrays.sort(distances, new Comparator<ValueIndexPair>() {
                @Override
                public int compare(final ValueIndexPair o1,
                        final ValueIndexPair o2) {
                    final double v1 = o1.getValue();
                    final double v2 = o2.getValue();
                    int res = 0;
                    if (v1 < v2) {
                        res = 1;
                    }
                    if (v1 > v2) {
                        res = -1;
                    }
                    return res;
                }
            });

            // get nearest neighbors
            final ValueIndexPair[] neighbors =
                    new ValueIndexPair[m_numNeighbors];
            for (int i = 0; i < neighbors.length; i++) {
                neighbors[i] = distances[i];
            }

            // Sort neighbors according to class
            // NOTE: Since the instances are ordered by class in the original
            // table
            // sorting by indices is equivalent
            Arrays.sort(neighbors, new Comparator<ValueIndexPair>() {
                @Override
                public int compare(final ValueIndexPair o1,
                        final ValueIndexPair o2) {
                    int res = o1.getIndex() - o2.getIndex();
                    if (res < 0) {
                        res = -1;
                    }
                    if (res > 0) {
                        res = 1;
                    }
                    return res;
                }
            });

            // get local labels and check for one class setting
            boolean oneClass = true;
            final String[] localLabels = new String[m_numNeighbors];
            final int[] trainingMatrixIndices = new int[m_numNeighbors];
            final String currentLabel = m_labels[neighbors[0].getIndex()];
            for (int i = 0; i < localLabels.length; i++) {
                final String label = m_labels[neighbors[i].getIndex()];
                if (!currentLabel.equals(label)) {
                    oneClass = false;
                }
                localLabels[i] = label;
                trainingMatrixIndices[i] = neighbors[i].getIndex();
            }
            final RealMatrix localTrainingKernelMatrix = m_trainingKernelMatrix
                    .getSubMatrix(trainingMatrixIndices, trainingMatrixIndices);

            double score = 0;
            KNFST localModel = null;

            if (oneClass) {
                try {
                    localModel = new OneClassKNFST(localTrainingKernelMatrix);
                } catch (final KNFSTException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                try {
                    localModel = new MultiClassKNFST(localTrainingKernelMatrix,
                            localLabels);
                } catch (final KNFSTException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            score = localModel
                    .scoreTestData(m_globalKernelMatrix
                            .getColumnMatrix(testIndex).getSubMatrix(
                                    trainingMatrixIndices, new int[] { 0 }))
                    .getScores()[0];

            // normalize novelty score
            if (m_normalize) {
                final double normalizer =
                        Tools.getMin(localModel.getBetweenClassDistances());
                score = score / normalizer;
            }

            // save novelty score
            m_controller.saveNoveltyScore(testIndex, score);

            // get index of next test sample to process
            testIndex = m_controller.getNextIndex();
        }
    }

}
