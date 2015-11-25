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

package org.knime.al.nodes.score.novelty.localnoveltyscorer;

import org.apache.commons.math3.linear.RealMatrix;
import org.knime.core.node.ExecutionContext;

public class ThreadController {
    private final ExecutionContext m_exec;
    private final RealMatrix m_globalKernelMatrix;
    private final RealMatrix m_trainingKernelMatrix;
    private final String[] m_labels;
    private final int m_numNeighbors;
    private final boolean m_normalize;

    private final double[] m_noveltyScores;
    private int m_currentIndex;

    public ThreadController(final ExecutionContext exec,
            final RealMatrix globalKernelMatrix,
            final RealMatrix trainingKernelMatrix, final String[] labels,
            final int numNeighbors, final boolean normalize) {
        m_exec = exec;
        m_globalKernelMatrix = globalKernelMatrix;
        m_trainingKernelMatrix = trainingKernelMatrix;
        m_labels = labels;
        m_numNeighbors = numNeighbors;
        m_normalize = normalize;

        m_noveltyScores = new double[globalKernelMatrix.getColumnDimension()];
    }

    public double[] process() {
        // get number of available cores
        final int numCores = Runtime.getRuntime().availableProcessors();

        final Thread[] threads = new Thread[numCores];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new ExecutionThread(this,
                    m_globalKernelMatrix, m_trainingKernelMatrix, m_labels,
                    m_numNeighbors, m_normalize));
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) {
            try {
                threads[i].join();
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return m_noveltyScores;
    }

    public synchronized void saveNoveltyScore(final int index,
            final double score) {
        m_noveltyScores[index] = score;
        m_exec.setProgress((double) index / m_noveltyScores.length);
    }

    public synchronized int getNextIndex() {
        if (m_currentIndex < m_globalKernelMatrix.getColumnDimension()) {
            return m_currentIndex++;
        } else {
            return -1;
        }
    }
}
