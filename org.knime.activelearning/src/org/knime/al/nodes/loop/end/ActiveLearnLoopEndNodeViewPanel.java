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
 * Created on 15.03.2013 by Jonathan Hale
 */
package org.knime.al.nodes.loop.end;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.knime.al.nodes.loop.end.components.ButtonList;

/**
 * ActiveLearnLoopEndNodeViewPanel
 *
 * Used in the ActiveLearnLoopEndNodeView, when execution suspended and user
 * interaction is required. Holds all the components of the GUI of
 * ActiveLearnLoopEndNodeView.
 *
 * @author Jonathan Hale
 */
public class ActiveLearnLoopEndNodeViewPanel extends JPanel {

    /**
     *
     */
    private static final long serialVersionUID = 6559002619357085432L;

    // Event Listener
    ActiveLearnLoopEndNodeViewListener m_eventListener;

    // GUI Components

    // Buttons
    /**
     * "Continue" Button
     */
    final JButton m_continueButton = new JButton("Continue");

    /**
     * "Terminate" Button
     */
    final JButton m_terminateButton = new JButton("Terminate");

    /**
     * "Add Class" Buttons
     */
    final JButton m_addClassButton = new JButton("Add Class"); // for the
    // Table
    // Selection
    // Panel
    final JButton m_addClassButtonWiz = new JButton("Add Class"); // for
    // the
    // Wizard
    // Panel
    final JButton m_addClassButtonMng = new JButton("Add Class"); // for
    // Class
    // Manager
    // Panel

    /**
     * "Remove Class" Button
     */
    public final JButton m_remClassButton = new JButton("Remove Class");

    /**
     * ComboBox which holds a list of existing classes to select from
     */
    public final JComboBox<String> m_classBox = new JComboBox<String>();

    /**
     * Table holding the row data
     */
    public final JTable m_hiliteTable = new JTable();

    /**
     * Wizard ToggleButtonList
     */
    public final ButtonList m_classBtnList = new ButtonList(10);

    /**
     * Class List List of all classes in the Class Manager Panel
     */
    public final JList<String> m_classList = new JList<String>();

    /**
     * Fields in which a new class can be entered
     */
    public final JTextField m_classField = new JTextField(14);
    public final JTextField m_classFieldWiz = new JTextField(14);
    public final JTextField m_classFieldMng = new JTextField(14);

    /**
     * Label which tells the user about how may unlabeled rows are left, and
     * which row the user is currently in. (In Wizard Panel)
     */
    public final JLabel m_rowStatsWiz = new JLabel();

    /**
     * Label which tells the user about which iteration he is currently in
     */
    public final JLabel m_iterLbl = new JLabel();

    /**
     * Notification Label to tell the user when he is done (In Wizard Panel)
     */
    public final JLabel m_labelingDone = new JLabel("(all rows labeled)");

    /**
     * the detailed view (for an hilited element)
     */
    public JPanel m_detailedView = new JPanel();

    /**
     * The Tabbed Pane for the different classifying Panels
     */
    public JTabbedPane m_tabbedPane = new JTabbedPane();

    /**
     * Auto Continue Check Box
     */
    public JCheckBox m_autoContCheckBox = new JCheckBox("Enable Auto Continue");

    /**
     * Constructor Sets up the gui components and layouts them.
     */
    public ActiveLearnLoopEndNodeViewPanel() {
        super();

        /*----------------------*
         * Layouting everything *
         *----------------------*/
        setPreferredSize(new Dimension(500, 600));

        setLayout(new GridBagLayout());
        // I read somwhere you shouldn't reuse GridBagConstraints
        // "to avoid subtle bugs", so:
        setLayout();
    }

    private void setLayout() {
        final GridBagConstraints gbc_iterLbl = new GridBagConstraints();
        final GridBagConstraints gbc_detView = new GridBagConstraints(); // for
        // detailed
        // View
        final GridBagConstraints gbc_tabs = new GridBagConstraints(); // for the
        // tabbedPane
        final GridBagConstraints gbc_contBtn = new GridBagConstraints();
        final GridBagConstraints gbc_termBtn = new GridBagConstraints();
        final GridBagConstraints gbc_autCont = new GridBagConstraints();

        gbc_iterLbl.fill = gbc_detView.fill = GridBagConstraints.HORIZONTAL;
        gbc_iterLbl.gridy = 0;
        gbc_iterLbl.weightx = 1.0;
        gbc_iterLbl.gridwidth = 4;

        gbc_detView.gridx = 0;
        gbc_detView.gridy = GridBagConstraints.RELATIVE;
        gbc_detView.gridheight = 2;
        gbc_detView.gridwidth = 4;
        gbc_detView.weighty = 1.0;
        gbc_detView.ipady = 180;

        gbc_tabs.gridx = 0;
        gbc_tabs.fill = GridBagConstraints.BOTH;
        gbc_tabs.gridy = GridBagConstraints.RELATIVE;
        gbc_tabs.gridwidth = 4;
        gbc_tabs.weighty = .9;
        gbc_tabs.weightx = 1.0;

        gbc_contBtn.anchor = gbc_termBtn.anchor = GridBagConstraints.WEST;
        gbc_contBtn.weighty = gbc_termBtn.weighty = 0.0;

        gbc_contBtn.gridx = 0;
        gbc_contBtn.gridy = GridBagConstraints.RELATIVE;
        gbc_contBtn.gridwidth = 1;
        gbc_contBtn.weightx = 0.0;

        gbc_termBtn.gridx = 1;
        gbc_termBtn.gridy = GridBagConstraints.RELATIVE;
        gbc_termBtn.gridwidth = 1;
        gbc_termBtn.weightx = 1.0;

        gbc_autCont.gridx = 3;
        gbc_autCont.anchor = GridBagConstraints.EAST;
        gbc_autCont.gridy = GridBagConstraints.RELATIVE;
        m_autoContCheckBox
                .setToolTipText("Automaticaly continue, when there are \n"
                        + "no unlabeled rows left in this iteration.");

        // add the Iteration Label and the Detailed view at the very top
        add(m_iterLbl, gbc_iterLbl);
        add(m_detailedView, gbc_detView);

        // SELECTION PANEL

        final JPanel selPanelTab = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc_classFld = new GridBagConstraints();
        final GridBagConstraints gbc_classBtn = new GridBagConstraints();
        final GridBagConstraints gbc_classBox = new GridBagConstraints();
        final GridBagConstraints gbc_editPanel = new GridBagConstraints();

        gbc_classFld.fill = gbc_classBtn.fill =
                gbc_classBox.fill = GridBagConstraints.HORIZONTAL;
        gbc_classFld.gridx = 0;
        gbc_classFld.gridy = 0;
        gbc_classFld.weightx = 1.0;
        gbc_classFld.ipadx = 50;

        gbc_classBtn.gridx = 1;
        gbc_classBtn.gridy = 0;
        gbc_classBtn.weightx = 0.5;

        gbc_classBox.gridx = 2;
        gbc_classBox.gridy = 0;
        gbc_classBox.weightx = 1.0;
        gbc_classBox.ipadx = 100;

        gbc_editPanel.fill = GridBagConstraints.BOTH;
        gbc_editPanel.gridx = 0;
        gbc_editPanel.gridy = 1;
        gbc_editPanel.gridwidth = 3;
        gbc_editPanel.weighty = 1.0;
        gbc_editPanel.weightx = 1.0;

        selPanelTab.add(m_classField, gbc_classFld);
        selPanelTab.add(m_addClassButton, gbc_classBtn);
        selPanelTab.add(m_classBox, gbc_classBox);

        // hilite Table
        final JScrollPane tableScrollPane = new JScrollPane(m_hiliteTable);
        m_hiliteTable.setCellSelectionEnabled(false);
        m_hiliteTable.setColumnSelectionAllowed(false);
        m_hiliteTable.setRowSelectionAllowed(true);

        // no row header, decided an extra column looks better
        selPanelTab.add(tableScrollPane, gbc_editPanel);

        // BUTTON PANEL
        final JPanel btnPanelTab = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc_rowStats = new GridBagConstraints();
        final GridBagConstraints gbc_lblDone = new GridBagConstraints();
        final GridBagConstraints gbc_clsList = new GridBagConstraints();
        final GridBagConstraints gbc_clsFld = new GridBagConstraints();
        final GridBagConstraints gbc_clsBtn = new GridBagConstraints();

        gbc_rowStats.anchor = gbc_clsList.anchor =
                gbc_lblDone.anchor = GridBagConstraints.LINE_START;
        gbc_rowStats.gridy = 0;
        gbc_rowStats.anchor = GridBagConstraints.CENTER;

        gbc_lblDone.fill = GridBagConstraints.HORIZONTAL;
        gbc_lblDone.gridy = 1;

        gbc_clsList.fill = GridBagConstraints.BOTH;
        gbc_clsList.gridy = 2;
        gbc_clsList.insets = new Insets(1, 0, 10, 0);
        gbc_clsList.weighty = 1.0;

        gbc_clsFld.fill = gbc_clsBtn.fill = GridBagConstraints.NONE;
        gbc_clsFld.gridy = 3;
        gbc_clsFld.ipadx = 60;
        gbc_clsBtn.gridy = 4;
        gbc_clsBtn.ipadx = 100; // the above ipadx value and this one are set to
        // make both equally large

        m_labelingDone.setForeground(new Color(65, 128, 64));
        m_labelingDone.setHorizontalAlignment(SwingConstants.CENTER);
        m_labelingDone.setVisible(false);

        final JPanel placeHolderPanel = new JPanel(new GridLayout(1, 1));
        placeHolderPanel.add(m_labelingDone);
        placeHolderPanel.setPreferredSize(m_labelingDone.getPreferredSize());

        final JPanel buttonListPanel = new JPanel(new GridLayout(1, 1));
        buttonListPanel.add(m_classBtnList);
        buttonListPanel.setPreferredSize(m_classBtnList.getPreferredSize());

        btnPanelTab.add(m_rowStatsWiz, gbc_rowStats);
        btnPanelTab.add(placeHolderPanel, gbc_lblDone);
        btnPanelTab.add(buttonListPanel, gbc_clsList);
        btnPanelTab.add(m_classFieldWiz, gbc_clsFld);
        btnPanelTab.add(m_addClassButtonWiz, gbc_clsBtn);

        // MANAGE CLASSES PANEL
        final JPanel clsPanelTab = new JPanel();

        clsPanelTab.setLayout(new GridBagLayout());

        final Insets defInsets = new Insets(5, 2, 5, 2);
        final GridBagConstraints gbc_classList = new GridBagConstraints(0, 0, 1,
                5, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                GridBagConstraints.BOTH, new Insets(0, 0, 0, 5), 0, 0);
        final GridBagConstraints gbc_classField = new GridBagConstraints(1, 1,
                1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, defInsets, 0, 0);
        final GridBagConstraints gbc_addClassBtn = new GridBagConstraints(2, 1,
                1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, defInsets, 0, 0);
        final GridBagConstraints gbc_remClassBtn = new GridBagConstraints(1, 3,
                2, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, defInsets, 0, 0);

        final JScrollPane listScrollPane = new JScrollPane(m_classList);

        clsPanelTab.add(m_addClassButtonMng, gbc_addClassBtn);
        clsPanelTab.add(listScrollPane, gbc_classList);
        clsPanelTab.add(m_classFieldMng, gbc_classField);

        clsPanelTab.add(m_remClassButton, gbc_remClassBtn);

        m_tabbedPane.addTab("Table Selection", selPanelTab);
        m_tabbedPane.addTab("Wizard", btnPanelTab);
        m_tabbedPane.addTab("Manage Classes", clsPanelTab);

        add(m_tabbedPane, gbc_tabs);

        add(m_continueButton, gbc_contBtn);
        add(m_terminateButton, gbc_termBtn);
    }

    /**
     * Set the ActiveLearnLoopEndNodeViewListener to listen to the gui
     * components events
     *
     * @param listener
     */
    public void addListener(final ActiveLearnLoopEndNodeViewListener listener) {
        // set the event listener member
        m_eventListener = listener;

        // add the listener to the gui-components
        m_hiliteTable.getSelectionModel()
                .addListSelectionListener(m_eventListener);
        m_classList.getSelectionModel()
                .addListSelectionListener(m_eventListener);

        m_classBox.addItemListener(new ItemListener() {

            /**
             * {@inheritDoc}
             */
            @Override
            public void itemStateChanged(final ItemEvent e) {

                if (e.getStateChange() == ItemEvent.SELECTED) {
                    // if we change something here and something is selected
                    // (and therefore visible in detail view), the class label
                    // of this rowkey will also be changed

                    if (m_hiliteTable.getSelectedRowCount() > 0) {
                        final int[] selectedRows =
                                m_hiliteTable.getSelectedRows();
                        final Object theClass = m_classBox.getSelectedItem();

                        for (final int row : selectedRows) {
                            // Next line sets the value of the class column
                            // <+1>, because the first column is for rowID and
                            // is not included in getClassColumnIndex()
                            m_eventListener.setClass(row, (String) theClass);
                        }

                        if (m_eventListener.m_classCellEditor != null) {
                            m_eventListener.m_classCellEditor
                                    .cancelCellEditing();
                            // cancel Cell Editing to show new values
                        }
                    }
                }

            }
        });

        m_continueButton.addActionListener(m_eventListener);
        m_continueButton.setActionCommand("continue");

        m_terminateButton.addActionListener(m_eventListener);
        m_terminateButton.setActionCommand("terminate");

        m_addClassButton.addActionListener(m_eventListener);
        m_addClassButtonWiz.addActionListener(m_eventListener);
        m_addClassButtonMng.addActionListener(m_eventListener);

        m_addClassButton.setActionCommand("addClass");
        m_addClassButtonWiz.setActionCommand("addClass");
        m_addClassButtonMng.setActionCommand("addClass");

        m_classField.addKeyListener(m_eventListener);
        m_classFieldWiz.addKeyListener(m_eventListener);
        m_classFieldMng.addKeyListener(m_eventListener);

        // The Wizards Button List
        m_classBtnList.addActionListener(m_eventListener);
        m_classBtnList.setActionCommand("setClass");

        // Stuff for the class manager
        m_remClassButton.addActionListener(m_eventListener);
        m_remClassButton.setActionCommand("removeClass");

        m_tabbedPane.addChangeListener(m_eventListener);

        m_eventListener.setGUI(this);
    }

    /**
     * removes the EventListener from gui and resets its reference to this gui
     * instance
     */
    public void removeListener() {
        m_eventListener.setGUI(null);

        m_hiliteTable.getSelectionModel()
                .removeListSelectionListener(m_eventListener);
        m_classList.getSelectionModel()
                .removeListSelectionListener(m_eventListener);

        // m_classBox.removeItemListener(m_eventListener);

        m_continueButton.removeActionListener(m_eventListener);

        m_terminateButton.removeActionListener(m_eventListener);

        m_addClassButton.removeActionListener(m_eventListener);
        m_addClassButtonWiz.removeActionListener(m_eventListener);
        m_addClassButtonMng.removeActionListener(m_eventListener);
        m_classField.removeKeyListener(m_eventListener);
        m_classFieldWiz.removeKeyListener(m_eventListener);
        m_classFieldMng.removeKeyListener(m_eventListener);

        m_classBtnList.removeActionListener(m_eventListener);

        m_remClassButton.removeActionListener(m_eventListener);

        m_tabbedPane.removeChangeListener(m_eventListener);

        m_eventListener = null;
    }

    /**
     * Gets the contents of the first unempty classField
     *
     * @return the text of the first non empty classField
     */
    public String getClassFieldText() {
        if (!m_classField.getText().isEmpty()) {
            return m_classField.getText();
        } else if (!m_classFieldWiz.getText().isEmpty()) {
            return m_classFieldWiz.getText();
        } else {
            return m_classFieldMng.getText();
        }
    }

    /**
     * Clear the class fields in all panels
     */
    public void clearClassFields() {
        m_classField.setText("");
        m_classFieldWiz.setText("");
        m_classFieldMng.setText("");
    }

    /**
     * Check whether a Component/Object is one of the class fields
     *
     * @param component
     * @return true if the parameter is a class field
     */
    public boolean isClassField(final Object component) {
        return ((component == m_classField) || (component == m_classFieldWiz)
                || (component == m_classFieldMng));
    }
}
