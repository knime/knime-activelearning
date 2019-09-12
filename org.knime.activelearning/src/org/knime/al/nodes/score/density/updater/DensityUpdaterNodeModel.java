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
 *   Aug 6, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.al.nodes.score.density.updater;

import java.util.List;
import java.util.UUID;

import org.knime.al.nodes.AbstractALNodeModel;
import org.knime.al.nodes.score.ExceptionHandling;
import org.knime.al.nodes.score.density.DensityScorerModel;
import org.knime.al.nodes.score.density.DensityScorerPortObject;
import org.knime.al.nodes.score.density.DensityScorerPortObjectSpec;
import org.knime.al.nodes.score.density.UnknownRowException;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;

import com.google.common.collect.Lists;

/**
 * Node model of the Density Updater node.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class DensityUpdaterNodeModel extends AbstractALNodeModel {

    private static final String UNKNOWN_ROW_TEMPLATE = "Unknown row %s in input table.";

    private static final int NEWLY_LABELED_INPORT = 1;

    private static final int MODEL_INPORT = 0;

    static SettingsModelString createUnknownRowHandling() {
        return new SettingsModelString("unknownRowHandling", ExceptionHandling.FAIL.name());
    }

    private final SettingsModelString m_unknownRowHandling = createUnknownRowHandling();

    /**
     */
    protected DensityUpdaterNodeModel() {
        super(new PortType[]{DensityScorerPortObject.TYPE, BufferedDataTable.TYPE},
            new PortType[]{DensityScorerPortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DensityScorerPortObjectSpec modelSpec = (DensityScorerPortObjectSpec)inSpecs[MODEL_INPORT];
        final DataTableSpec newlyLabeledSpec = (DataTableSpec)inSpecs[NEWLY_LABELED_INPORT];
        // not really necessary but if the features don't match it's unlikely the key will be correct
        final DataTableSpec featureSpec = modelSpec.getFeatureSpec();
        checkSpecsCompatible(featureSpec, newlyLabeledSpec, "input");
        return new PortObjectSpec[]{modelSpec};
    }

    @SuppressWarnings("null") // we explicitly check that tableCol is not null
    private static void checkSpecsCompatible(final DataTableSpec featureSpec, final DataTableSpec tableSpec,
        final String tableName) throws InvalidSettingsException {
        for (final DataColumnSpec featureCol : featureSpec) {
            final DataColumnSpec tableCol = tableSpec.getColumnSpec(featureCol.getName());
            CheckUtils.checkSetting(tableCol != null, "The %s table does not contain the required feature column %s.",
                tableName, featureCol);
            CheckUtils.checkSetting(featureCol.getType().isASuperTypeOf(tableCol.getType()),
                "The %s table contains a column with incompatible type for feature %s. Expected %s but received %s.",
                tableName, featureCol.getName(), featureCol.getType(), tableCol.getType());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final DensityScorerPortObject densityScorerPortObject = (DensityScorerPortObject)inData[MODEL_INPORT];
        final DensityScorerModel model = densityScorerPortObject.getModel();
        final BufferedDataTable newlyLabeledData = (BufferedDataTable)inData[NEWLY_LABELED_INPORT];
        updateModel(model, newlyLabeledData, exec);
        final DensityScorerPortObject outputPo = DensityScorerPortObject.createPortObject(
            densityScorerPortObject.getSpec(), model, exec.createFileStore(UUID.randomUUID().toString()));
        return new PortObject[]{outputPo};
    }

    private void updateModel(final DensityScorerModel model, final BufferedDataTable newlyLabeledData,
        final ExecutionMonitor monitor) throws CanceledExecutionException {
        final boolean failOnUnknown = failOnMissing();
        int unknownRows = 0;
        try (CloseableRowIterator iter = newlyLabeledData.filter(TableFilter.materializeCols()).iterator()) {
            final long size = newlyLabeledData.size();
            for (long i = 1; iter.hasNext(); i++) {
                final RowKey key = iter.next().getKey();
                monitor.checkCanceled();
                monitor.setProgress(i / ((double)size),
                    String.format("Updating model with newly labeled row %s (%s of %s).", key, i, size));
                try {
                    model.updateNeighbors(key);
                } catch (UnknownRowException e) {
                    if (failOnUnknown) {
                        throw new IllegalArgumentException(
                            String.format(UNKNOWN_ROW_TEMPLATE, e.getUnknownKey()), e);
                    } else {
                        // row is ignored
                        unknownRows++;
                    }
                }
            }
        }
        if (unknownRows > 0) {
            setUnknownRowsWarning(unknownRows);
        }
    }

    private void setUnknownRowsWarning(final int unknownRows) {
        final boolean single = unknownRows == 1;
        final String personalPronoun = single ? "it" : "they";
        final String verb = single ? "is" : "are";
        setWarningMessage(String.format(
            "%s row%s %s ignored during the update because %s %s unknown to the model.",
            unknownRows, single ? "" : "s", verb, personalPronoun, verb));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to reset
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<SettingsModel> collectSettingsModels() {
        return Lists.newArrayList(m_unknownRowHandling);
    }

    private boolean failOnMissing() {
        return ExceptionHandling.valueOf(m_unknownRowHandling.getStringValue()) == ExceptionHandling.FAIL;
    }

}
