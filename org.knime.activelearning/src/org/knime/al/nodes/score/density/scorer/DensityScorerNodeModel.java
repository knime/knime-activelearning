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
package org.knime.al.nodes.score.density.scorer;

import java.util.List;

import org.knime.al.nodes.AbstractALNodeModel;
import org.knime.al.nodes.score.ExceptionHandling;
import org.knime.al.nodes.score.density.DensityScorerModel;
import org.knime.al.nodes.score.density.DensityScorerPortObject;
import org.knime.al.nodes.score.density.DensityScorerPortObjectSpec;
import org.knime.al.nodes.score.density.UnknownRowException;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.util.UniqueNameGenerator;

import com.google.common.collect.Lists;

/**
 * Node model for the Density Scorer node.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class DensityScorerNodeModel extends AbstractALNodeModel {

    private static final String UNKNOWN_ROW_TEMPLATE = "Unknown row %s in input table.";

    private static final MissingCell MISSING_CELL = new MissingCell("Unknown row.");

    private static final int UNLABELED_INPORT = 1;

    private static final int MODEL_INPORT = 0;

    static SettingsModelString createOutputColumnNameModel() {
        return new SettingsModelString("outputColumnName", "Density Score");
    }

    static SettingsModelString createUnknownRowHandling() {
        return new SettingsModelString("unknownRowHandling", ExceptionHandling.FAIL.name());
    }

    private final SettingsModelString m_outputColumnName = createOutputColumnNameModel();

    private final SettingsModelString m_unknownRowHandling = createUnknownRowHandling();

    /**
     */
    protected DensityScorerNodeModel() {
        super(new PortType[]{DensityScorerPortObject.TYPE, BufferedDataTable.TYPE},
            new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DensityScorerPortObjectSpec modelSpec = (DensityScorerPortObjectSpec)inSpecs[MODEL_INPORT];
        final DataTableSpec unlabeledSpec = (DataTableSpec)inSpecs[UNLABELED_INPORT];
        // not really necessary but if the features don't match it's unlikely the key will be correct
        modelSpec.checkCompatibility(unlabeledSpec);
        return new PortObjectSpec[]{createRearranger(unlabeledSpec, null).createSpec()};
    }

    private ColumnRearranger createRearranger(final DataTableSpec unlabeledSpec, final DensityScorerModel model) {
        final UniqueNameGenerator nameGen = new UniqueNameGenerator(unlabeledSpec);
        final ColumnRearranger rearranger = new ColumnRearranger(unlabeledSpec);
        rearranger
            .append(new SingleCellFactory(nameGen.newColumn(m_outputColumnName.getStringValue(), DoubleCell.TYPE)) {
                private final boolean m_failOnUnknown = failOnMissing();

                @Override
                public DataCell getCell(final DataRow row) {
                    try {
                        return new DoubleCell(model.getPotential(row.getKey()));
                    } catch (UnknownRowException e) {
                        if (m_failOnUnknown) {
                            throw new IllegalArgumentException(
                                String.format(UNKNOWN_ROW_TEMPLATE, e.getUnknownKey()), e);
                        } else {
                            setWarningMessage(String.format(
                                "The output for row %s is a missing value because it is unknown to the model.",
                                e.getUnknownKey()));
                            return MISSING_CELL;
                        }
                    }
                }
            });
        return rearranger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final DensityScorerPortObject densityScorerPortObject = (DensityScorerPortObject)inData[MODEL_INPORT];
        final DensityScorerModel model = densityScorerPortObject.getModel();
        final BufferedDataTable unlabeledData = (BufferedDataTable)inData[UNLABELED_INPORT];
        final ColumnRearranger rearranger = createRearranger(unlabeledData.getDataTableSpec(), model);
        final BufferedDataTable outputTable =
            exec.createColumnRearrangeTable(unlabeledData, rearranger, exec);
        return new PortObject[]{outputTable};
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
        return Lists.newArrayList(m_outputColumnName, m_unknownRowHandling);
    }

    private boolean failOnMissing() {
        return ExceptionHandling.valueOf(m_unknownRowHandling.getStringValue()) == ExceptionHandling.FAIL;
    }

}
