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
 *
 */
package org.knime.al.nodes.score.combine.pbac;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.knime.al.nodes.score.ExceptionHandling;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.UniqueNameGenerator;

/**
 * Node model for the Exploration/Exploitation Score Combiner node.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
final class UncertaintyDistributionScorer2NodeModel extends SimpleStreamableFunctionNodeModel {

    static final String SAME_COLUMN_ERROR = "The exploration and exploitation score columns must not be the same.";

    private final SettingsModelString m_explorationColumnModel = createExplorationColumnModel();

    private final SettingsModelString m_exploitationColumnModel = createExploitationColumnModel();

    private final SettingsModelString m_outputColumnNameModel = createOutputColumnNameModel();

    private final SettingsModelDouble m_exploitationFactorModel = createExploitationFactorModel();

    private final SettingsModelString m_missingValueHandlingModel = createMissingValueHandlingModel();

    /**
     * {@inheritDoc}
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec inSpec) throws InvalidSettingsException {
        if (isFirstConfigure()) {
            autoConfigure(inSpec);
        }
        // check configurations
        checkConfiguration(inSpec);

        // create rearranger
        final ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        final DataColumnSpec newColSpec =
            new UniqueNameGenerator(inSpec).newColumn(m_outputColumnNameModel.getStringValue(), DoubleCell.TYPE);
        rearranger.append(new SingleCellFactory(newColSpec) {

            final boolean m_fail = m_missingValueHandlingModel.getStringValue().equals(ExceptionHandling.FAIL.name());

            boolean m_hasMissing = false;

            final int m_explorationIndex = inSpec.findColumnIndex(m_explorationColumnModel.getStringValue());

            final int m_exploitationIndex = inSpec.findColumnIndex(m_exploitationColumnModel.getStringValue());

            final double m_exploitationFactor = m_exploitationFactorModel.getDoubleValue();

            @Override
            public DataCell getCell(final DataRow row) {
                final DataCell explorationCell = row.getCell(m_explorationIndex);
                final DataCell exploitationCell = row.getCell(m_exploitationIndex);
                if (explorationCell.isMissing() || exploitationCell.isMissing()) {
                    if (m_fail) {
                        throw new IllegalArgumentException("The row '" + row.getKey() + "' contains missing values.");
                    }
                    // set the same warning only once
                    if (!m_hasMissing) {
                        setWarningMessage(
                            "At least one row contains a missing value. Missing values will be in the output.");
                        m_hasMissing = true;
                    }
                    return new MissingCell("Input row contains missing values.");
                }
                final double explorationScore =
                    (1 - m_exploitationFactor) * ((DoubleValue)explorationCell).getDoubleValue();
                final double exploitationScore =
                    m_exploitationFactor * ((DoubleValue)exploitationCell).getDoubleValue();
                return new DoubleCell(exploitationScore + explorationScore);
            }
        });
        return rearranger;
    }

    private void autoConfigure(final DataTableSpec tableSpec) throws InvalidSettingsException {
        final List<DataColumnSpec> doubleColumns =
            tableSpec.stream().filter(c -> c.getType().isCompatible(DoubleValue.class)).collect(Collectors.toList());
        CheckUtils.checkSetting(doubleColumns.size() > 1, "At least two numeric columns must be in the input table.");
        m_exploitationColumnModel.setStringValue(doubleColumns.get(0).getName());
        m_explorationColumnModel.setStringValue(doubleColumns.get(1).getName());
    }

    private boolean isFirstConfigure() {
        return m_exploitationColumnModel.getStringValue() == null;
    }

    private void checkConfiguration(final DataTableSpec inSpec) throws InvalidSettingsException {
        // exploration column
        final String explorationColumnName = m_explorationColumnModel.getStringValue();
        if (explorationColumnName == null) {
            throw new InvalidSettingsException("No exploration score column is selected.");
        }
        final int explorationIndex = inSpec.findColumnIndex(explorationColumnName);
        if (explorationIndex < 0) {
            throw new InvalidSettingsException(
                "The selected exploration score column '" + explorationColumnName + "' is not available in the input.");
        }
        if (!inSpec.getColumnSpec(explorationIndex).getType().isCompatible(DoubleValue.class)) {
            throw new InvalidSettingsException(
                "The selected exploration score column '" + explorationColumnName + "' must be numeric.");
        }
        // exploitation column
        final String exploitationColumnName = m_exploitationColumnModel.getStringValue();
        if (exploitationColumnName == null) {
            throw new InvalidSettingsException("No exploitation score column is selected.");
        }
        final int exploitationIndex = inSpec.findColumnIndex(exploitationColumnName);
        if (exploitationIndex < 0) {
            throw new InvalidSettingsException("The selected exploitation score column '" + exploitationColumnName
                + "' is not available in the input.");
        }
        if (!inSpec.getColumnSpec(exploitationIndex).getType().isCompatible(DoubleValue.class)) {
            throw new InvalidSettingsException(
                "The selected exploitation score column '" + exploitationColumnName + "' must be numeric.");
        }
        if (explorationIndex == exploitationIndex) {
            throw new InvalidSettingsException(SAME_COLUMN_ERROR);
        }
        // exploitation factor
        final double exploitationFactor = m_exploitationFactorModel.getDoubleValue();
        if (exploitationFactor < 0 || exploitationFactor > 1) {
            throw new InvalidSettingsException(
                "The exploitation factor must be in [0,1] but is " + exploitationFactor + ".");
        }
        // output column name
        if (m_outputColumnNameModel.getStringValue().trim().isEmpty()) {
            throw new InvalidSettingsException("The output column name must not be empty.");
        }
        // missing value handling
        final String exceptionHandlingStrategy = m_missingValueHandlingModel.getStringValue();
        if (Arrays.stream(ExceptionHandling.values()).map(ExceptionHandling::name)
            .noneMatch(exceptionHandlingStrategy::equals)) {
            throw new InvalidSettingsException(
                "Unknown option to handle missing values: '" + exceptionHandlingStrategy + "'");
        }
    }

    /**
     * @return Settings model for the exploration factor.
     */
    static SettingsModelDoubleBounded createExploitationFactorModel() {
        return new SettingsModelDoubleBounded("Exploitation Factor", 0.5, 0, 1);
    }

    /**
     * @return Settings model for the name of the exploitation column.
     */
    static SettingsModelString createExploitationColumnModel() {
        return new SettingsModelString("Exploitation Score Column", null);
    }

    /**
     * @return Settings model for the name of the exploration column.
     */
    static SettingsModelString createExplorationColumnModel() {
        return new SettingsModelString("Exploration Score Column", null);
    }

    /**
     * @return Settings model for the name of the output column.
     */
    static SettingsModelString createOutputColumnNameModel() {
        return new SettingsModelString("Output Column Name", "Uncertainty Distribution Score");
    }

    /**
     * @return Settings Model to store the Exception Handling Model
     */
    static SettingsModelString createMissingValueHandlingModel() {
        return new SettingsModelString("Missing Value Handling", ExceptionHandling.FAIL.name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_exploitationColumnModel.saveSettingsTo(settings);
        m_explorationColumnModel.saveSettingsTo(settings);
        m_exploitationFactorModel.saveSettingsTo(settings);
        m_outputColumnNameModel.saveSettingsTo(settings);
        m_missingValueHandlingModel.saveSettingsTo(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_exploitationColumnModel.validateSettings(settings);
        m_explorationColumnModel.validateSettings(settings);
        m_exploitationFactorModel.validateSettings(settings);
        m_outputColumnNameModel.validateSettings(settings);
        m_missingValueHandlingModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_exploitationColumnModel.loadSettingsFrom(settings);
        m_explorationColumnModel.loadSettingsFrom(settings);
        m_exploitationFactorModel.loadSettingsFrom(settings);
        m_outputColumnNameModel.loadSettingsFrom(settings);
        m_missingValueHandlingModel.loadSettingsFrom(settings);
    }

}
