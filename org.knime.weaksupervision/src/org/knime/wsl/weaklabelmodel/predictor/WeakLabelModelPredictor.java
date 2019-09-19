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
 *   Sep 13, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.wsl.weaklabelmodel.predictor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.knime.base.data.filter.column.FilterColumnRow;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.probability.ProbabilityDistributionCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.wsl.weaklabelmodel.LabelMatrixReader;
import org.knime.wsl.weaklabelmodel.LabelModelAdapter;
import org.knime.wsl.weaklabelmodel.WeakLabelModelContent;
import org.knime.wsl.weaklabelmodel.WeakLabelModelPortObject;
import org.knime.wsl.weaklabelmodel.WeakLabelModelPortObjectSpec;

/**
 * Holds the TensorFlow model, which is used by all the {@link CellFactory CellFactories} created by
 * this class.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class WeakLabelModelPredictor implements AutoCloseable {

    private final LabelModelAdapter m_labelModel;

    private final WeakLabelModelConfigurator m_configurator;

    WeakLabelModelPredictor(final WeakLabelModelPortObjectSpec spec, final WeakLabelModelPortObject model,
        final WeakLabelModelPredictorSettings settings) {
        m_configurator = new WeakLabelModelConfigurator(spec, settings);
        if (model != null) {
            final WeakLabelModelContent content = model.getContent();
            m_labelModel = LabelModelAdapter.createDefault();
            m_labelModel.setMu(content.getMu());
            m_labelModel.setClassBalance(content.getClassBalance());
        } else {
            m_labelModel = null;
        }
    }

    CellFactory createCellFactory(final DataTableSpec spec) throws InvalidSettingsException {
        final int[] sourceIndices = m_configurator.getFilterIndices(spec);
        final LabelMatrixReader matrixReader = m_configurator.createLabelMatrixReader(spec);
        return new AbstractCellFactory(true, m_configurator.createPredictionSpecs(spec)) {

            @Override
            public DataCell[] getCells(final DataRow row) {
                assert m_labelModel != null : "The label model has not been initialized.";
                final DataRow filtered = new FilterColumnRow(row, sourceIndices);
                final float[] input = matrixReader.readAndAugmentRow(filtered);
                final float[][] probabilities = m_labelModel.calculateProbabilities(new float[][]{input});
                return createCells(probabilities[0]);
            }
        };
    }

    private DataCell[] createCells(final float[] probabilities) {
        final double[] probs = toDouble(probabilities);
        if (!m_configurator.isAppendProbabilities()) {
            return new DataCell[]{ProbabilityDistributionCellFactory.createCell(probs)};
        }
        final List<DataCell> cells = Arrays.stream(probs).mapToObj(DoubleCell::new).collect(Collectors.toList());
        cells.add(ProbabilityDistributionCellFactory.createCell(probs));
        return cells.toArray(new DataCell[0]);
    }

    private static double[] toDouble(final float[] array) {
        final double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception {
        if (m_labelModel != null) {
            m_labelModel.close();
        }
    }

}
