package org.knime.al.nodes.legacy.loop.end.components;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * ButtonList
 *
 * A single-selection, no-duplicates list of buttons for quick selection of an
 * item.
 *
 * Used in ActiveLearnLoopEndNodeViewPanel
 *
 * @author Jonathan Hale
 */
public class ButtonList extends JPanel
        implements ActionListener, ListDataListener {

    private static final long serialVersionUID = 987049663025341598L;

    private ClassModel m_classModel;

    private String m_selectedItem;

    private final int m_maxRows;

    private final List<JPanel> m_panels;
    private final List<JButton> m_buttons;
    private JPanel m_lastPanel;

    private String m_defaultText = "default";

    /**
     * Constructor.
     *
     * @param maxRows
     *            maximum number of buttons in a column
     */
    public ButtonList(final int maxRows) {
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
    public ButtonList(final ClassModel classModel, final int maxRows) {
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

        for (final JButton btn : m_buttons) {
            btn.removeActionListener(this);
        }

        m_panels.clear();
        m_buttons.clear();

        removeAll();

        // create an initial button for the start.
        for (final String name : m_classModel.getDefinedClasses()) {
            addButton(name);
        }
    }

    /*
     * Helper function to add buttons
     */
    private JButton addButton(final String value) {
        final int numBtns = m_buttons.size();

        if (numBtns >= (m_maxRows * m_panels.size())) {
            // create a new Panel
            final JPanel panel = new JPanel(new GridLayout(m_maxRows, 1));

            this.add(panel);

            m_panels.add(panel);
            m_lastPanel = panel;
        }
        // createButton and add this as itemListener
        final JButton btn = new JButton(value);
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

        if (!(source instanceof JButton)) {
            return;
        }

        // only dealing with the toggleButtons
        final JButton btn = (JButton) source;

        m_selectedItem = btn.getText(); // future selection

        if (m_selectedItem.equals(m_defaultText)) {
            m_selectedItem = ClassModel.NO_CLASS;
        }

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
}
