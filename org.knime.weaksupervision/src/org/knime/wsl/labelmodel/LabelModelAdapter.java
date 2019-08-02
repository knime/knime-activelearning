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

/**
 * Utilizes a specialized TensorFlow SavedModel to learn a Label Model as proposed by the author of Snorkel Metal.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany, KNIME GmbH, Konstanz, Germany
 */
final class LabelModelAdapter implements AutoCloseable {

	private final SavedModelAdapter m_savedModel;

	LabelModelAdapter(final String exportDir) {
		m_savedModel = new SavedModelAdapter(exportDir, "LabelModel");
	}

	@Override
	public void close() throws Exception {
		m_savedModel.close();
	}

	private void setLearningRate(final float learningRate) {
		m_savedModel.set("learning_rate", "lr_initialized", learningRate);
	}

	private void setClassBalance(final float[] classBalance) {
		m_savedModel.set("p_init", "p_initialized", classBalance);
	}

	private void setMask(final boolean[][] mask) {
		m_savedModel.set("mask_init", "mask_initialized", mask);
	}

	float trainStep() {
		return m_savedModel.getFloatScalar("train_loss");
	}

	float[][] getMu() {
		return m_savedModel.getFloatMatrix("mu");
	}

	void initialize(final float[][] augmentedLabelMatrix, final boolean[][] mask, final float[] classBalance,
			final float[] initialPrecisions, final float learningRate) {
		setClassBalance(classBalance);
		setMask(mask);
		setLearningRate(learningRate);
		initializeO(augmentedLabelMatrix);
		initializeMu(initialPrecisions, classBalance);
	}

	private void initializeMu(final float[] initialPrecisions, final float[] classBalance) {
		final float[][] muInit = createMuInit(initialPrecisions, classBalance);
		m_savedModel.set("mu_init", "mu_initialized", muInit);
	}

	private float[][] createMuInit(final float[] initialPrecisions, final float[] classBalance) {
		final int cardinality = classBalance.length;
		final int numSources = initialPrecisions.length;
		final float[] oDiag = m_savedModel.getFloatVector("o_diag");
		final float[][] muInit = new float[oDiag.length][cardinality];
		for (int i = 0; i < numSources; i++) {
			for (int y = 0; y < cardinality; y++) {
				final int idx = i * cardinality + y;
				final float v = LabelModelUtil.clip(oDiag[idx] * initialPrecisions[i] / classBalance[y], 0, 1);
				muInit[idx][y] += v;
			}
		}
		return muInit;
	}


	/**
	 * Only meant for testing purposes.
	 * @return the underlying SavedModelAdapter
	 */
	SavedModelAdapter getSavedModelAdapter() {
	    return m_savedModel;
	}

	float[][] getProbabilities(final float[][] augmentedLabelMatrix) {
		return m_savedModel.getFloatMatrix("l_aug", augmentedLabelMatrix, "probabilities");
	}

	private void initializeO(final float[][] augmentedLabelMatrix) {
		m_savedModel.set("l_aug", "o_initialized", augmentedLabelMatrix);
	}

}
