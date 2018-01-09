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

import java.util.ArrayList;
import java.util.List;

import org.knime.al.nodes.AbstractALNodeModel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;

/**
 * Abstract Superclass for the node models of the Uncertainty scorers. Houses
 * all the common methods, subclasses only need to implement the
 * {@link #createResRearranger(DataTableSpec)} method.
 *
 * @author gabriel
 */
public abstract class AbstractUncertaintyNodeModel extends AbstractALNodeModel {

    /** The m_column filter model. */
    protected final SettingsModelColumnFilter2 m_columnFilterModel =
            createColumnFilterModel();

    /**
     */
    protected AbstractUncertaintyNodeModel() {
        super(1, 1);
    }

    /**
     * @return Settings Model to store the Column Filter Model
     */
    @SuppressWarnings("unchecked")
    public static SettingsModelColumnFilter2 createColumnFilterModel() {
        return new SettingsModelColumnFilter2("filter_string2",
                DoubleValue.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[] {
                createResRearranger(inSpecs[0]).createSpec() };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        if (m_columnFilterModel.applyTo(inData[0].getDataTableSpec())
                .getIncludes().length == 0) {
            throw new InvalidSettingsException("No Columns selected!");
        }

        final ColumnRearranger c =
                createResRearranger(inData[0].getDataTableSpec());
        final BufferedDataTable out =
                exec.createColumnRearrangeTable(inData[0], c, exec);
        return new BufferedDataTable[] { out };
    }

    /**
     * Creates the column rearranger that performs the calculation.
     *
     * @param inPutSpec
     *            the input spec of the node
     * @return A column rearranger that implements the exploitation strategy
     * @throws InvalidSettingsException
     *             if invalid settings are detected.
     */
    protected abstract ColumnRearranger createResRearranger(
            final DataTableSpec inPutSpec) throws InvalidSettingsException;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // Nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<SettingsModel> collectSettingsModels() {
        if (m_settingsModels == null) {
            m_settingsModels = new ArrayList<>(1);
            m_settingsModels.add(m_columnFilterModel);
        }
        return m_settingsModels;
    }

    /**
     * Handle invalid distribution.
     *
     * @param key
     *            the row in which the invalid distribution occured.
     */
    public static final void handleInvalidDistribution(final RowKey key) {
        throw new IllegalArgumentException(
                "The distribution in Row: '" + key.getString()
                        + "' is invalid, as it does not sum up to 1.");
    }

}
