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
 *   Nov 7, 2019 (Perla Gjoka, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.al.nodes.score.uncertainty;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.knime.al.nodes.score.ExceptionHandling;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.probability.ProbabilityDistributionValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 */
final class UncertaintyNodeDialog extends NodeDialogPane {
    private DataTableSpec m_spec;

    private final SettingsModelColumnFilter2 m_numericColumnsFilterModel =
        AbstractUncertaintyNodeModel.createColumnFilterModel();

    private final DialogComponentButtonGroup m_columnTypeModel = new DialogComponentButtonGroup(
        AbstractUncertaintyNodeModel.createColumnTypeFilterModel(), null, true, ColumnType.values());

    private final DialogComponentColumnFilter2 m_numericColumnsFilterComponent =
        new DialogComponentColumnFilter2(m_numericColumnsFilterModel, 0, false);

    private final SettingsModelString m_probabilityColumnSettingsModel =
        AbstractUncertaintyNodeModel.createSingleColumnFilterModel();

    private final DialogComponentString m_outputName = new DialogComponentString(
        AbstractUncertaintyNodeModel.createColumnNameModel(null), "Output column name ", true, 17);

    private final DialogComponentButtonGroup m_exceptionHandling = new DialogComponentButtonGroup(
        AbstractUncertaintyNodeModel.createExceptionHandlingModel(), null, true, ExceptionHandling.values());

    @SuppressWarnings("unchecked")
    private final ColumnSelectionComboxBox m_singleColumnSelection =
        new ColumnSelectionComboxBox((Border)null, ProbabilityDistributionValue.class);

    public UncertaintyNodeDialog() {
        m_columnTypeModel.getModel().addChangeListener(e -> updateColumnNumber());
        JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 0;
        panel.add(createColumnSelectionPanel(), c);
        c.gridy++;
        c.weighty = 0;
        panel.add(createOutputSettingsPanel(), c);
        c.gridy++;
        panel.add(createInvalidHandlingPanel(), c);
        addTab("Default Settings", panel);

    }

    private JPanel createColumnSelectionPanel() {
        JPanel columnSelection = new JPanel(new GridBagLayout());
        columnSelection.setBorder(BorderFactory.createTitledBorder("Column Selection"));
        final GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        columnSelection.add(m_columnTypeModel.getButton(ColumnType.PROBABILITY_COLUMN.getActionCommand()), c);
        c.gridx = 1;
        c.insets = new Insets(0, 80, 0, 0);
        columnSelection.add(m_singleColumnSelection, c);
        c.gridx = 0;
        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        columnSelection.add(m_columnTypeModel.getButton(ColumnType.NUMERIC_COLUMNS.getActionCommand()), c);
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        columnSelection.add(m_numericColumnsFilterComponent.getComponentPanel(), c);
        return columnSelection;
    }

    private JPanel createOutputSettingsPanel() {
        JPanel outputPanel = new JPanel();
        outputPanel.setBorder(BorderFactory.createTitledBorder("Output Settings"));
        outputPanel.add(m_outputName.getComponentPanel());
        return outputPanel;
    }

    private JPanel createInvalidHandlingPanel() {
        JPanel invalidHandling = new JPanel();
        invalidHandling.setBorder(BorderFactory.createTitledBorder("Invalid Handling"));
        invalidHandling.add(m_exceptionHandling.getComponentPanel());
        return invalidHandling;
    }

    private void updateColumnNumber() {
        final boolean isMultiple = !isSingleColumn();
        m_numericColumnsFilterComponent.getModel().setEnabled(isMultiple);
        if (m_spec.containsCompatibleType(ProbabilityDistributionValue.class)) {
            m_singleColumnSelection.setEnabled(!isMultiple);
        } else {
            m_singleColumnSelection.setEnabled(false);
            m_columnTypeModel.getButton(ColumnType.PROBABILITY_COLUMN.getActionCommand()).setEnabled(false);
        }
    }

    private boolean isSingleColumn() {
        return m_columnTypeModel.getButton(ColumnType.PROBABILITY_COLUMN.getActionCommand()).isSelected();
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_spec = specs[0];
        m_numericColumnsFilterComponent.loadSettingsFrom(settings, specs);
        m_columnTypeModel.loadSettingsFrom(settings, specs);
        m_outputName.loadSettingsFrom(settings, specs);
        m_exceptionHandling.loadSettingsFrom(settings, specs);
        try {
            m_probabilityColumnSettingsModel.loadSettingsFrom(settings);
            updateSelectionPanel(m_probabilityColumnSettingsModel.getStringValue());
        } catch (InvalidSettingsException ex) {
            updateSelectionPanel(null);
        }
        updateColumnNumber();
    }

    private void updateSelectionPanel(final String columnSelection) {
        try {
            m_singleColumnSelection.update(m_spec, columnSelection);
        } catch (NotConfigurableException ex) {
            m_columnTypeModel.getButton(ColumnType.NUMERIC_COLUMNS.getActionCommand()).setSelected(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        if (m_spec != null) {
            if (!isSingleColumn() && m_numericColumnsFilterModel.applyTo(m_spec).getIncludes().length < 2) {
                throw new InvalidSettingsException("At least two columns must be included.");
            }
            if (isSingleColumn() && m_spec.findColumnIndex(m_singleColumnSelection.getSelectedColumn()) == -1) {
                throw new InvalidSettingsException("At least one probability distribution column must be selected.");
            }
        }
        m_numericColumnsFilterComponent.saveSettingsTo(settings);
        m_probabilityColumnSettingsModel.setStringValue(m_singleColumnSelection.getSelectedColumn());
        m_probabilityColumnSettingsModel.saveSettingsTo(settings);
        m_columnTypeModel.saveSettingsTo(settings);
        m_outputName.saveSettingsTo(settings);
        m_exceptionHandling.saveSettingsTo(settings);

    }

}
