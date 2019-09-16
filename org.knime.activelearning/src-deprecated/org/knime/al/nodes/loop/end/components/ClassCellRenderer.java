package org.knime.al.nodes.loop.end.components;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.knime.al.nodes.loop.end.ActiveLearnLoopEndSettingsModels;
import org.knime.core.node.defaultnodesettings.SettingsModelOptionalString;

/**
 * ClassCellRenderer
 *
 * Cell Renderer for lists and comboBoxes. Renders ClassModel.NO_CLASS as
 * Default class name, or as "?" when m_nodeModel.getAllowUnlabeled() is false
 *
 * @author Jonathan Hale
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 * @deprecated Retired in favor of new active learning loop based on standard recursive loop.
 */

@Deprecated
public class ClassCellRenderer extends DefaultListCellRenderer
        implements TableCellRenderer {

    private static final long serialVersionUID = 2111656611016131543L;

    private final SettingsModelOptionalString m_defaultClassModel =
            ActiveLearnLoopEndSettingsModels.createDefaultClassModel();

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getListCellRendererComponent(final JList<?> list,
            final Object value, final int index, final boolean isSelected,
            final boolean cellHasFocus) {
        // simply replace NO_CLASS value with the default class

        Object newValue;
        if (value == null) {
            // in case of null value
            return super.getListCellRendererComponent(list, " ", index,
                    isSelected, cellHasFocus);
        } else {
            newValue = value;
        }

        if (value instanceof String) {
            if (value.equals(ClassModel.NO_CLASS)) {
                if (m_defaultClassModel.isActive()) {
                    newValue = m_defaultClassModel.getStringValue();
                } else {
                    newValue = "?"; // Not labeled!
                }
            }
        }
        return super.getListCellRendererComponent(list, newValue, index,
                isSelected, cellHasFocus);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellRendererComponent(final JTable table,
            final Object value, final boolean isSelected,
            final boolean hasFocus, final int row, final int column) {
        Object newValue = value;

        if (value instanceof String) {
            if (value.equals(ClassModel.NO_CLASS)) {
                if (m_defaultClassModel.isActive()) {
                    newValue = m_defaultClassModel.getStringValue();
                } else {
                    newValue = "?"; // Not labeled!
                }
            }

            final JLabel label = new JLabel((String) newValue);

            if (isSelected) {
                label.setForeground(table.getSelectionForeground());
                label.setBackground(table.getSelectionBackground());
                label.setOpaque(true);
            }

            if (hasFocus) {
                label.setBorder(table.getBorder());
            }

            return label;
        }

        return new JLabel("non string");
    }

}
