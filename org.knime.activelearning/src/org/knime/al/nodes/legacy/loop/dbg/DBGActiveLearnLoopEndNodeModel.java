package org.knime.al.nodes.legacy.loop.dbg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.al.nodes.AbstractALNodeModel;
import org.knime.al.nodes.legacy.loop.ActiveLearnLoopEnd;
import org.knime.al.nodes.legacy.loop.ActiveLearnLoopUtils;
import org.knime.al.util.NodeUtils;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortType;

/**
 *
 * @author dietzc University of Konstanz
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
final class DBGActiveLearnLoopEndNodeModel extends AbstractALNodeModel
        implements ActiveLearnLoopEnd {

    private final SettingsModelString m_groundTruthColumnModel =
            createGroundTruthClassColumnModel();
    private final SettingsModelInteger m_maxIterationsModel =
            createMaxIterationsModel();

    private Map<RowKey, String> m_newClassesMap;
    private Map<RowKey, String> m_allClassesMap;

    static final int UNLABELED_PORT = 0;
    static final int LABELED_PORT = 1;
    static final int PASSTHROUGH_PORT = 2;

    /**
     * @return Settings Model to store the class column of the ground truth
     *         table
     */
    static SettingsModelString createGroundTruthClassColumnModel() {
        return new SettingsModelString("ground_truth_class_column", "");
    }

    /**
     * @return Settings Model to store the maximal number of iterations.
     */
    static SettingsModelInteger createMaxIterationsModel() {
        return new SettingsModelInteger("max_iterations", 10);
    }

    // In 0 = All unlabeled examples
    // In 1 = All labeled examples
    // In 2 = Pass through data

    protected DBGActiveLearnLoopEndNodeModel() {
        super(new PortType[] { BufferedDataTable.TYPE, BufferedDataTable.TYPE,
                BufferedDataTable.TYPE_OPTIONAL },
                new PortType[] { BufferedDataTable.TYPE });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

        // in case the optional input is not connected, copy the first input.
        if (inSpecs[PASSTHROUGH_PORT] == null) {
            return new DataTableSpec[] { inSpecs[UNLABELED_PORT] };
        }
        // else route the optional input
        return new DataTableSpec[] { inSpecs[PASSTHROUGH_PORT] };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        final int currentIteration = getAvailableFlowVariables()
                .get(ActiveLearnLoopUtils.AL_STEP).getIntValue();

        // cache the rowkey to class mapping.
        if (currentIteration == 0) {
            initializeClassMap(inData);
        }

        exec.setProgress("Processing ...");

        m_newClassesMap = new HashMap<RowKey, String>();
        if ((currentIteration < (m_maxIterationsModel.getIntValue()))
                && (inData[UNLABELED_PORT].size() > 0)) {

            // Lookup class labels
            inData[UNLABELED_PORT].forEach((row) -> {
                final RowKey rowKey = row.getKey();
                m_newClassesMap.put(rowKey, m_allClassesMap.get(rowKey));
            });
            super.continueLoop();
            return null;
        } else {
            // return an empty table when the optional input is not connected
            if (inData[PASSTHROUGH_PORT] == null) {
                final BufferedDataContainer paddContainer = exec
                        .createDataContainer(inData[UNLABELED_PORT].getSpec());
                paddContainer.close();
                return new BufferedDataTable[] { paddContainer.getTable() };
            }
            return new BufferedDataTable[] { inData[PASSTHROUGH_PORT] };
        }
    }

    /**
     * Reads the classes from the ground truth table and adds them to the class
     * map.
     *
     * @param inData
     * @throws InvalidSettingsException
     */
    private void initializeClassMap(final BufferedDataTable[] inData)
            throws InvalidSettingsException {
        final int classColIdxLabeledTable = NodeUtils.autoColumnSelection(
                inData[LABELED_PORT].getDataTableSpec(),
                m_groundTruthColumnModel, StringValue.class, this.getClass());

        // initialize all classes map
        m_allClassesMap = new HashMap<>((int)inData[LABELED_PORT].size());

        for (final DataRow row : inData[LABELED_PORT]) {
            m_allClassesMap.put(row.getKey(),
                    ((StringCell) row.getCell(classColIdxLabeledTable))
                            .getStringValue());
        }
    }

    @Override
    public Map<RowKey, String> getNewlyLabeledRows() {
        return m_newClassesMap;
    }

    @Override
    protected List<SettingsModel> collectSettingsModels() {
        if (m_settingsModels == null) {
            m_settingsModels = new ArrayList<>();
            m_settingsModels.add(m_groundTruthColumnModel);
            m_settingsModels.add(m_maxIterationsModel);
        }
        return m_settingsModels;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // Nothing to do here.
    }
}
