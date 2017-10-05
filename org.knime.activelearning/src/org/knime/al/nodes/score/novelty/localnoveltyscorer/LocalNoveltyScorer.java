/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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

package org.knime.al.nodes.score.novelty.localnoveltyscorer;

import java.util.TimerTask;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import org.apache.commons.math3.linear.RealMatrix;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.util.KNIMETimer;
import org.knime.core.util.ThreadPool;

public class LocalNoveltyScorer {

    private final ExecutionMonitor m_exec;
    private final RealMatrix m_globalKernelMatrix;
    private final RealMatrix m_trainingKernelMatrix;
    private final String[] m_labels;
    private final int m_numNeighbors;
    private final boolean m_normalize;

    public LocalNoveltyScorer(final ExecutionMonitor executionMonitor,
            final RealMatrix m_globalKernelMatrix,
            final RealMatrix m_trainingKernelMatrix, final String[] m_labels,
            final int m_numNeighbors, final boolean m_normalize) {
        super();
        m_exec = executionMonitor;
        this.m_globalKernelMatrix = m_globalKernelMatrix;
        this.m_trainingKernelMatrix = m_trainingKernelMatrix;
        this.m_labels = m_labels;
        this.m_numNeighbors = m_numNeighbors;
        this.m_normalize = m_normalize;
    }

    public double[] calculateNoveltyScores() throws Exception {

        final ThreadPool pool = KNIMEConstants.GLOBAL_THREAD_POOL;
        final int procCount =
                (int) (Runtime.getRuntime().availableProcessors() * (2.0 / 3));
        final Semaphore semaphore = new Semaphore(procCount);

        final int numTestSamples = m_globalKernelMatrix.getColumnDimension();
        final NoveltyScoreCalculationCallable[] nct =
                new NoveltyScoreCalculationCallable[numTestSamples];
        for (int i = 0; i < numTestSamples; i++) {
            nct[i] = new NoveltyScoreCalculationCallable(i, semaphore,
                    m_numNeighbors, m_trainingKernelMatrix,
                    m_globalKernelMatrix, m_labels, m_normalize);
        }
        final Future<?>[] scores = new Future<?>[numTestSamples];
        final KNIMETimer timer = KNIMETimer.getInstance();
        final TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    m_exec.checkCanceled();
                } catch (final CanceledExecutionException ce) {
                    for (int i = 0; i < scores.length; i++) {
                        if (scores[i] != null) {
                            scores[i].cancel(true);
                        }
                    }
                    super.cancel();
                }

            }
        };
        timer.scheduleAtFixedRate(timerTask, 0, 3000);
        double progCounter = 0;
        for (int i = 0; i < numTestSamples; i++) {
            try {
                m_exec.checkCanceled();
            } catch (final Exception e) {
                for (int j = 0; j < i; j++) {
                    if (scores[j] != null) {
                        scores[j].cancel(true);
                    }
                }
                timerTask.cancel();
                throw e;
            }
            semaphore.acquire();
            scores[i] = pool.enqueue(nct[i]);
            m_exec.setProgress(progCounter / (2 * numTestSamples),
                    "Local novelty score calculation started (" + i + "/"
                            + numTestSamples + ")");
            progCounter += 1;
        }
        final double[] result = new double[numTestSamples];

        for (int i = 0; i < numTestSamples; i++) {
            semaphore.acquire();
            try {
                m_exec.checkCanceled();
                result[i] = (Double) scores[i].get();
                nct[i].ok();
            } catch (final Exception e) {
                for (int j = 0; j < scores.length; j++) {
                    scores[j].cancel(true);
                }
                timerTask.cancel();
                throw e;

            }
            m_exec.setProgress(progCounter / (2 * numTestSamples),
                    "Local novelty score calculated (" + i + "/"
                            + numTestSamples + ")");
            progCounter += 1;
            semaphore.release();
        }

        timerTask.cancel();

        return result;
    }

}
