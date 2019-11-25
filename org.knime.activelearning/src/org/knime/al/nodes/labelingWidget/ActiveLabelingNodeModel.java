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
import org.knime.core.data.DoubleValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.js.core.JSONDataTable;
import org.knime.js.core.node.table.AbstractTableNodeModel;

/**
 * This is the model implementation of ActiveLearning.
 *
 *
 * @author Daniel Bogenrieder, KNIME GmbH, Konstanz, Germany
 */
public class ActiveLabelingNodeModel
    extends AbstractTableNodeModel<ActiveLabelingViewRepresentation, ActiveLabelingViewValue> {

    private static final String DEFAULT_Label = "?";

    private static final String SKIP_NAME = "Skip";

    private static final String DEFAULT_COLOR = "F0F0F0";

    private final LinkedList<Integer> colorScheme1 = new LinkedList<Integer>();

    private final ActiveLabelingConfig m_config;

//    private final LinkedList<Integer> colorScheme2 = new LinkedList<Integer>();

    private final SettingsModelString m_colorSchemeModel = createColorSchemeSettingsModel();

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
            if (viewRepresentation.getSettings().getTable() == null) {
                m_table = (BufferedDataTable)inObjects[0];
                final JSONDataTable jsonTable =
                    createJSONTableFromBufferedDataTable(m_table, exec.createSubExecutionContext(0.33));
                viewRepresentation.getSettings().setTable(jsonTable);
                copyConfigToRepresentation();

                // Load possible domain values into representation
                // final String possibleValuesColumnName = m_tableViewPossibleValuesModel.getStringValue();
                String possibleValuesColumnName = m_config.getLabelCol();
                if (possibleValuesColumnName == null) {
                    final Map<String, Integer> colors = new HashMap<String, Integer>();
                    colors.put(SKIP_NAME, Integer.parseInt(DEFAULT_COLOR, 16));
                    viewRepresentation.setColors(colors);
                } else if (m_table.getDataTableSpec().getColumnSpec(possibleValuesColumnName) == null) {
                    throw new InvalidSettingsException("The column which is selected for possible values is invalid");
                } else {
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
            final ActiveLabelingViewValue viewValue = getViewValue();
            final DataTableSpec spec = m_table.getDataTableSpec();
            final ColumnRearranger rearranger = createColumnRearranger(spec);
            out = exec.createColumnRearrangeTable(m_table, rearranger, exec);

            final BufferedDataContainer dc = exec.createDataContainer(out.getDataTableSpec());


            final int index;
            if (m_config.isAppendRadio()) {
                index = out.getSpec().findColumnIndex(m_config.getAppendCol());
            } else {
                index = out.getSpec().findColumnIndex(m_config.getReplaceCol());
            }
            for (final DataRow row : out) {
                final DataCell[] copy = new DataCell[row.getNumCells()];
                for (int i = 0; i < row.getNumCells(); i++) {
                    final DataCell cell = row.getCell(i);
                    copy[i] = cell;
                }
                // Check if row got a label assigned
                if (viewValue.getLabels().keySet().contains(row.getKey().toString())) {
                    if (viewValue.getLabels().get(row.getKey().toString()) != null) {
                        if (viewValue.getLabels().get(row.getKey().toString()) == "") {
                            copy[index] = new StringCell(DEFAULT_Label);
                        } else {
                            copy[index] = new StringCell(viewValue.getLabels().get(row.getKey().toString()));
                        }
                    }
                }

                dc.addRowToTable(new DefaultRow(row.getKey(), copy));
            }
            dc.close();
            out = dc.getTable();
        }

        // return modified Table
        exec.setProgress(1);
        return new PortObject[]{out};
    }

    /** {@inheritDoc} */
    protected ColumnRearranger createColumnRearranger(final DataTableSpec in) throws InvalidSettingsException {
        final String value = m_config.getAppendCol();
        final String colName;
//        checkSetting(value != null, "Configuration missing.");
//
//        checkSetting(!(m_config.getReplacedColumn() == null && m_config.getNewColumnName() == null),
//            "Either a replacing column or a new column name must be specified");

        if (m_config.isReplaceRadio()) {
            colName = m_config.getReplaceCol();
        } else {
            colName = m_config.getAppendCol();
        }
        final int replacedColumn = in.findColumnIndex(colName);

//        checkSetting(!(colName != null && replacedColumn < 0), "Column to replace: '%s' does not exist in input table",
//            colName);

        String newName =
                replacedColumn >= 0 ? colName : DataTableSpec.getUniqueColumnName(in, m_config.getAppendCol());

        final DataColumnSpec outColumnSpec = new DataColumnSpecCreator(newName, StringCell.TYPE).createSpec();

        final DataCell constantCell = new MissingCell(DEFAULT_Label);

        final ColumnRearranger rearranger = new ColumnRearranger(in);
        final CellFactory fac = new SingleCellFactory(outColumnSpec) {
            @Override
            public DataCell getCell(final DataRow row) {
                return constantCell;
            }
        };

        if (replacedColumn >= 0) {
            rearranger.replace(fac, replacedColumn);
        } else {
            rearranger.append(fac);
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

        final ColumnRearranger rearranger = createColumnRearranger((DataTableSpec)inSpecs[0]);

        final DataTableSpec tableSpec = rearranger.createSpec();

        for (final DataColumnSpec colspec : tableSpec) {
            if (colspec.getType().isCompatible(DoubleValue.class)
                || colspec.getType().isCompatible(org.knime.core.data.StringValue.class)) {
                allAllowedCols.add(colspec.getName());
            }
        }

        if (tableSpec.getNumColumns() < 1 || allAllowedCols.size() < 1) {
            throw new InvalidSettingsException(
                "Data table must have" + " at least one numerical or categorical column.");
        }

        return new PortObjectSpec[]{tableSpec};
    }

    static SettingsModelString createColorSchemeSettingsModel() {
        return new SettingsModelString("COLOR_SCHEME", "");
    }

    private Integer[] getCurrentColorScheme() {
        switch (getViewRepresentation().getColorScheme()) {
            case "Scheme 1":
                return colorScheme1.toArray(new Integer[0]);
//            case "Scheme 2":
//                return colorScheme2.toArray(new Integer[0]);
            default:
                return null;
        }
    }

    private Integer getNextColorFromScheme() {
        switch (getViewRepresentation().getColorScheme()) {
            case "Scheme 1":
                colorScheme1.add(colorScheme1.peek());
                return colorScheme1.remove();
//            case "Scheme 2":
//                colorScheme2.add(colorScheme2.peek());
//                return colorScheme2.remove();
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

//        colorScheme2.add(Integer.parseInt("a6cee3", 16));
//        colorScheme2.add(Integer.parseInt("1f78b4", 16));
//        colorScheme2.add(Integer.parseInt("b2df8a", 16));
//        colorScheme2.add(Integer.parseInt("33a02c", 16));
//        colorScheme2.add(Integer.parseInt("fb9a99", 16));
//        colorScheme2.add(Integer.parseInt("e31a1c", 16));
//        colorScheme2.add(Integer.parseInt("fdbf6f", 16));
//        colorScheme2.add(Integer.parseInt("ff7f00", 16));
//        colorScheme2.add(Integer.parseInt("cab2d6", 16));
//        colorScheme2.add(Integer.parseInt("6a3d9a", 16));
//        colorScheme2.add(Integer.parseInt("ffff99", 16));
//        colorScheme2.add(Integer.parseInt("b15928", 16));
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
