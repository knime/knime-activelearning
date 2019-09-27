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
 *   Aug 7, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.al.nodes.score.density;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.knime.al.nodes.AbstractALNodeModel;
import org.knime.al.nodes.score.ExceptionHandling;
import org.knime.al.util.NodeTools;
import org.knime.base.data.filter.column.FilterColumnRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractDensityInitializerNodeModel extends AbstractALNodeModel {

    static final int DATA_PORT = 0;

    /**
     * @return Settings model to store the column filter settings.
     */
    @SuppressWarnings("unchecked")
    static SettingsModelColumnFilter2 createColumnFilterModel() {
        return new SettingsModelColumnFilter2("filter_string_model", DoubleValue.class);
    }

    static SettingsModelString createMissingValueHandling() {
        return new SettingsModelString("missingValueHandling", ExceptionHandling.FAIL.name());
    }

    private final SettingsModelColumnFilter2 m_columnFilterModel = createColumnFilterModel();

    private final SettingsModelString m_missingValueHandling = createMissingValueHandling();

    /**
     */
    protected AbstractDensityInitializerNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{DensityScorerPortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec tableSpec = (DataTableSpec)inSpecs[DATA_PORT];
        if (NodeTools.collectAllColumnIndicesOfType(DoubleValue.class, tableSpec).isEmpty()) {
            throw new InvalidSettingsException("No numerical columns avaiable.");
        }
        return new PortObjectSpec[]{createSpec(tableSpec)};
    }

    private DensityScorerPortObjectSpec createSpec(final DataTableSpec tableSpec) throws InvalidSettingsException {
        final String[] includes = m_columnFilterModel.applyTo(tableSpec).getIncludes();
        final ColumnRearranger cr = new ColumnRearranger(tableSpec);
        cr.keepOnly(includes);

        DataTableSpec featureSpec = cr.createSpec();
        CheckUtils.checkSetting(featureSpec.stream().allMatch(c -> c.getType().isCompatible(DoubleValue.class)),
            "Not all specified feature columns are numeric.");
        return new DensityScorerPortObjectSpec(featureSpec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        // Init data structures and potentials
        BufferedDataTable unlabeledTable = (BufferedDataTable)inData[DATA_PORT];
        CheckUtils.checkSetting(unlabeledTable.size() > 0, "The input table is empty.");
        checkInputTable(unlabeledTable);
        exec.setProgress("Init data structures and density");
        final DensityScorerModel model = initialize(unlabeledTable, exec);
        final FileStore neighborhoodFileStore = createFileStore(exec);
        final FileStore potentialsFilestore = createFileStore(exec);
        final DensityScorerPortObject po =
            DensityScorerPortObject.createPortObject(createSpec(unlabeledTable.getDataTableSpec()), model,
                neighborhoodFileStore, potentialsFilestore);
        return new PortObject[]{po};
    }

    private static FileStore createFileStore(final ExecutionContext exec) throws IOException {
        return exec.createFileStore(UUID.randomUUID().toString());
    }

    /**
     * Allows extending classes to validate the input table with regard to algorithm specific settings.
     *
     * @param table the input table of the node
     * @throws InvalidSettingsException if the settings are not compatible with the input table
     */
    protected abstract void checkInputTable(final BufferedDataTable table) throws InvalidSettingsException;

    private DensityScorerModel initialize(final BufferedDataTable unlabeledTable, final ExecutionMonitor progress)
        throws CanceledExecutionException {
        final DensityScorerModelCreator builder = createBuilder(unlabeledTable, progress.createSubProgress(0.1));
        final DensityScorerModel model = builder.buildModel(progress.createSubProgress(0.9));
        builder.getWarning().ifPresent(this::setWarningMessage);
        return model;
    }

    private DensityScorerModelCreator createBuilder(final BufferedDataTable unlabeledTable,
        final ExecutionMonitor progress) throws CanceledExecutionException {
        final List<Integer> selectedIndices = NodeTools.getIndicesFromFilter(unlabeledTable.getSpec(),
            m_columnFilterModel, DoubleValue.class, this.getClass());
        final int[] idxs = selectedIndices.stream().mapToInt(Integer::intValue).toArray();
        final DensityScorerModelCreator builder = createBuilder(idxs.length);
        builder.setMissingValueHandling(ExceptionHandling.valueOf(m_missingValueHandling.getStringValue()));
        final long size = unlabeledTable.size();
        try (final CloseableRowIterator iter = unlabeledTable.filter(TableFilter.materializeCols(idxs)).iterator()) {
            for (long i = 1; iter.hasNext(); i++) {
                progress.checkCanceled();
                progress.setProgress(i / ((double)size), String.format("Reading row %s of %s.", i, size));
                builder.addRow(new FilterColumnRow(iter.next(), idxs));
            }
        }
        return builder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // Nothing to reset
    }

    /**
     * @param nrFeatures the number of features used
     * @return a {@link DensityScorerModelCreator} corresponding to the current node configuration
     */
    protected abstract DensityScorerModelCreator createBuilder(final int nrFeatures);

    /**
     * {@inheritDoc}
     */
    @Override
    protected final List<SettingsModel> collectSettingsModels() {
        final List<SettingsModel> list = new ArrayList<>();
        list.add(m_columnFilterModel);
        list.add(m_missingValueHandling);
        list.addAll(getSettingsModels());
        return list;
    }

    /**
     * @return the settings models used in implementing classes
     */
    protected abstract List<SettingsModel> getSettingsModels();

}
