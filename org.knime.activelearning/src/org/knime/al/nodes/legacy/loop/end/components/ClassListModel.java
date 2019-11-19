package org.knime.al.nodes.legacy.loop.end.components;

import javax.swing.MutableComboBoxModel;
import javax.swing.event.ListDataListener;

/**
 * ClassListModel
 *
 * ListModel/ComboBoxModel for all GUI components (JComboBox, JList) that are
 * supposed to contain defined classes held in an ClassModel.
 *
 * @author Jonathan Hale
 */
public class ClassListModel implements MutableComboBoxModel<String> {

    private Object m_selectedItem;
    private final ClassModel m_classModel;

    /**
     * Contructor Initializes ClassListModel with a ClassModel. This sets the
     * contents of the ClassListModel exactly to the contents of the ClassModel
     * and applies changes automatically.
     *
     * @param classModel
     */
    public ClassListModel(final ClassModel classModel) {
        m_classModel = classModel;

        m_selectedItem = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addListDataListener(final ListDataListener listener) {
        m_classModel.addListDataListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getSelectedItem() {
        return m_selectedItem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSelectedItem(final Object anItem) {
        m_selectedItem = anItem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getElementAt(final int index) {
        return m_classModel.getElementAt(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSize() {
        return m_classModel.getSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeListDataListener(final ListDataListener l) {
        m_classModel.removeListDataListener(l);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addElement(final String item) {
        m_classModel.addClass(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insertElementAt(final String item, final int index) {
        m_classModel.addClassAt(item, index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeElement(final Object obj) {
        if (!(obj instanceof String)) {
            m_classModel.removeClass((String) obj);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeElementAt(final int index) {
        m_classModel.removeClassAt(index);
    }

}