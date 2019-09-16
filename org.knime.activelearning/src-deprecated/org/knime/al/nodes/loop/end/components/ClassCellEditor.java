package org.knime.al.nodes.loop.end.components;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

/**
 * ClassBoxCellEditor
 *
 * Custom cell editor for the class column of the hilite table. Simply a
 * comboBox cell editor with all the classes of a ClassModel and a
 * ClassCellRenderer as cell renderer.
 *
 * @author Jonathan Hale
 * @deprecated Retired in favor of new active learning loop based on standard recursive loop.
 *
 */
@Deprecated
public class ClassCellEditor extends AbstractCellEditor
        implements TableCellEditor, ItemListener {

    /**
     *
     */
    private static final long serialVersionUID = -2279460059604912165L;

    private JComboBox<String> m_comboBox = null; // the editors component
    private ClassListModel m_classListModel = null;
    private Object m_selectedItem = null; // last value of the editor

    /**
     * Contructor
     *
     * Initialized ClassBoxCellEditor with a ClassModel. This basically sets the
     * ComboBox to contain the items of the ClassModel at any point in time with
     * all of the changes.
     *
     * @param classModel
     *            the contests of the editor
     */
    public ClassCellEditor(final ClassModel classModel) {
        super();

        m_classListModel = new ClassListModel(classModel);
        m_comboBox = new JComboBox<String>(m_classListModel);
        m_comboBox.addItemListener(this);
        m_comboBox.setRenderer(new ClassCellRenderer());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellEditorComponent(final JTable table,
            final Object value, final boolean isSelected, final int row,
            final int col) {
        m_classListModel.setSelectedItem(value);
        m_comboBox.setSelectedItem(value);
        m_selectedItem = value;

        return m_comboBox;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getCellEditorValue() {
        return m_selectedItem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void itemStateChanged(final ItemEvent e) {
        if (e.getSource() == m_comboBox) {
            m_selectedItem = m_classListModel.getSelectedItem();
            fireEditingStopped();
        }

    }
}
