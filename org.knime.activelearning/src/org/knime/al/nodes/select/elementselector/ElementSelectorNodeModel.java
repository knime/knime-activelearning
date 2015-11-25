/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *
 */
package org.knime.al.nodes.select.elementselector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.knime.al.nodes.AbstractALNodeModel;
import org.knime.al.nodes.select.elementselector.ElementSelectorSettingsModels.ElementSelectionStrategy;
import org.knime.al.util.EnumUtils;
import org.knime.al.util.NodeUtils;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is the model implementation of the Element Selector. It sorts the table
 * on the given row with the {@link BufferedDataTableSorter} and returns the top
 * or bottom N elements.
 *
 * @author Marvin Kickuth, University of Konstanz
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */

public class ElementSelectorNodeModel extends AbstractALNodeModel {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(ElementSelectorNodeModel.class);

    // the settings model for the number of rows to return
    private final SettingsModelIntegerBounded m_numberOfElementsModel =
            ElementSelectorSettingsModels.createNumElementsModel();

    // the settings model storing the column to search
    private final SettingsModelString m_columnModel =
            ElementSelectorSettingsModels.createColumnModel();

    private final SettingsModelString m_selectionStrategyModel =
            ElementSelectorSettingsModels.createElementSelectionStrategyModel();

    private ElementSelectionStrategy m_selectionStrategy;

    /**
     * Constructor for the node model.
     */
    protected ElementSelectorNodeModel() {

        // one input port, one output port
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        // select sort strategy
        boolean[] sortAscending;
        switch (m_selectionStrategy) {
        case BIGGEST:
            sortAscending = new boolean[] { false };
            break;
        case SMALLEST:
            sortAscending = new boolean[] { true };
            break;
        default:
            throw new IllegalStateException(
                    "No case defined for this ElementSelectionStrategy:"
                            + m_selectionStrategy.toString());
        }

        // Sort
        final List<String> columns =
                Arrays.asList(m_columnModel.getStringValue());

        final int index = inData[0].getSpec()
                .findColumnIndex(m_columnModel.getStringValue());

        final BufferedDataTableSorter sorter =
                new BufferedDataTableSorter(inData[0], columns, sortAscending);
        final BufferedDataTable sorted = sorter.sort(exec);

        exec.setMessage("Writing result table");
        final BufferedDataContainer container =
                exec.createDataContainer(inData[0].getDataTableSpec());
        final CloseableRowIterator iterator = sorted.iterator();

        int i = 0;
        while (i < m_numberOfElementsModel.getIntValue()) {
            try {
                final DataRow next = iterator.next();
                // skip rows with missing
                if (next.getCell(index).isMissing()) {
                    LOGGER.warn("Encountered missing value, skipping row: "
                            + next.getKey());
                    continue;
                } else {
                    container.addRowToTable(next);
                    i++;
                }
            } catch (final NoSuchElementException e) {
                LOGGER.warn("Not enough Elements in the input table to satisfy "
                        + "the number of elements setting.");
                break;
            }
        }
        container.close();

        // output the result as a BufferedDataTable[].
        return new BufferedDataTable[] { container.getTable() };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

        m_selectionStrategy = EnumUtils.valueForName(
                m_selectionStrategyModel.getStringValue(),
                ElementSelectionStrategy.values());

        NodeUtils.autoColumnSelection(inSpecs[0], m_columnModel,
                DoubleValue.class, ElementSelectorNodeModel.class);

        return new DataTableSpec[] { inSpecs[0] };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<SettingsModel> collectSettingsModels() {
        if (m_settingsModels == null) {
            m_settingsModels = new ArrayList<>();
            m_settingsModels.add(m_columnModel);
            m_settingsModels.add(m_numberOfElementsModel);
            m_settingsModels.add(m_selectionStrategyModel);
        }
        return m_settingsModels;
    }

}