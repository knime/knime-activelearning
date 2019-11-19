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
 * Created on 15.03.2013 by The Master
 */
package org.knime.al.nodes.legacy.loop.end;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import org.knime.al.nodes.legacy.loop.ActiveLearnLoopUtils.NodeModelState;
import org.knime.al.nodes.legacy.loop.end.components.ClassCellEditor;
import org.knime.al.nodes.legacy.loop.end.components.ClassCellRenderer;
import org.knime.al.nodes.legacy.loop.end.components.ClassListModel;
import org.knime.al.nodes.legacy.loop.end.components.ClassModel;
import org.knime.core.data.RowKey;
import org.knime.core.node.defaultnodesettings.SettingsModelOptionalString;

/**
 * ActiveLearnLoopEndNodeViewListener
 *
 * Event listener for the GUI of the ActiveLearnLoopEndNodeView and hilite
 * Events.
 *
 * Contains complete functionality of the GUI (ActiveLearnLoopEndNodeViewPanel).
 *
 * SOME NOTES:
 *
 * > setting the class of a hilite Is done by HilitetableModel (setValueAt),
 * since it is the most efficient way to update the table. This method is
 * currently only called by the itemEvent of m_classBox.
 *
 * > iterations Are prepared in constructor and in prepare() which is called
 * before the interactive gui is shown. Are finalized when continue/terminate
 * button is pressed in actionPerformed()
 *
 * > classBoxCellEditor A custom cell editor, which provided with a class list
 * edits a class cell directly in the table.
 *
 * @author Jonathan Hale
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */

final class ActiveLearnLoopEndNodeViewListener implements ActionListener,
        ListSelectionListener, KeyListener, ChangeListener {

    // reference to the GUI components
    private ActiveLearnLoopEndNodeViewPanel m_gui;
    // Corresponding model
    private final ActiveLearnLoopEndNodeModel m_nodeModel;

    // Map holding a class label (defined by user) for each hilited rowkey.
    // default is DEFAULT_CLASS
    Map<RowKey, String> m_classMap;

    private ClassCellEditor m_classCellEditor; // cell editor for the class
    // column in the hilite table

    private ClassViewerTable m_classViewerTable;

    private ClassCellRenderer m_classCellRenderer;
    private final SettingsModelOptionalString m_defaultClassModel =
            ActiveLearnLoopEndSettingsModels.createDefaultClassModel();

    private int m_curRow; // current Row being edited by QuickButton Panel.

    /**
     * Constructor
     *
     * @param nodeModel
     *            node model of the node which the gui belongs to
     */
    public ActiveLearnLoopEndNodeViewListener(
            final ActiveLearnLoopEndNodeModel nodeModel) {
        m_nodeModel = nodeModel;
        prepareNextIteration();
    }

    /**
     * Set ActiveLearnLoopEndNodeViewPanel to listen to. Should only be done by
     * the ActiveLearnLoopEndNodeViewPanel
     *
     * @param gui
     *            reference to the gui (ActiveLearnLoopEndNodeViewPanel)
     */
    public void setGUI(final ActiveLearnLoopEndNodeViewPanel gui) {
        m_gui = gui;

        if (m_gui == null) {
            return;
        }

        // setCellRenderer for ComboBoxes:
        m_classCellRenderer = new ClassCellRenderer();
        m_gui.m_classBox.setRenderer(m_classCellRenderer);
        m_gui.m_classList.setCellRenderer(m_classCellRenderer);

        m_classViewerTable = new ClassViewerTable(this);
        m_gui.m_hiliteTable.setModel(m_classViewerTable);

        m_gui.m_classBtnList.setClassModel(m_nodeModel.getClassModel());

        m_gui.m_classBox
                .setModel(new ClassListModel(m_nodeModel.getClassModel()));
        m_gui.m_classList
                .setModel(new ClassListModel(m_nodeModel.getClassModel()));

        m_classCellEditor = new ClassCellEditor(m_nodeModel.getClassModel());

        updateGUI();
    }

    ClassCellEditor getClassCellEditor() {
        return m_classCellEditor;
    }

    /**
     * Prepares GUI and Listener to be shown to the user.
     */
    public void prepare() {
        prepareNextIteration();

        updateGUI();
    }

    /**
     * Set the class of a row
     *
     * @param rowkey
     *            the rowkey
     * @param classValue
     */
    public void setClass(final RowKey rowkey, final String classValue) {
        m_classMap.put(rowkey, classValue);

        // if (getAllRowsLabeled()) {
        notifyAllRowsLabled();
        // } else {
        // notifyRowsStillUnlabeled();
        // }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void valueChanged(final ListSelectionEvent e) {

        if (m_gui == null) {
            return; // If this happens after the view was closed once or more,
            // the memleak is still there! (shouldn't happen though)
        }
        final Object source = e.getSource();

        if ((source == m_gui.m_hiliteTable.getSelectionModel())
                && e.getValueIsAdjusting()) { // table row Selection Event

            // If nothing is selected we'll try to select first row in table
            if (m_gui.m_hiliteTable.getSelectedRow() < 0) {
                m_gui.m_hiliteTable.changeSelection(0, 0, false, false);
            }

            // if something was selected, we can update detailed view and
            // classBox
            if (m_gui.m_hiliteTable.getSelectedRow() > -1) {
                final RowKey selected = getSelectedRowKey();
                updateDetailedView(selected); // update detailed view with
                // selected RowKey

                if (m_gui.m_hiliteTable.getSelectedRowCount() > 1) {
                    m_gui.m_classBox.setSelectedItem(null);
                    m_gui.m_classBox.repaint();
                } else {
                    // set class box value to class of now selected row
                    m_gui.m_classBox.setSelectedItem(m_classMap.get(selected));
                    m_gui.m_classBox.repaint();
                }
            }
            return;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        final String command = e.getActionCommand();

        // continueButton
        if (command.equals("continue")) {
            if (getAllRowsLabeled() || m_defaultClassModel.isActive()) {
                // Cleanup and stop
                finishAndContinue();
            } else {
                final int ret = JOptionPane.showConfirmDialog(m_gui,
                        "Not all rows are labeled, are you sure you want to continue? \n"
                                + "Continuing will cause these rows to be added back to the unlabeled pool.",
                        "Continue?", JOptionPane.CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                if ((ret == JOptionPane.CANCEL_OPTION)
                        || (ret == JOptionPane.CLOSED_OPTION)) {
                    return;
                }
                finishAndContinue();
            }
            return;
        }

        // addClassButton
        if (command.equals("addClass")) {
            actionUserAddedClass();

            return;
        }

        // terminateButton
        if (command.equals("terminate")) {
            if (getAllRowsLabeled() || m_defaultClassModel.isActive()) {
                // Cleanup and stop
                finishCurrentIteration();
            } else {
                // Launch a Dialog
                final int ret = JOptionPane.showConfirmDialog(m_gui,
                        "Terminating will cause current changes\n"
                                + "of hilite labels to be discarded!\n",
                        "Terminating...", JOptionPane.CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                if ((ret == JOptionPane.CANCEL_OPTION)
                        || (ret == JOptionPane.CLOSED_OPTION)) {
                    return;
                }
            }

            m_nodeModel.terminate();
            return;
        }

        // ------ToggleButtonList Butttons ------ //
        if (command.equals("setClass")) {
            setClass(m_curRow, m_gui.m_classBtnList.getSelectedItem());

            // If we're through all the rows, start from beginning
            RowKey curKey = null;

            while (m_curRow < m_classViewerTable.getRowCount()) {
                curKey = m_classViewerTable.getRowKeyOf(m_curRow);
                if (m_classMap.get(curKey).equals(ClassModel.NO_CLASS)) {
                    break;
                }
                m_curRow++;
            }

            //
            if (m_curRow >= m_classViewerTable.getRowCount()) {
                if (!getAllRowsLabeled()) {
                    if (m_gui.m_autoContCheckBox.isSelected()
                            && !m_defaultClassModel.isActive()) {
                        final int ret = JOptionPane.showConfirmDialog(m_gui,
                                "You are at the end of the list,\n"
                                        + "but there are still rows unlabeled.\n"
                                        + "Do you want to continue anyway?\n",
                                "Continue with unlabeled?",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE);

                        if (ret == JOptionPane.YES_OPTION) {
                            finishAndContinue();
                        }
                    }
                    m_curRow = getFirstUnlabeledRow(); // start from first
                    // unlabeled
                    updateDetailedView(
                            m_classViewerTable.getRowKeyOf(m_curRow));

                }else {
                    finishAndContinue();
                }
            } else {
                // update view with new row.
                updateDetailedView(curKey);
            }
            updateRowStatsLabel(curKey);
        }

        if (command.equals("removeClass")) {
            if (m_gui.m_tabbedPane.getSelectedIndex() == CLASS_PANEL) {
                final String defName = (m_defaultClassModel.isActive())
                        ? m_defaultClassModel.getStringValue() : "?";

                final int selClassIndex = m_gui.m_classList.getSelectedIndex();
                final String selClass = m_gui.m_classList.getSelectedValue();

                if ((selClass == null) || (selClassIndex == -1)) {
                    JOptionPane.showMessageDialog(m_gui,
                            "No class selected. Please\nselect a class first.",
                            "Error removing class.", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (selClass.equals(ClassModel.NO_CLASS)) {
                    JOptionPane.showMessageDialog(m_gui,
                            "You cannot remove the default class!",
                            "Cannot remove default.",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                final int nRows = getNumRowsWithClass(selClass);
                // Launch a Dialog
                final int ret = JOptionPane.showConfirmDialog(m_gui,
                        "Removing this class will reset " + nRows + "\n"
                                + "rows back to \"" + defName + "\"\n",
                        "Removing class...", JOptionPane.CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                if ((ret == JOptionPane.CANCEL_OPTION)
                        || (ret == JOptionPane.CLOSED_OPTION)) {
                    return;
                }

                resetAllWithClass(selClass);
                m_nodeModel.getClassModel().removeClassAt(selClassIndex);
            }
        }

        return;
    }

    /*
     * Finish current Itertation and continue executing
     */
    private void finishAndContinue() {
        // Next Iteration is about to be processed. Finish current iteration
        // and prepare the next one
        finishCurrentIteration();

        // prepares next iteration as well
        m_nodeModel.continueExecution();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void keyReleased(final KeyEvent e) {
        final Object source = e.getSource();

        if (m_gui.isClassField(source)) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                actionUserAddedClass();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void keyTyped(final KeyEvent arg0) {
        // Nothing to do here
        return;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void keyPressed(final KeyEvent arg0) {
        // nothing to do here
        return;
    }

    // Some constants for the tabbedPane indices
    private final int TABLE_PANEL = 0;
    private final int WIZARD_PANEL = 1;
    private final int CLASS_PANEL = 2;

    // The lastly selected Panel
    private int m_lastPanel = TABLE_PANEL;

    /**
     * {@inheritDoc}
     */
    @Override
    public void stateChanged(final ChangeEvent e) {
        if (e.getSource() == m_gui.m_tabbedPane) {
            // Do stuff on panel change
            m_gui.clearClassFields();

            final int selectedPanel = m_gui.m_tabbedPane.getSelectedIndex();
            if (selectedPanel == TABLE_PANEL) {
                if (m_lastPanel == WIZARD_PANEL) {
                    // set selection of table to m_curRow
                    m_gui.m_hiliteTable.changeSelection(m_curRow, m_curRow,
                            false, false);
                }
                if (m_lastPanel == CLASS_PANEL) {
                    // select first Item
                    m_gui.m_hiliteTable.changeSelection(0, 0, false, false);
                }
            } else if (selectedPanel == WIZARD_PANEL) {
                /* Only change current row if there are unlabeled rows left */
                if (!getAllRowsLabeled()) {
                    // set current row to first unlabeled row
                    m_curRow = getFirstUnlabeledRow();
                }

                // update view with m_curRow
                updateDetailedView(m_classViewerTable.getRowKeyOf(m_curRow));
                updateRowStatsLabel(m_classViewerTable.getRowKeyOf(m_curRow));
            } else if (selectedPanel == CLASS_PANEL) {
                m_gui.m_hiliteTable.clearSelection();
            }

            m_lastPanel = selectedPanel; // remember last selected Panel
        }

    }

    private int getFirstUnlabeledRow() {
        String lastClass = null;
        int i = 0;
        for (; m_curRow < m_classMap.size(); i++) {
            lastClass = m_classMap.get(m_classViewerTable.getRowKeyOf(i));
            // we have to do that like this over
            // m_hiliteTableMode, because maps are not sorted
            if (lastClass.equals(ClassModel.NO_CLASS)) {
                break; // this is the first unlabeled row, break and keep value
                // for return
            }
        }

        return i;
    }

    /*
     * Prepare a new Iteration
     */
    private void prepareNextIteration() {
        m_classMap = new HashMap<RowKey, String>(); // make a new hiliteMap

        // Add the hilites to our hiliteMap
        for (final RowKey key : m_nodeModel.getRowMap().keySet()) {
            m_classMap.put(key, ClassModel.NO_CLASS);
            // add all the keys, but without an value
        }
        m_curRow = 0;
    }

    /*
     * Finish/Cleanup current Iteration and pass results to the nodeModel
     */
    private void finishCurrentIteration() {
        if (!getAllRowsLabeled()) {
            if (m_defaultClassModel.isActive()) {
                // replace all NO_CLASS with default class
                for (final RowKey key : m_classMap.keySet()) {
                    if (m_classMap.get(key).equals(ClassModel.NO_CLASS)) {
                        m_classMap.put(key,
                                m_defaultClassModel.getStringValue());
                    }
                }
            } else {

                // add all rows labeled with NO_CLASS to toBeRemoved list
                final List<RowKey> toBeRemoved = new ArrayList<RowKey>();
                for (final RowKey key : m_classMap.keySet()) {
                    if (m_classMap.get(key).equals(ClassModel.NO_CLASS)) {
                        toBeRemoved.add(key);
                    }
                }

                for (final RowKey remove : toBeRemoved) {
                    m_classMap.remove(remove);
                }
            }
        }
        m_nodeModel.setClassMap(m_classMap); // Pass the hiliteMap onto the
        // nodeModel

        m_classViewerTable.clearEntries();
        updateDetailedView(null);
    }

    /*
     * Updates GUI with new values
     */
    private void updateGUI() {
        if (m_gui == null) {
            return;
        }

        if (m_nodeModel.getNodeState() != NodeModelState.SUSPENDED) {
            return; // only update GUI while in SUSPENDED state
        }

        if (m_classMap.size() > 0) {
            if (m_classMap.containsValue(ClassModel.NO_CLASS)) {
                notifyRowsStillUnlabeled();
            }

            updateViewerTable(); // update the Viewer Table
            updateDetailedView(getSelectedRowKey()); // update with first value
        }

        if (m_defaultClassModel.isActive()) {
            m_gui.m_classBtnList
                    .setDefaultText(m_defaultClassModel.getStringValue());
        } else {
            m_gui.m_classBtnList.setDefaultText("- Skip -");
        }

        final RowKey key = m_classViewerTable.getRowKeyOf(m_curRow);
        if (key != null) {
            updateRowStatsLabel(key);
            m_gui.m_iterLbl.setText("Current Iteration: "
                    + m_nodeModel.getCurrentIterationIndex());
        }
    }

    /*
     * Update the hiliteTable
     */
    private void updateViewerTable() {
        // Set the data in the GUI and select the first entry in the table

        // Tell the node model which column is the class column
        m_classViewerTable.setClassColumn(m_nodeModel.getClassColumnName(),
                m_nodeModel.getClassColumnIndex());

        m_classViewerTable.updateEntries(m_classMap.keySet(),
                m_nodeModel.getRowMap(), m_nodeModel.getColNames());
        m_gui.m_hiliteTable.changeSelection(0, 0, false, false); // Select first
        // entry

        // Set the CellEditor
        final int classColumnIndex = m_classViewerTable.getClassColumnIndex();

        final TableColumn classColumn = m_gui.m_hiliteTable.getColumnModel()
                .getColumn(classColumnIndex); // get the column with
        // that index
        classColumn.setCellEditor(m_classCellEditor);
        classColumn.setCellRenderer(m_classCellRenderer);
    }

    /*
     * Update the detailed view with representative value of the current
     * selection
     */
    private void updateDetailedView(final RowKey selectedValue) {
        m_gui.m_detailedView.removeAll();

        if (selectedValue != null) {

            m_gui.m_detailedView
                    .add(m_nodeModel.requireRenderer(selectedValue));
            m_gui.m_detailedView.repaint();
            m_gui.updateUI();
        }
    }

    /*
     * Update the row stats label of the Wizard Panel with number of unlabeled
     * rows and the currently "selected" row.
     */
    private void updateRowStatsLabel(final RowKey rowKey) {
        int numUnlabeled = 0;

        for (final String cls : m_classMap.values()) {
            if (cls.equals(ClassModel.NO_CLASS)) {
                numUnlabeled++;
            }
        }

        m_gui.m_rowStatsWiz.setText("Number of unlabeled rows: " + numUnlabeled
                + ", current row: " + rowKey.toString());
    }

    /*
     * Returns the in the hiliteTabel currently selcted row as a RowKey.
     */
    private RowKey getSelectedRowKey() {
        return m_classViewerTable
                .getRowKeyOf(m_gui.m_hiliteTable.getSelectedRow());
    }

    /*
     * Notification to the listener that all rows have been labeled; no
     * unlabeled rows are left.
     */
            void notifyAllRowsLabled() {
        if (m_defaultClassModel.isActive()) {
            // means, buttons were deactivated
            m_gui.m_continueButton.setEnabled(true);

            // clear tooltips
            m_gui.m_continueButton.setToolTipText(null);
            m_gui.m_terminateButton.setToolTipText(null);

            m_gui.m_labelingDone.setVisible(true);
            m_gui.m_classBtnList.setVisible(false);
        }

        if (m_gui.m_autoContCheckBox.isSelected()) {
            finishAndContinue();
        }
    }

    /*
     * notifyRowsStillUnlabeled
     *
     * Notify the Listener that there still are unlabeled rows. The Listener
     * will then update the GUI (disable Buttons/ set Tooltips etc).
     */
            void notifyRowsStillUnlabeled() {
        if (m_defaultClassModel.isActive()) {
            return;
        }

        // m_gui.m_continueButton.setEnabled(false);

        m_gui.m_continueButton
                .setToolTipText("Can't continue with unlabled rows.");
        m_gui.m_terminateButton
                .setToolTipText("Terminating will loose current labels!");

        m_gui.m_labelingDone.setVisible(false);
        m_gui.m_classBtnList.setVisible(true);
    }

    /*
     * Set class of a row (index) to cls
     */
            void setClass(final int row, final String cls) {
        m_classViewerTable.setValueAt(cls, row,
                m_nodeModel.getClassColumnIndex() + 1);
    }

    /*
     * Returns number of rows labeled with given class (class).
     */
    private int getNumRowsWithClass(final String classLabel) {
        int numRows = 0;

        for (final String cls : m_classMap.values()) {
            if (cls.equals(classLabel)) {
                numRows++;
            }
        }
        return numRows;
    }

    /*
     * Resets all rows with certain class to ClassModel.NO_CLASS
     */
    private void resetAllWithClass(final String selClass) {
        for (final RowKey row : m_classMap.keySet()) {
            if (m_classMap.get(row).equals(selClass)) {
                setClass(row, ClassModel.NO_CLASS);
            }
        }
    }

    /*
     * To be called when the user clicked on an "Add Class" button.
     */
    private void actionUserAddedClass() {
        // stop showing cell editor, so it refreshes
        final TableCellEditor ce = m_gui.m_hiliteTable.getCellEditor();

        if (ce != null) {
            ce.stopCellEditing();
        }

        final String newClass = m_gui.getClassFieldText();

        if (newClass.isEmpty()) {
            return;
        }

        m_nodeModel.getClassModel().addClass(newClass);

        m_gui.m_classBox.setSelectedIndex(m_gui.m_classBox.getItemCount() - 1);
        // change // the // class // Box // to // show // the // new // item

        m_gui.clearClassFields();

    }

    /*
     * Retruns true if there are no unlabeled rows left.
     */
            boolean getAllRowsLabeled() {
        return !m_classMap.containsValue(ClassModel.NO_CLASS);
    }
}
