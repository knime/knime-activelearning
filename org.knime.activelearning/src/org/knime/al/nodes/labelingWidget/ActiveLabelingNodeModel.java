/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.al.nodes.labelingWidget;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.js.core.JSONDataTable;
import org.knime.js.core.node.table.AbstractTableNodeModel;

import com.google.common.collect.Iterators;

/**
 * This is the model implementation of ActiveLearning.
 *
 *
 * @author Daniel Bogenrieder, KNIME GmbH, Konstanz, Germany
 */
public class ActiveLabelingNodeModel
    extends AbstractTableNodeModel<ActiveLabelingViewRepresentation, ActiveLabelingViewValue> {

    private static final String DEFAULT_LABEL = "?";

    private static final String SKIP_NAME = "Skip";

    private static final String DEFAULT_COLOR = "808080";

    private final LinkedList<Integer> colorScheme1 = new LinkedList<Integer>();

    private final ActiveLabelingConfig m_config;

    protected ActiveLabelingNodeModel(final String viewName) {
        super(viewName, new ActiveLabelingConfig());
        fillColorSchemes();
        m_config = new ActiveLabelingConfig();
    }

    @Override
    public ActiveLabelingViewRepresentation createEmptyViewRepresentation() {
        return new ActiveLabelingViewRepresentation();
    }

    @Override
    public ActiveLabelingViewValue createEmptyViewValue() {
        return new ActiveLabelingViewValue();
    }

    @Override
    public String getJavascriptObjectID() {
        return "org.knime.activelearning.labelingWidget";
    }

    @Override
    protected PortObject[] performExecute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        BufferedDataTable out = (BufferedDataTable)inObjects[0];
        synchronized (getLock()) {
            final ActiveLabelingViewRepresentation viewRepresentation = getViewRepresentation();
            final ActiveLabelingViewValue viewValue = getViewValue();
            if (viewRepresentation.getSettings().getTable() == null) {
                m_table = (BufferedDataTable)inObjects[0];
                final JSONDataTable jsonTable =
                    createJSONTableFromBufferedDataTable(m_table, exec.createSubExecutionContext(0.33));
                viewRepresentation.getSettings().setTable(jsonTable);
                copyConfigToRepresentation();

                // Load possible domain values into representation
                String possibleValuesColumnName = m_config.getLabelCol();
                DataTableSpec m_spec = m_table.getDataTableSpec();
                if (possibleValuesColumnName == null) {
                    final Map<String, Integer> colors = new HashMap<String, Integer>();
                    colors.put(SKIP_NAME, Integer.parseInt(DEFAULT_COLOR, 16));
                    viewRepresentation.setColors(colors);
                } else if (m_spec == null || m_spec.getColumnSpec(possibleValuesColumnName) == null) {
                    throw new InvalidSettingsException("The column which is selected for possible values is missing");
                } else {
                    if (!m_config.getUseExistingLabels() && viewValue.getLabels().isEmpty()) {
                        int possibleValuesColumnIndex = m_spec.findColumnIndex(possibleValuesColumnName);
                        TableFilter tableFilter = TableFilter.materializeCols(possibleValuesColumnIndex);
                        Map<String, String> existingLabels = new HashMap<String, String>();
                        int maxRows = m_config.getSettings().getRepresentationSettings().getMaxRows();
                        try (CloseableRowIterator inDataIterator = ((BufferedDataTable)inObjects[0]).filter(tableFilter).iterator()) {
                            Iterators.limit(inDataIterator, maxRows);
                            for (int i = 0; inDataIterator.hasNext() && i < maxRows; i++) {
                                final DataRow row = inDataIterator.next();
                                DataCell missingCell = DataType.getMissingCell();
                                DataCell labelCell = row.getCell(possibleValuesColumnIndex).isMissing() ?
                                    missingCell :
                                    row.getCell(possibleValuesColumnIndex);
                                existingLabels.put(row.getKey().toString(), labelCell.toString());
                            }
                            viewValue.setLabels(existingLabels);
                        }
                    }
                    final Set<DataCell> possibleValuesSet =
                        m_table.getDataTableSpec().getColumnSpec(possibleValuesColumnName).getDomain().getValues();
                    final Set<String> values = new HashSet<String>();
                    for (final DataCell dc : possibleValuesSet) {
                        values.add(dc.toString());
                    }
                    viewRepresentation.setPossibleLabelValues(values.toArray(new String[0]));
                    final Map<String, Integer> colors = new HashMap<String, Integer>();
                    // Check if there are Colors defined in the Color Scheme and define which color
                    // belongs to which label. If no Color Scheme is found all labels get the same
                    // default color.
                    if (getCurrentColorScheme() != null) {
                        for (final String label : values) {
                            if (!colors.containsKey(label)) {
                                colors.put(label, getNextColorFromScheme());
                            }
                        }
                        colors.put(SKIP_NAME, Integer.parseInt(DEFAULT_COLOR, 16));
                    }
                    viewRepresentation.setColors(colors);
                }
            }
            // Add labels from view to table
            final DataTableSpec spec = m_table.getDataTableSpec();
            final ColumnRearranger rearranger = createColumnRearranger(spec, viewValue.getLabels());
            out = exec.createColumnRearrangeTable(m_table, rearranger, exec);
        }

        // return modified Table
        exec.setProgress(1);
        return new PortObject[]{out};
    }

    /** {@inheritDoc} */
    protected ColumnRearranger createColumnRearranger(final DataTableSpec in, final Map<String, String> labelList) throws InvalidSettingsException {
        final String value = m_config.getAppendCol();
        final String colName;

        if (m_config.isReplaceRadio()) {
            colName = m_config.getReplaceCol();
        } else {
            colName = m_config.getAppendCol();
        }

        final boolean usingSameColumn = colName != m_config.getLabelCol();
        final int replacedColumn = in.findColumnIndex(colName);
        final int possibleValuesCol = in.findColumnIndex(m_config.getLabelCol());
        final boolean useUniqueColumnName = m_config.isAppendRadio() && (replacedColumn > -1  || usingSameColumn);

        final String newName = useUniqueColumnName ? DataTableSpec.getUniqueColumnName(in, colName) : colName;


        final DataColumnSpec outColumnSpec = new DataColumnSpecCreator(newName, StringCell.TYPE).createSpec();
        final ColumnRearranger rearranger = new ColumnRearranger(in);
        final Map<String, String> alreadyLabeled = new HashMap<String, String>();
        CellFactory fac = new SingleCellFactory(outColumnSpec) {

            private int m_rowIndex = 0;

            @Override
            public DataCell getCell(final DataRow row) {
                if (++m_rowIndex > m_config.getSettings().getRepresentationSettings().getMaxRows()) {
                    return DataType.getMissingCell();
                }
                if (possibleValuesCol > -1 && !row.getCell(possibleValuesCol).getClass().equals(MissingCell.class)) {
                    alreadyLabeled.put(row.getKey().getString(), row.getCell(possibleValuesCol).toString());
                }
                if (labelList == null) {
                    return DataType.getMissingCell();
                } else if (labelList.keySet().contains(row.getKey().toString()) &&
                    !labelList.get(row.getKey().toString()).equals(SKIP_NAME) &&
                    !labelList.get(row.getKey().toString()).equals(DEFAULT_LABEL)) {
                    return new StringCell(labelList.get(row.getKey().toString()));
                } else {
                    return DataType.getMissingCell();
                }
            }
        };

        if (getViewValue() != null && alreadyLabeled.size() > 0) {
            getViewValue().setLabels(alreadyLabeled);
        }

        if (useUniqueColumnName) {
            rearranger.append(fac);
        } else {
            rearranger.replace(fac, replacedColumn);
        }
        return rearranger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.loadSettings(settings);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final List<String> allAllowedCols = new LinkedList<String>();
        final ActiveLabelingViewValue viewValue = getViewValue();
        final ColumnRearranger rearranger;

        String possibleValuesColumnName = m_config.getLabelCol();
        if (((DataTableSpec)inSpecs[0]).getColumnSpec(possibleValuesColumnName) == null) {
            throw new InvalidSettingsException("The column which is selected for possible values is missing");
        }
        if (viewValue == null) {
            rearranger = createColumnRearranger((DataTableSpec)inSpecs[0], null);
        } else {
            rearranger = createColumnRearranger((DataTableSpec)inSpecs[0], viewValue.getLabels());
        }

        final DataTableSpec tableSpec = rearranger.createSpec();

        for (final DataColumnSpec colspec : tableSpec) {
            if (colspec.getType().isCompatible(DoubleValue.class)
                || colspec.getType().isCompatible(org.knime.core.data.StringValue.class)) {
                allAllowedCols.add(colspec.getName());
            }
        }

        if (tableSpec.getNumColumns() < 1 || allAllowedCols.size() < 1) {
            throw new InvalidSettingsException(
                "Data table must have at least one numerical or categorical column.");
        }

        return new PortObjectSpec[]{tableSpec};
    }

    static SettingsModelString createColorSchemeSettingsModel() {
        return new SettingsModelString("COLOR_SCHEME", "");
    }

    private Integer[] getCurrentColorScheme() {
        switch (getViewRepresentation().getColorScheme()) {
            case "Scheme 1":
                Collections.sort(colorScheme1);
                return colorScheme1.toArray(new Integer[0]);
            default:
                return null;
        }
    }

    private Integer getNextColorFromScheme() {
        switch (getViewRepresentation().getColorScheme()) {
            case "Scheme 1":
                colorScheme1.add(colorScheme1.get(0));
                return colorScheme1.remove();
            default:
                return null;
        }
    }

    private void fillColorSchemes() {
        colorScheme1.add(Integer.parseInt("a6cee3", 16));
        colorScheme1.add(Integer.parseInt("1f78b4", 16));
        colorScheme1.add(Integer.parseInt("b2df8a", 16));
        colorScheme1.add(Integer.parseInt("33a02c", 16));
        colorScheme1.add(Integer.parseInt("fb9a99", 16));
        colorScheme1.add(Integer.parseInt("e31a1c", 16));
        colorScheme1.add(Integer.parseInt("fdbf6f", 16));
        colorScheme1.add(Integer.parseInt("ff7f00", 16));
        colorScheme1.add(Integer.parseInt("cab2d6", 16));
        colorScheme1.add(Integer.parseInt("6a3d9a", 16));
        colorScheme1.add(Integer.parseInt("ffff99", 16));
        colorScheme1.add(Integer.parseInt("b15928", 16));

    }

    /**
     * Copies the settings from dialog into representation and values objects.
     */
    @Override
    protected void copyConfigToRepresentation() {
        synchronized (getLock()) {
            final ActiveLabelingConfig conf = m_config;
            final ActiveLabelingViewRepresentation viewRepresentation = getViewRepresentation();
            // Use setSettingsFromDialog, it ensures the table that got set on the representation settings is preserved
            viewRepresentation.setSettingsFromDialog(m_config.getSettings().getRepresentationSettings());
            viewRepresentation.setLabelColumnName(conf.getLabelCol());
            viewRepresentation.setLabelCol(conf.getLabelCol());
            viewRepresentation.setColorScheme(conf.getColorScheme());
            viewRepresentation.setColorSchemeValues(getCurrentColorScheme());
            viewRepresentation.setUseNumCols(conf.getUseNumCols());
            viewRepresentation.setUseColWidth(conf.getUseColWidth());
            viewRepresentation.setNumCols(conf.getNumCols());
            viewRepresentation.setColWidth(conf.getColWidth());
            viewRepresentation.setLabelCol(conf.getLabelCol());
            viewRepresentation.setUseRowID(conf.getUseRowID());
            viewRepresentation.setAlignLeft(conf.getAlignLeft());
            viewRepresentation.setAlignRight(conf.getAlignRight());
            viewRepresentation.setAlignCenter(conf.getAlignCenter());
            viewRepresentation.setUseProgressBar(conf.getUseProgressBar());
            viewRepresentation.setLabelCreation(conf.isAddLabelsDynamically());
            viewRepresentation.setAutoSelectNextTile(conf.isAutoSelectNextTile());
            viewRepresentation.setReplaceCol(conf.getReplaceCol());
            viewRepresentation.setAppendCol(conf.getAppendCol());

            final ActiveLabelingViewValue viewValue = getViewValue();
            if (isViewValueEmpty()) {
                viewValue.setSettings(m_config.getSettings().getValueSettings());
            }
        }
    }
}
