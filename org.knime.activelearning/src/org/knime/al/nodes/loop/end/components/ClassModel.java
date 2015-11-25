package org.knime.al.nodes.loop.end.components;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * ClassModel
 *
 * Manages classes in ActiveLearnLoopEndNodeModel
 *
 * @author Jonathan Hale
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 *
 */
public class ClassModel implements ListModel<String> {

    /**
     * Constant which defines the placeholder for unlabeled rows
     */
    public static final String NO_CLASS = " ";

    private final List<String> m_defClasses; // backbone of this class
    private final List<ListDataListener> m_listeners;

    public ClassModel() {
        m_defClasses = new ArrayList<>();
        m_defClasses.add(NO_CLASS);

        m_listeners = new ArrayList<>();
    }

    /**
     * Add a new class to the collection of defined classes
     *
     * @param classValue
     *            class to be added to the model
     * @return true if class is new, false otherwise
     */
    public boolean addClass(final String classValue) {
        if (!m_defClasses.contains(classValue)) {
            m_defClasses.add(classValue);

            dispatchListDataEvent(new ListDataEvent(this,
                    ListDataEvent.CONTENTS_CHANGED, 0, 0));
            return true;
        }
        return false;
    }

    /**
     * Removes a given class from the model
     *
     * @param classLabel
     *            class to be removed
     * @return true, if class existed
     */
    public boolean removeClass(final String classLabel) {
        final boolean existed = m_defClasses.remove(classLabel);

        dispatchListDataEvent(
                new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, 0));

        return existed;
    }

    /**
     * Get the element at index
     *
     * @param index
     *            position of the class in the models list
     * @return the element at index
     */
    public String getClassAt(final int index) {
        return m_defClasses.get(index);
    }

    /**
     * Remove class at index
     *
     * @param index
     *            position of the class to be removed in the models
     * @return the removed element
     */
    public String removeClassAt(final int index) {
        final String removed = m_defClasses.remove(index);

        dispatchListDataEvent(new ListDataEvent(this,
                ListDataEvent.INTERVAL_REMOVED, index, index));

        return removed;
    }

    /**
     * Add class at index
     *
     * @param classLabel
     *            class to be added to the model
     * @param index
     *            position to insert the class into
     */
    public void addClassAt(final String classLabel, final int index) {
        m_defClasses.add(index, classLabel);
        dispatchListDataEvent(new ListDataEvent(this,
                ListDataEvent.INTERVAL_ADDED, index, index));
    }

    /**
     * Returns a Set of all defined classes in the ClassModel.
     *
     * @return Set containing all defined classes
     */
    public List<String> getDefinedClasses() {
        return m_defClasses;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getElementAt(final int index) {
        return getClassAt(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSize() {
        return m_defClasses.size();
    }

    /**
     * Check if the ClassModel contains the given class.
     *
     * @param classLabel
     *            class to check for
     * @return true if class exists in the model
     */
    public boolean containsClass(final String classLabel) {
        return m_defClasses.contains(classLabel);
    }

    // + + + + + + + + + +//
    // + Event Listening +//
    // + + + + + + + + + +//

    /**
     * {@inheritDoc}
     */
    @Override
    public void addListDataListener(final ListDataListener l) {
        m_listeners.add(l);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeListDataListener(final ListDataListener listener) {
        m_listeners.remove(listener);
    }

    /*
     * Helper function to fire ListDataEvents to the listeners
     */
    private void dispatchListDataEvent(final ListDataEvent e) {
        for (final ListDataListener l : m_listeners) {
            if (e.getType() == ListDataEvent.INTERVAL_ADDED) {
                l.intervalAdded(e);
            } else if (e.getType() == ListDataEvent.INTERVAL_REMOVED) {
                l.intervalRemoved(e);
            } else { // CONTENTS_CHANGED
                l.contentsChanged(e);
            }
        }
    }
}
