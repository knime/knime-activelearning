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
 * History
 *   Sep 6, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.wsl.weaklabelmodel;

import java.util.stream.IntStream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;

/**
 * Stores the parameters of a weak label model.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class WeakLabelModelContent {

    private static final String CFG_CLASS_BALANCE = "classBalance";

    private static final String CFG_NR_ROWS = "nrRows";

    private float[][] m_mu;

    private float[] m_classBalance;

    /**
     * @param mu the model parameters
     * @param classBalance the class distribution of the training data
     */
    public WeakLabelModelContent(final float[][] mu, final float[] classBalance) {
        m_mu = cloneFloatMatrix(mu);
        m_classBalance = classBalance.clone();
    }

    private static float[][] cloneFloatMatrix(final float[][] matrix) {
        return IntStream.range(0, matrix.length).mapToObj(i -> matrix[i].clone()).toArray(float[][]::new);
    }

    void save(final ModelContentWO modelContent) {
        modelContent.addFloatArray(CFG_CLASS_BALANCE, m_classBalance);
        saveMu(modelContent);
    }

    private void saveMu(final ModelContentWO modelContent) {
        modelContent.addInt(CFG_NR_ROWS, m_mu.length);
        for (int i = 0; i < m_mu.length; i++) {
            final float[] row = m_mu[i];
            modelContent.addFloatArray(Integer.toString(i), row);
        }
    }

    static WeakLabelModelContent load(final ModelContentRO modelContent) throws InvalidSettingsException {
        final float[] classBalance = modelContent.getFloatArray(CFG_CLASS_BALANCE);
        final int nrRows = modelContent.getInt(CFG_NR_ROWS);
        final float[][] mu = new float[nrRows][];
        for (int i = 0; i < nrRows; i++) {
            mu[i] = modelContent.getFloatArray(Integer.toString(i));
        }
        return new WeakLabelModelContent(mu, classBalance);
    }

    /**
     * @return mu (i.e. the model parameters)
     */
    public float[][] getMu() {
        return cloneFloatMatrix(m_mu);
    }

    /**
     * @return the class distribution of the training data
     */
    public float[] getClassBalance() {
        return m_classBalance.clone();
    }

}
