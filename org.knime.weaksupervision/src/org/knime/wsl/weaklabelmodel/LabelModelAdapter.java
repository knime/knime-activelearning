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
package org.knime.wsl.weaklabelmodel;

import java.io.File;

import org.knime.core.node.util.CheckUtils;
import org.knime.wsl.WeakSupervisionPlugin;

/**
 * Utilizes a specialized TensorFlow SavedModel to learn a Label Model as proposed by the authors of Snorkel Metal.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany, KNIME GmbH, Konstanz, Germany
 */
public final class LabelModelAdapter implements AutoCloseable {

    static final String SAVED_MODEL_PATH = WeakSupervisionPlugin.getDefault().getPluginRootPath() + File.separator
            + "resources" + File.separator + "labelmodel" + File.separator + "labelModel";

    private final SavedModelAdapter m_savedModel;

    LabelModelAdapter(final String exportDir) {
        m_savedModel = new SavedModelAdapter(exportDir, "LabelModel");
    }

    /**
     * @return loads the default label model
     */
    public static LabelModelAdapter createDefault() {
        return new LabelModelAdapter(SAVED_MODEL_PATH);
    }

    @Override
    public void close() throws Exception {
        m_savedModel.close();
    }

    private void setLearningRate(final float learningRate) {
        m_savedModel.set("learning_rate", "lr_initialized", learningRate);
    }

    /**
     * @param classBalance the distribution of classes
     */
    public void setClassBalance(final float[] classBalance) {
        m_savedModel.set("p_init", "p_initialized", classBalance);
    }

    private void setMask(final boolean[][] mask) {
        m_savedModel.set("mask_init", "mask_initialized", mask);
    }

    /**
     * Performs a single training step and returns the loss for this step.
     *
     * @return the loss of the training step
     */
    public float trainStep() {
        return m_savedModel.getFloatScalar("train_loss");
    }

    /**
     * @return mu (i.e. the model parameters)
     */
    public float[][] getMu() {
        return m_savedModel.getFloatMatrix("mu");
    }

    /**
     * @param mu the model parameters
     */
    public void setMu(final float[][] mu) {
        m_savedModel.set("mu_init", "mu_initialized", mu);
    }

    /**
     * Initializes the model before training.
     *
     * @param covarianceMatrix the covariance matrix of the source columns (represented as probabilities)
     * @param mask junction tree mask for the handling of correlations
     * @param classBalance distribution of classes
     * @param initialPrecisions initial precision values for the label sources
     * @param learningRate learning rate (also called step size)
     */
    public void initialize(final float[][] covarianceMatrix, final boolean[][] mask, final float[] classBalance,
        final float[] initialPrecisions, final float learningRate) {
        setClassBalance(classBalance);
        setMask(mask);
        setLearningRate(learningRate);
        setO(covarianceMatrix);
        initializeMu(extractDiag(covarianceMatrix), initialPrecisions, classBalance);
    }

    private void setO(final float[][] o) {
        m_savedModel.set("o_init", "o_setter", o);
    }

    private static float[] extractDiag(final float[][] matrix) {
        final float[] diag = new float[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            CheckUtils.checkArgument(matrix[i].length == diag.length, "The provided matrix isn't square.");
            diag[i] = matrix[i][i];
        }
        return diag;
    }

    private void initializeMu(final float[] oDiag, final float[] initialPrecisions, final float[] classBalance) {
        final float[][] muInit = createMuInit(oDiag, initialPrecisions, classBalance);
        setMu(muInit);
    }

    private static float[][] createMuInit(final float[] oDiag, final float[] initialPrecisions,
        final float[] classBalances) {
        final int cardinality = classBalances.length;
        final int numSources = initialPrecisions.length;
        final float[][] muInit = new float[oDiag.length][cardinality];
        for (int i = 0; i < numSources; i++) {
            for (int y = 0; y < cardinality; y++) {
                final int idx = i * cardinality + y;
                final float v = LabelModelUtil.clip(oDiag[idx] * initialPrecisions[i] / classBalances[y], 0, 1);
                muInit[idx][y] += v;
            }
        }
        return muInit;
    }

    /**
     * Currently only used for testing purposes.
     *
     * @return the learning rate
     */
    float getLearningRate() {
        return m_savedModel.getFloatScalar("lr");
    }

    /**
     * Calculates the probability distributions over the true label given the
     * source labels encoded as probabilities.
     *
     * @param labelSourceMatrix matrix of label sources encoded as probabilities
     * @return a matrix of class probabilities according to the current model parameters
     */
    public float[][] calculateProbabilities(final float[][] labelSourceMatrix) {
        return m_savedModel.getFloatMatrix("l_aug", labelSourceMatrix, "probabilities");
    }

}
