package org.knime.al.nodes.loop.end.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * ToggleButtonList
 *
 * A single-selection, no-duplicates list of toggle buttons for quick selection
 * of an item.
 *
 * Used in ActiveLearnLoopEndNodeViewPanel
 *
 * @author Jonathan Hale
 *
 */
public class ToggleButtonList extends JPanel
        implements ActionListener, ListDataListener {

    private static final long serialVersionUID = 987049663025341598L;

    private ClassModel m_classModel;

    private String m_selectedItem;

    private final int m_maxRows;

    private final List<JPanel> m_panels;
    private final List<JToggleButton> m_buttons;
    private JPanel m_lastPanel;

    private String m_defaultText = " ";

    /**
     * Constructor.
     *
     * @param maxRows
     *            maximum number of buttons in a column
     */
    public ToggleButtonList(final int maxRows) {
        super();

        m_maxRows = maxRows;

        m_panels = new ArrayList<>();
        m_buttons = new ArrayList<>();

        setLayout(new GridLayout(1, 3));

        m_classModel = null;
    }

    /**
     * Contructor with ClassModel Initializes ClassModel directly via the
     * constructor.
     *
     * @param classModel
     *            the ClassModel
     * @param maxRows
     *            maximum number of buttons per column
     */
    public ToggleButtonList(final ClassModel classModel, final int maxRows) {
        super();

        m_maxRows = maxRows;
        m_panels = new ArrayList<>();
        m_buttons = new ArrayList<>();

        m_classModel = classModel;

        setLayout(new GridLayout(1, 3));

        recreateButtonList();
    }

    /**
     * get the currently selected item.
     *
     * @return the currently selected item
     */
    public String getSelectedItem() {
        return m_selectedItem;
    }

    /**
     * setSelectedItem - sets the ToggleButtonLists selected button.
     *
     * Iterates through all buttons deselecting the ones whoes getText() doesn't
     * match the itemName given in the parameter. If the Item does not match any
     * in the List, selection won't change.
     *
     * @param itemName
     */
    public void setSelectedItem(final String itemName) {
        // To avoid not having anything selected at the end
        if (!m_classModel.containsClass(itemName)) {
            return;
        }

        m_selectedItem = itemName;

        String sel;
        if (itemName.equals(ClassModel.NO_CLASS)) {
            sel = m_defaultText;
        } else {
            sel = itemName;
        }

        // iterate through all buttons
        for (final JToggleButton btn : m_buttons) {
            // check whether text matches itemName
            if (btn.getText().equals(sel)) {
                btn.setSelected(true); // it is our item, so select it
            } else {
                btn.setSelected(false); // deselect everything else
            }
            revalidate();
        }

    }

    /*
     * Completely recreate button list from scratch
     */
    private void recreateButtonList() {
        if (m_classModel == null) {
            return;
        }

        for (final JPanel panel : m_panels) {
            panel.removeAll();
        }

        for (final JToggleButton btn : m_buttons) {
            btn.removeActionListener(this);
        }

        m_panels.clear();
        m_buttons.clear();

        removeAll();

        // create an initial button for the start.
        for (final String name : m_classModel.getDefinedClasses()) {
            final JToggleButton btn = addButton(name);
            if (name.equals(m_selectedItem)) {
                btn.setSelected(true);
            } else {
                btn.setSelected(false);
            }
        }
    }

    /*
     * Helper function to add buttons
     */
    private JToggleButton addButton(final String value) {
        final int numBtns = m_buttons.size();

        if (numBtns >= (m_maxRows * m_panels.size())) {
            // create a new Panel
            final JPanel panel = new JPanel(new GridLayout(m_maxRows, 1));

            this.add(panel);

            m_panels.add(panel);
            m_lastPanel = panel;
        }
        // createButton and add this as itemListener
        final JToggleButton btn = new CustomToggleButton(value);
        btn.addActionListener(this);

        if (value.equals(ClassModel.NO_CLASS)) {
            btn.setText(m_defaultText);
        }

        m_lastPanel.add(btn); // add it to the last Panel

        m_buttons.add(btn);
        return btn;
    }

    // ACTION HANDLING
    // TODO: (probably the "unofficial" way... is there an Interface for this?)
    List<ActionListener> m_actionListeners = new ArrayList<ActionListener>();
    String m_actionCommand = "action";

    /**
     * Add an ActionListener to the ToggleButtonList
     *
     * @param listener
     */
    public void addActionListener(final ActionListener listener) {
        if (m_actionListeners.contains(listener)) {
            return;
        }

        m_actionListeners.add(listener);
    }

    /**
     * Remove a certain ActionListener from the ToggleButtonsList
     *
     * @param listener
     */
    public void removeActionListener(final ActionListener listener) {
        m_actionListeners.remove(listener);
    }

    /*
     * Fire an action event to the listeners
     */
    private void fireActionEvent(final ActionEvent e) {
        for (final ActionListener l : m_actionListeners) {
            l.actionPerformed(e);
        }
    }

    /**
     * Set the ActionCommand
     *
     * @param actionCommand
     */
    public void setActionCommand(final String actionCommand) {
        m_actionCommand = actionCommand;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        final Object source = e.getSource();

        if (!(source instanceof JToggleButton)) {
            return;
        }

        // only dealing with the toggleButtons
        final JToggleButton btn = (JToggleButton) source;

        String sel = btn.getText(); // future selection

        if (sel.equals(m_defaultText)) {
            sel = ClassModel.NO_CLASS;
        }

        setSelectedItem(sel);
        fireActionEvent(new ActionEvent(btn, (int) (Math.random() * 12312),
                m_actionCommand));
    }

    /**
     * Set the ClassModel
     *
     * @param classModel
     */
    public void setClassModel(final ClassModel classModel) {
        m_classModel = classModel;

        classModel.addListDataListener(this);

        recreateButtonList();
    }

    /**
     * Sets the Text of the ClassModel.NO_CLASS items
     *
     * @param defText
     */
    public void setDefaultText(final String defText) {
        m_defaultText = defText;

        recreateButtonList();
    }

    // Recreate the button list from scatch, when ClassModel changes
    /**
     * {@inheritDoc}
     */
    @Override
    public void contentsChanged(final ListDataEvent arg0) {
        recreateButtonList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void intervalAdded(final ListDataEvent arg0) {
        recreateButtonList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void intervalRemoved(final ListDataEvent arg0) {
        recreateButtonList();
    }

    private class CustomToggleButton extends JToggleButton {
        /**
         *
         */
        private static final long serialVersionUID = -390311905442686674L;

        public CustomToggleButton(final String caption) {
            super(caption);
        }

        @Override
        public void paint(final Graphics g) {
            super.paint(g);

            if (isSelected()) {
                g.setColor(new Color(65, 128, 64));
                final int x = getWidth() - 14;
                final int y = (getHeight() / 2) - 4;
                g.fillRect(x, y, 8, 8);
            }
        }
    }
}
