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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Jan 30, 2013 (hornm): created
 */
package org.knime.al.nodes.loop.start;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.al.nodes.loop.ActiveLearnLoopEnd;
import org.knime.al.nodes.loop.ActiveLearnLoopStart;
import org.knime.al.nodes.loop.ActiveLearnLoopUtils;
import org.knime.al.util.NodeTools;
import org.knime.al.util.NodeUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Active learning loop start. Gets newly labeled data from the active learning
 * loop end and reorganizes the input tables accordingly.
 *
 * @author dietzc, hornm, University of Konstanz
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public class ActiveLearnLoopStartNodeModel extends NodeModel
        implements ActiveLearnLoopStart {

    private static final int UNLABELED_KEY = 0;
    private static final int LABELED_KEY = 1;
    private static final int NEW_LABELED_KEY = 2;

    // Settings Models
    private List<SettingsModel> m_settingsModels;
    private final SettingsModelString m_classLabelColumnModel =
            ActiveLearnLoopStartSettingsModels.createClassLabelColumnModel();
    private final SettingsModelString m_customClassCollumnNameModel =
            ActiveLearnLoopStartSettingsModels
                    .createCustomClassColumnNameModel();
    private final SettingsModelBoolean m_appendClassCollumnModel =
            ActiveLearnLoopStartSettingsModels.createAppendClassColumnModel();
    private final SettingsModelBoolean m_appendIterationModel =
            ActiveLearnLoopStartSettingsModels.createAppendIterationModel();

    private final Set<String> m_deffinedClasses = new HashSet<>();

    private final Map<RowKey, Integer> m_labeledInIteration = new HashMap<>();

    private final Map<RowKey, String> m_allLabeledRowKeys = new HashMap<>();

    private Map<RowKey, String> m_newlyLabeledRows = null;

    private int m_currentIteration;

    private int m_classColIdx;

    /**
     *
     */
    protected ActiveLearnLoopStartNodeModel() {
        super(1, 3);

        // state consistency for the dialog
        m_classLabelColumnModel
                .setEnabled(!m_appendClassCollumnModel.getBooleanValue());
        m_customClassCollumnNameModel
                .setEnabled(m_appendClassCollumnModel.getBooleanValue());
    }

    /**
     * @return the deffinedClasses
     */
    @Override
    public Set<String> getDefinedClassLabels() {
        return m_deffinedClasses;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return createOutSpec(inSpecs[0]);
    }

    private DataTableSpec[] createOutSpec(final DataTableSpec inSpec)
            throws InvalidSettingsException {

        // locate class column
        String classColumnName;
        if (m_appendClassCollumnModel.getBooleanValue()) {
            classColumnName = m_customClassCollumnNameModel.getStringValue();
        } else {
            classColumnName = m_classLabelColumnModel.getStringValue();
        }

        // Fall back
        if (!m_appendClassCollumnModel.getBooleanValue()
                && !inSpec.containsName(classColumnName)) {
            classColumnName =
                    inSpec.getColumnNames()[NodeUtils.autoColumnSelection(
                            inSpec, m_classLabelColumnModel, StringValue.class,
                            ActiveLearnLoopStartNodeModel.class)];
        }

        // Create outspecs
        final DataTableSpec spec =
                createColumnRearranger(inSpec, classColumnName).createSpec();

        m_classColIdx = spec.findColumnIndex(classColumnName);

        final DataTableSpecCreator labeledRowsSpecCreator =
                new DataTableSpecCreator(spec);
        if (m_appendIterationModel.getBooleanValue()) {
            labeledRowsSpecCreator.addColumns(
                    new DataColumnSpecCreator("Iteration", IntCell.TYPE)
                            .createSpec());
        }
        return new DataTableSpec[] { spec, labeledRowsSpecCreator.createSpec(),
                spec };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        if ((getLoopEndNode() != null)
                && !(getLoopEndNode() instanceof ActiveLearnLoopEnd)) {
            throw new InvalidSettingsException(
                    "ActiveLearnLoopEndNode required!");
        }

        // get newly labeled rows from loop end.
        m_newlyLabeledRows = null;
        if (getLoopEndNode() != null) {
            m_newlyLabeledRows = ((ActiveLearnLoopEnd) getLoopEndNode())
                    .getNewlyLabeledRows();

            m_newlyLabeledRows.forEach((key, value) -> {
                m_allLabeledRowKeys.put(key, value);
                m_deffinedClasses.add(value);
            });
        }

        // Setup the tables and locate the class column
        final DataTableSpec[] specs = createOutSpec(inData[0].getSpec());

        final BufferedDataContainer unlabeledRowsTable =
                exec.createDataContainer(specs[UNLABELED_KEY]);
        final BufferedDataContainer newlyLabeledRowsTable =
                exec.createDataContainer(specs[NEW_LABELED_KEY]);
        final BufferedDataContainer allLabeledRowsTable =
                exec.createDataContainer(specs[LABELED_KEY]);

        for (final DataRow row : inData[0]) {
            final RowKey key = row.getKey();

            // in the first iteration load all already labeled cells
            if ((m_currentIteration == 0)
                    && !m_appendClassCollumnModel.getBooleanValue()) {

                // avoid reading missing cells
                final DataCell a = row.getCell(m_classColIdx);
                if (a instanceof StringValue) {
                    final String className = ((StringValue) a).getStringValue();
                    m_allLabeledRowKeys.put(key, className);
                    m_deffinedClasses.add(className);
                }
            }

            // write newly labeled rows to the tables
            if ((m_newlyLabeledRows != null)
                    && m_newlyLabeledRows.containsKey(key)) {
                handleNewlyLabledRow(newlyLabeledRowsTable, allLabeledRowsTable,
                        row, key);
            }

            // rewrite already labeled rows
            else if (m_allLabeledRowKeys.containsKey(key)) {
                handleAlreadyLabeledRow(newlyLabeledRowsTable,
                        allLabeledRowsTable, row, key);
            }

            // write unlabeled rows
            else {
                writeRowToTable(unlabeledRowsTable, row,
                        DataType.getMissingCell());
            }
        }

        newlyLabeledRowsTable.close();
        allLabeledRowsTable.close();
        unlabeledRowsTable.close();

        pushFlowVariableInt(ActiveLearnLoopUtils.AL_STEP, m_currentIteration);
        m_currentIteration++;

        return new BufferedDataTable[] { unlabeledRowsTable.getTable(),
                allLabeledRowsTable.getTable(),
                newlyLabeledRowsTable.getTable() };
    }

    /**
     * Handles an newly labeled row, writing it to the correct tables.
     *
     * @param newlyLabeledRowsTable
     * @param allLabeledRowsTable
     * @param row
     * @param key
     */
    private void handleNewlyLabledRow(
            final BufferedDataContainer newlyLabeledRowsTable,
            final BufferedDataContainer allLabeledRowsTable, final DataRow row,
            final RowKey key) {
        DataCell cell;
        String label;
        if ((label = m_newlyLabeledRows.get(key)) != null) {
            cell = new StringCell(label);
        } else {
            cell = DataType.getMissingCell();
        }
        m_labeledInIteration.put(row.getKey(), m_currentIteration);
        writeRowToTable(newlyLabeledRowsTable, row, cell);
        if (m_appendIterationModel.getBooleanValue()) {
            writeRowToTable(allLabeledRowsTable, row, cell, m_currentIteration);
        } else {
            writeRowToTable(allLabeledRowsTable, row, cell);
        }
    }

    /**
     * Handles an already labeled row , writing it to the correct tables.
     *
     * @param newlyLabeledRowsTable
     * @param allLabeledRowsTable
     * @param row
     * @param key
     */
    private void handleAlreadyLabeledRow(
            final BufferedDataContainer newlyLabeledRowsTable,
            final BufferedDataContainer allLabeledRowsTable, final DataRow row,
            final RowKey key) {
        final DataCell cell;
        String label;
        if ((label = m_allLabeledRowKeys.get(key)) == null) {
            cell = DataType.getMissingCell();
        } else {
            cell = new StringCell(label);
        }

        if (m_currentIteration == 0) {
            m_labeledInIteration.put(row.getKey(), 0);
            if (m_appendIterationModel.getBooleanValue()) {
                writeRowToTable(allLabeledRowsTable, row, cell, 0);
            } else {
                writeRowToTable(allLabeledRowsTable, row, cell);
            }
            // In the first iteration add externally labeled rows to the
            // newly labeled rows
            writeRowToTable(newlyLabeledRowsTable, row, cell);
        } else {
            if (m_appendIterationModel.getBooleanValue()) {
                writeRowToTable(allLabeledRowsTable, row, cell,
                        m_labeledInIteration.get(row.getKey()));
            } else {
                writeRowToTable(allLabeledRowsTable, row, cell);
            }
        }
    }

    /**
     * Writes the given row to the table, either replacing the class cell with
     * the one given as argument or appending i Also appends a cell with the
     * iteration number.
     *
     * @param table
     *            the table
     * @param row
     *            the row
     * @param cell
     *            the cell to be inserted
     * @param iteration
     *            (optional) the iteration number for this classification, only
     *            the first argument is evaluated!
     */
    private void writeRowToTable(final BufferedDataContainer table,
            final DataRow row, final DataCell cell, final int... iteration) {

        final List<DataCell> rowList = NodeTools.makeListFromRow(row);
        if (m_appendClassCollumnModel.getBooleanValue()) {
            rowList.add(cell);
        } else {
            rowList.set(m_classColIdx, cell);
        }
        if (iteration.length > 0) {
            rowList.add(new IntCell(iteration[0]));
        }
        table.addRowToTable(new DefaultRow(row.getKey(), rowList));
    }

    /**
     * Creates the createColumnRearranger for the output tables.
     *
     * @param inSpec
     *            the tableSpec
     * @param columnName
     *            the name of the class column.
     * @return the createColumnRearranger with the appended
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec,
            final String columnName) {
        final ColumnRearranger rearranger = new ColumnRearranger(inSpec);

        // append the
        if (m_appendClassCollumnModel.getBooleanValue()) {
            rearranger.append(createCellFactory(
                    m_customClassCollumnNameModel.getStringValue()));
        } else {
            final int classLabelColIdx = inSpec.findColumnIndex(columnName);
            rearranger.replace(createCellFactory(columnName), classLabelColIdx);
        }
        return rearranger;
    }

    /**
     * Creates a CellFactory for the class column.
     *
     * @param colName
     *            the name of the class column
     * @return CellFactory for the class column.
     */
    private CellFactory createCellFactory(final String colName) {
        return new CellFactory() {

            @Override
            public void setProgress(final int curRowNr, final int rowCount,
                    final RowKey lastKey, final ExecutionMonitor exec) {
                exec.setProgress((double) curRowNr / rowCount);
            }

            @Override
            public DataColumnSpec[] getColumnSpecs() {
                return new DataColumnSpec[] {
                        new DataColumnSpecCreator(colName, StringCell.TYPE)
                                .createSpec() };
            }

            @Override
            public DataCell[] getCells(final DataRow row) {
                throw new IllegalStateException(
                        new IllegalAccessException("This shouldn't be called"));
            }
        };
    }

    /*
     * Helper to collect all settings models and add them to one list (if not
     * already done)
     */
    private void collectSettingsModels() {
        if (m_settingsModels == null) {
            m_settingsModels = new ArrayList<SettingsModel>();
            m_settingsModels.add(m_classLabelColumnModel);
            m_settingsModels.add(m_customClassCollumnNameModel);
            m_settingsModels.add(m_appendClassCollumnModel);
            m_settingsModels.add(m_appendIterationModel);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        collectSettingsModels();
        m_settingsModels.forEach((sm) -> sm.saveSettingsTo(settings));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        collectSettingsModels();
        for (final SettingsModel sm : m_settingsModels) {
            sm.validateSettings(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        collectSettingsModels();
        for (final SettingsModel sm : m_settingsModels) {
            sm.loadSettingsFrom(settings);
        }
    }

    /**
     * final {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_allLabeledRowKeys.clear();
        m_labeledInIteration.clear();
        m_deffinedClasses.clear();
        m_currentIteration = 0;
    }
}
