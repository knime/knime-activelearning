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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.wsl.labelmodel;

import java.util.Arrays;

import org.knime.core.node.util.CheckUtils;

/**
 * Helper class that wraps the conditional probability matrix returned by the matrix completion algorithm and allows to
 * easily retrieve the conditional probabilities.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany, KNIME GmbH, Konstanz, Germany
 */
final class ConditionalProbabilities {

    private final float[][] m_cProbs;

    private final int m_cardinality;

    /**
     * Creates a {@link ConditionalProbabilities} object by extracting the conditional probabilities from <b>mu</b>.
     *
     * @param mu the parameter matrix
     */
    ConditionalProbabilities(final float[][] mu) {
        m_cardinality = mu[0].length;
        CheckUtils.checkArgument(m_cardinality > 1, "mu's shape should be [numSources * numClasses, numClasses].");
        CheckUtils.checkArgument(mu.length > m_cardinality && mu.length % m_cardinality == 0,
            "mu's shape should be [numSources * numClasses, numClasses].");
        final int nlf = mu.length / m_cardinality;
        m_cProbs = new float[nlf * (m_cardinality + 1)][m_cardinality];

        for (int i = 0; i < nlf; i++) {
            final int muiStart = i * m_cardinality;
            final int muiEnd = (i + 1) * m_cardinality;
            final int cProbsStart = i * (m_cardinality + 1) + 1;
            for (int j = 0; j < m_cardinality; j++) {
                System.arraycopy(mu[muiStart + j], 0, m_cProbs[cProbsStart + j], 0, m_cardinality);
            }
            // the 0th row corresponds to the abstains and is by the law of total probability 1 minus the sum of the
            // other rows
            Arrays.fill(m_cProbs[cProbsStart - 1], 1);
            for (int j = muiStart; j < muiEnd; j++) {
                for (int k = 0; k < m_cardinality; k++) {
                    m_cProbs[cProbsStart - 1][k] -= mu[j][k];
                }
            }
        }
    }

    /**
     * Retrieves the probability P(lf=lfValue|label=labelValue). Note that <b>labelValue</b> is indexed starting from 1
     * (because 0 is reserved for the abstain class which the true label can't have).
     *
     * @param lf the labeling function for which to retrieve the conditional probabilities
     * @param lfValue the value of <b>lf</b> for which to receive the conditional probability
     * @param labelValue the value of the latent label, IMPORTANT: Indexing starts at 1
     */
    double getConditionalProbability(final int lf, final int lfValue, final int labelValue) {
        final int rowIdx = lf * (m_cardinality + 1) + lfValue;
        final int colIdx = labelValue - 1;
        return LabelModelUtil.clip(m_cProbs[rowIdx][colIdx], 0.01, 0.99);
    }

}
