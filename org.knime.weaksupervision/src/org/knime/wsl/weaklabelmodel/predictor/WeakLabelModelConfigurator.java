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

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.NominalValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.probability.ProbabilityDistributionCellFactory;
import org.knime.core.data.probability.ProbabilityDistributionValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.wsl.weaklabelmodel.LabelMatrixReader;
import org.knime.wsl.weaklabelmodel.SourceParserFactory;
import org.knime.wsl.weaklabelmodel.WeakLabelModelPortObjectSpec;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class WeakLabelModelConfigurator {

    private final DataTableSpec m_sourceSpec;

    private final Set<String> m_possibleClasses;

    private final WeakLabelModelPredictorSettings m_settings;

    /**
     * @param settings predictor settings
     */
    WeakLabelModelConfigurator(final WeakLabelModelPortObjectSpec spec,
        final WeakLabelModelPredictorSettings settings) {
        m_sourceSpec = spec.getTrainingSpec();
        m_possibleClasses = spec.getPossibleClasses();
        m_settings = settings;
    }

    DataColumnSpec[] createPredictionSpecs(final DataTableSpec spec) throws InvalidSettingsException {
        checkSourceCompatibility(spec);
        final List<DataColumnSpec> colSpecs;
        final UniqueNameGenerator nameGen = new UniqueNameGenerator(spec);
        if (!m_settings.isAppendProbabilities()) {
            return new DataColumnSpec[]{createPredictionSpec(nameGen)};
        } else {
            colSpecs = createProbabilitySpecs(nameGen);
        }
        colSpecs.add(createPredictionSpec(nameGen));
        return colSpecs.toArray(new DataColumnSpec[0]);
    }

    private void checkSourceCompatibility(final DataTableSpec spec) throws InvalidSettingsException {
        for (final DataColumnSpec sourceSpec : m_sourceSpec) {
            final DataColumnSpec inSpec = spec.getColumnSpec(sourceSpec.getName());
            CheckUtils.checkSetting(inSpec != null, "The source column %s is missing.", sourceSpec);
            @SuppressWarnings("null") // explicitly checked above
            DataType type = inSpec.getType();
            if (type.isCompatible(ProbabilityDistributionValue.class)) {
                checkCompatibilityOfProbabilityDistribution(inSpec);
            } else if (type.isCompatible(NominalValue.class)) {
                checkCompatibilityOfNominalColumn(inSpec);
            } else {
                throw new InvalidSettingsException(
                    String.format("The column %s is neither a probability distribution nor nominal.", inSpec));
            }
        }
    }

    @SuppressWarnings("null") // we explicitly check that the possible values are not null
    private void checkCompatibilityOfNominalColumn(final DataColumnSpec inSpec) throws InvalidSettingsException {
        final Set<DataCell> possibleValues = inSpec.getDomain().getValues();
        CheckUtils.checkSetting(possibleValues != null && !possibleValues.isEmpty(),
            "The nominal column %s does not specify its possible values.", inSpec);
        checkClasses(inSpec, possibleValues.stream().map(DataCell::toString).iterator());
    }

    @SuppressWarnings("null") // elementNames is explicitly checked to not be null
    private void checkCompatibilityOfProbabilityDistribution(final DataColumnSpec inSpec)
        throws InvalidSettingsException {
        final List<String> possibleClasses = inSpec.getElementNames();
        CheckUtils.checkArgument(possibleClasses != null && !possibleClasses.isEmpty(),
            "A probability distribution column must always have non-empty element names.");
        checkClasses(inSpec, possibleClasses.iterator());
    }

    private void checkClasses(final DataColumnSpec inSpec, final Iterator<String> possibleClasses)
        throws InvalidSettingsException {
        for (String c = possibleClasses.next(); possibleClasses.hasNext(); c = possibleClasses.next()) {
            CheckUtils.checkSetting(m_possibleClasses.contains(c),
                "The class %s contained in column %s is unknown to the model.", c, inSpec);
        }
    }

    private List<DataColumnSpec> createProbabilitySpecs(final UniqueNameGenerator nameGenerator) {
        final String predictionColumnName = m_settings.getPredictionColumnName();
        return m_possibleClasses.stream().map(s -> String.format("P (%s=%s)", predictionColumnName, s))
            .map(s -> nameGenerator.newColumn(s, DoubleCell.TYPE)).collect(Collectors.toList());
    }

    private DataColumnSpec createPredictionSpec(final UniqueNameGenerator nameGenerator) {
        final DataColumnSpecCreator colSpecCreator =
            nameGenerator.newCreator(m_settings.getPredictionColumnName(), ProbabilityDistributionCellFactory.TYPE);
        colSpecCreator.setElementNames(m_possibleClasses.toArray(new String[0]));
        return colSpecCreator.createSpec();
    }

    int[] getFilterIndices(final DataTableSpec spec) throws InvalidSettingsException {
        checkSourceCompatibility(spec);
        return spec.columnsToIndices(m_sourceSpec.getColumnNames());
    }

    boolean isAppendProbabilities() {
        return m_settings.isAppendProbabilities();
    }

    LabelMatrixReader createLabelMatrixReader(final DataTableSpec inSpec) {
        final ColumnRearranger cr = new ColumnRearranger(inSpec);
        final String[] sourceNames = m_sourceSpec.getColumnNames();
        cr.keepOnly(sourceNames);
        cr.permute(sourceNames);
        return new LabelMatrixReader(SourceParserFactory.createParsers(cr.createSpec(), m_possibleClasses),
            m_possibleClasses.size());
    }

}
