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
 *   Jun 28, 2015 (gabriel): created
 */
package org.knime.al.nodes.score.uncertainty;

import java.util.Arrays;

import org.knime.al.nodes.score.ExceptionHandling;
import org.knime.al.util.MathUtils;
import org.knime.al.util.NodeTools;
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
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.knime.core.util.UniqueNameGenerator;

/**
 * Abstract Superclass for the node models of the Uncertainty scorers. Houses all the common methods, subclasses only
 * need to implement the {@link #createColumnRearranger(DataTableSpec)} method.
 *
 * @author gabriel
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractUncertaintyNodeModel extends SimpleStreamableFunctionNodeModel {

    /** The config key used for the column name setting. */
    protected static final String CFG_KEY_COLUMN_NAME = "column_name";

    /** The m_column filter model. */
    protected final SettingsModelColumnFilter2 m_columnFilterModel = createColumnFilterModel();

    /** The m_column filter model. */
    protected final SettingsModelString m_columnNameModel = createColumnNameModel(getDefaultColumnName());

    /** The m_column filter model. */
    protected final SettingsModelString m_exceptionHandlingModel = createExceptionHandlingModel();

    /**
     * @return Settings Model to store the Column Filter Model
     */
    @SuppressWarnings("unchecked")
    static SettingsModelColumnFilter2 createColumnFilterModel() {
        return new SettingsModelColumnFilter2("filter_string2", DoubleValue.class);
    }

    /**
     * @param defColName the default column name to set
     * @return Settings Model to store the Column Name Model
     */
    static SettingsModelString createColumnNameModel(final String defColName) {
        // we have to override the validation and loading methods in order to ensure backwards compatibility
        return new SettingsModelString(CFG_KEY_COLUMN_NAME, defColName) {
            @Override
            protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
                settings.getString(CFG_KEY_COLUMN_NAME, defColName);
            }

            @Override
            protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
                try {
                    // use the default value, if no value is stored in the settings
                    setStringValue(settings.getString(CFG_KEY_COLUMN_NAME, defColName));
                } catch (final IllegalArgumentException iae) {
                    // if the argument is not accepted: keep the old value.
                }
            }
        };
    }

    /**
     * @return Settings Model to store the Exception Handling Model
     */
    static SettingsModelString createExceptionHandlingModel() {
        // we have to override the validation and loading methods in order to ensure backwards compatibility
        return new SettingsModelString("exception_handling", ExceptionHandling.FAIL.name()) {
            @Override
            protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
                settings.getString("exception_handling", ExceptionHandling.FAIL.name());
            }

            @Override
            protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
                try {
                    // use the default value, if no value is stored in the settings
                    setStringValue(settings.getString("exception_handling", ExceptionHandling.FAIL.name()));
                } catch (final IllegalArgumentException iae) {
                    // if the argument is not accepted: keep the old value.
                }
            }
        };
    }

    /**
     * @return the default output column name
     */
    protected abstract String getDefaultColumnName();

    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec inSpec) throws InvalidSettingsException {
        // check configuration
        if (m_columnFilterModel.applyTo(inSpec).getIncludes().length < 2) {
            throw new InvalidSettingsException("At least two columns must be included.");
        }
        if (m_columnNameModel.getStringValue().trim().isEmpty()) {
            throw new InvalidSettingsException("The column name must not be empty.");
        }

        final String exceptionHandlingStrategy = m_exceptionHandlingModel.getStringValue();
        if (!Arrays.stream(ExceptionHandling.values()).map(e -> e.name()).anyMatch(exceptionHandlingStrategy::equals)) {
            throw new InvalidSettingsException(
                "Unknown option to handle exceptions: '" + exceptionHandlingStrategy + "'");
        }
        final DataColumnSpec newColSpec =
            new UniqueNameGenerator(inSpec).newColumn(m_columnNameModel.getStringValue(), DoubleCell.TYPE);

        // create column rearranger
        final ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        rearranger.append(new SingleCellFactory(newColSpec) {

            final int[] m_columnIndices = inSpec.columnsToIndices(m_columnFilterModel.applyTo(inSpec).getIncludes());

            final boolean m_fail = m_exceptionHandlingModel.getStringValue().equals(ExceptionHandling.FAIL.name());

            boolean m_hasMissing = false;

            boolean m_hasInvalidDistribution = false;

            @Override
            public DataCell getCell(final DataRow row) {
                final double[] values = NodeTools.toDoubleArray(row, m_columnIndices);
                if (values == null) {
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
                if (!MathUtils.checkDistribution(values)) {
                    if (m_fail) {
                        throw new IllegalArgumentException("The distribution of row '" + row.getKey().getString()
                            + "' is invalid as it does not sum up to 1.");
                    }
                    // set the same warning only once
                    if (!m_hasInvalidDistribution) {
                        setWarningMessage(
                            "The distribution of at least one row is invalid as it does not sum up to 1. Missing "
                                + "values will be in the output.");
                        m_hasInvalidDistribution = true;
                    }
                    return new MissingCell("The distribution is invalid as it does not sum up to 1.");
                }
                return new DoubleCell(calculateUncertainty(values));
            }
        });
        return rearranger;
    }

    /**
     * Calculates the uncertainty which is the actual output.
     *
     * @param values the distribution values
     * @return the calculated uncertainty
     */
    protected abstract double calculateUncertainty(final double[] values);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_columnFilterModel.saveSettingsTo(settings);
        m_columnNameModel.saveSettingsTo(settings);
        m_exceptionHandlingModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnFilterModel.validateSettings(settings);
        m_columnNameModel.validateSettings(settings);
        m_exceptionHandlingModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnFilterModel.loadSettingsFrom(settings);
        m_columnNameModel.loadSettingsFrom(settings);
        m_exceptionHandlingModel.loadSettingsFrom(settings);
    }

}
