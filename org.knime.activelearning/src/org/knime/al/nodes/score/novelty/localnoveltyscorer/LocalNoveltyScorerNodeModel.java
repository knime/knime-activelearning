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
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.al.nodes.score.novelty.localnoveltyscorer;

import java.io.File;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.math3.linear.RealMatrix;
import org.knime.al.util.noveltydetection.kernel.EXPHIKKernel;
import org.knime.al.util.noveltydetection.kernel.HIKKernel;
import org.knime.al.util.noveltydetection.kernel.KernelCalculator;
import org.knime.al.util.noveltydetection.kernel.KernelCalculator.KernelType;
import org.knime.al.util.noveltydetection.kernel.KernelFunction;
import org.knime.al.util.noveltydetection.kernel.PolynomialKernel;
import org.knime.al.util.noveltydetection.kernel.RBFKernel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Crop BitMasks or parts of images according to a Labeling
 *
 * @author <a href="mailto:adrian.nembach@uni-konstanz.de">Adrian Nembach</a>
 *
 */
public class LocalNoveltyScorerNodeModel extends NodeModel
        implements BufferedDataTableHolder {

    static final int DEFAULT_NUMBER_OF_NEIGHBORS = 100;
    static final String[] AVAILABLE_KERNELS = { "HIK", "EXPHIK" };
    static final KernelType DEFAULT_KERNEL = KernelType.RBF;
    static final boolean DEFAULT_SORT_TABLE = false;
    static final boolean DEFAULT_NORMALIZE = true;
    static final double DEFAULT_SIGMA = 0.5;
    static final double DEFAULT_GAMMA = 1.0;
    static final double DEFAULT_BIAS = 2.0;
    static final double DEFAULT_POWER = 3.0;

    /**
     * Helper
     *
     * @return SettingsModel to store img column
     */

    static SettingsModelInteger createNumberOfNeighborsModel() {
        return new SettingsModelIntegerBounded("NumberOfNeighbors",
                DEFAULT_NUMBER_OF_NEIGHBORS, 1, Integer.MAX_VALUE);
    }

    static SettingsModelString createKernelFunctionSelectionModel() {
        return new SettingsModelString("kernelFunctionLocalNoveltyScorer",
                DEFAULT_KERNEL.toString());
    }

    static SettingsModelFilterString createColumnSelectionModel() {
        return new SettingsModelFilterString("ColumnFilterLocalNoveltyScorer");
    }

    static SettingsModelString createClassColumnSelectionModel() {
        return new SettingsModelString("ClassLocalNoveltyScorer", "");
    }

    static SettingsModelBoolean createSortTableModel() {
        return new SettingsModelBoolean("SortTableLocalNoveltyScorer",
                DEFAULT_SORT_TABLE);
    }

    static SettingsModelBoolean createNormalizeModel() {
        return new SettingsModelBoolean("NormalizeLocalNoveltyScorer",
                DEFAULT_NORMALIZE);
    }

    static SettingsModelDouble createRBFSigmaModel() {
        final SettingsModelDouble sm =
                new SettingsModelDouble("sigmaRBF", DEFAULT_SIGMA);
        // sm.setEnabled(false);
        return sm;
    }

    static SettingsModelDouble createPolynomialGammaModel() {
        final SettingsModelDouble sm =
                new SettingsModelDouble("gammaPolynomial", DEFAULT_GAMMA);
        sm.setEnabled(false);
        return sm;
    }

    static SettingsModelDouble createPolynomialBiasModel() {
        final SettingsModelDouble sm =
                new SettingsModelDouble("biasPolynomial", DEFAULT_BIAS);
        sm.setEnabled(false);
        return sm;
    }

    static SettingsModelDouble createPolynomialPower() {
        final SettingsModelDouble sm =
                new SettingsModelDouble("powerPolynomial", DEFAULT_POWER);
        sm.setEnabled(false);
        return sm;
    }

    /* SettingsModels */
    private final SettingsModelInteger m_numberOfNeighbors =
            createNumberOfNeighborsModel();
    private final SettingsModelString m_kernelFunction =
            createKernelFunctionSelectionModel();
    private final SettingsModelFilterString m_columnSelection =
            createColumnSelectionModel();
    private final SettingsModelString m_classColumn =
            createClassColumnSelectionModel();
    private final SettingsModelBoolean m_sortTable = createSortTableModel();
    private final SettingsModelBoolean m_normalize = createNormalizeModel();
    private final SettingsModelDouble m_sigma = createRBFSigmaModel();
    private final SettingsModelDouble m_gamma = createPolynomialGammaModel();
    private final SettingsModelDouble m_bias = createPolynomialBiasModel();
    private final SettingsModelDouble m_power = createPolynomialPower();

    /* Resulting BufferedDataTable */
    private BufferedDataTable m_data;

    /**
     * Constructor LocalNoveltyScorerNodeModel
     */
    public LocalNoveltyScorerNodeModel() {
        super(2, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

        final DataTableSpec trainingTableSpec = inSpecs[0];
        final DataTableSpec testTableSpec = inSpecs[1];

        // Check for class column
        final DataColumnSpec classColSpec =
                trainingTableSpec.getColumnSpec(m_classColumn.getStringValue());
        if (classColSpec == null
                || !classColSpec.getType().isCompatible(NominalValue.class)) {
            for (int i = trainingTableSpec.getNumColumns() - 1; i >= 0; i--) {
                if (trainingTableSpec.getColumnSpec(i).getType()
                        .isCompatible(NominalValue.class)) {
                    m_classColumn.setStringValue(
                            trainingTableSpec.getColumnSpec(i).getName());
                    break;
                } else if (i == 0) {
                    throw new InvalidSettingsException(
                            "Table contains no nominal attribute for classification.");
                }
            }
        }

        // Check if selected columns from the training table are also in the
        // test table
        // Also check compatibility of selected columns

        final List<String> includedCols = m_columnSelection.getIncludeList();
        for (final String col : includedCols) {
            if (!trainingTableSpec.getColumnSpec(col).getType()
                    .isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException(
                        "Selected columns must be compatible with DoubleValue!");
            }
            if (!testTableSpec.containsName(col)) {
                throw new InvalidSettingsException(
                        "Selected columns need also be contained in the Test Table");
            }
        }

        return createOutSpec(testTableSpec);
    }

    private DataTableSpec[] createOutSpec(final DataTableSpec inTableSpec) {
        final DataColumnSpec scoreSpec =
                new DataColumnSpecCreator("Novelty Score", DoubleCell.TYPE)
                        .createSpec();

        return new DataTableSpec[] {
                new DataTableSpec(inTableSpec, new DataTableSpec(scoreSpec)) };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({})
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        BufferedDataTable trainingIn = inData[0];
        final BufferedDataTable testIn = inData[1];

        final long longTrainingSize = trainingIn.size();
        final long longTestSize = testIn.size();

        if (longTrainingSize == 0 || longTestSize == 0) {
            throw new InvalidSettingsException(
                    "One of the input tables is empty");
        } else if (longTrainingSize > Integer.MAX_VALUE) {
            throw new InvalidSettingsException(
                    "The training table is too large.");
        }

        final int trainingSize = (int) longTrainingSize;

        final ColumnRearranger trainingRearranger =
                new ColumnRearranger(trainingIn.getDataTableSpec());
        final ColumnRearranger testRearranger =
                new ColumnRearranger(testIn.getDataTableSpec());
        final List<String> includedCols = m_columnSelection.getIncludeList();
        int numberOfNeighbors = m_numberOfNeighbors.getIntValue();
        final int classColIdx = trainingIn.getDataTableSpec()
                .findColumnIndex(m_classColumn.getStringValue());

        ExecutionMonitor calcProgMon = null;
        ExecutionMonitor globalKernelProgMon = exec.createSubProgress(0.2);
        ExecutionMonitor trainingKernelProgMon = exec.createSubProgress(0.2);
        final ExecutionContext appendNoveltyExec =
                exec.createSubExecutionContext(0.1);

        if (m_sortTable.getBooleanValue()) {
            final ExecutionContext sortExec =
                    exec.createSubExecutionContext(0.1);
            calcProgMon = exec.createSubProgress(0.4);
            globalKernelProgMon = exec.createSubProgress(0.2);
            trainingKernelProgMon = exec.createSubProgress(0.2);
            final BufferedDataTableSorter sorter = new BufferedDataTableSorter(
                    trainingIn, new Comparator<DataRow>() {

                        @Override
                        public int compare(final DataRow arg0,
                                final DataRow arg1) {
                            final String c1 =
                                    ((StringCell) arg0.getCell(classColIdx))
                                            .getStringValue();
                            final String c2 =
                                    ((StringCell) arg1.getCell(classColIdx))
                                            .getStringValue();
                            return c1.compareTo(c2);
                        }

                    });
            trainingIn = sorter.sort(sortExec);
            sortExec.setProgress(1.0);
        } else {
            calcProgMon = exec.createSubProgress(0.5);
        }

        if (numberOfNeighbors > trainingSize) {
            numberOfNeighbors = trainingSize;
        }

        trainingRearranger.keepOnly(
                includedCols.toArray(new String[includedCols.size()]));
        testRearranger.keepOnly(
                includedCols.toArray(new String[includedCols.size()]));

        final double[][] trainingData = KernelCalculator.readBufferedDataTable(
                exec.createColumnRearrangeTable(trainingIn, trainingRearranger,
                        exec));
        final double[][] testData = KernelCalculator.readBufferedDataTable(
                exec.createColumnRearrangeTable(testIn, testRearranger, exec));

        // Get labels for training Data
        final String[] labels = new String[trainingSize];
        int l = 0;
        for (final DataRow row : trainingIn) {
            final DataCell classCell = row.getCell(classColIdx);
            if (classCell.isMissing()) {
                throw new IllegalArgumentException(
                        "Missing values are not supported.");
            } else if (!classCell.getType().isCompatible(StringValue.class)) {
                throw new IllegalArgumentException(
                        "The class column must be nominal.");
            }
            final StringValue label = (StringValue) row.getCell(classColIdx);

            labels[l++] = label.getStringValue();
        }

        // Get KernelFunction
        KernelFunction kernelFunction = null;
        final KernelType kernelType =
                KernelType.valueOf(m_kernelFunction.getStringValue());
        switch (kernelType) {
        case HIK:
            kernelFunction = new HIKKernel();
            break;
        case EXPHIK:
            kernelFunction = new EXPHIKKernel();
            break;
        case RBF:
            kernelFunction = new RBFKernel(m_sigma.getDoubleValue());
            break;
        case Polynomial:
            kernelFunction = new PolynomialKernel(m_gamma.getDoubleValue(),
                    m_bias.getDoubleValue(), m_power.getDoubleValue());
            break;
        default:
            kernelFunction = new RBFKernel(m_sigma.getDoubleValue());
        }

        exec.checkCanceled();

        // Create KernelCalculator
        final KernelCalculator kernelCalculator =
                new KernelCalculator(trainingData, kernelFunction);

        exec.checkCanceled();

        // Get global KernelMatrix
        final RealMatrix globalKernelMatrix = kernelCalculator
                .kernelize(trainingData, testData, globalKernelProgMon);
        globalKernelProgMon.setProgress(1.0);

        exec.checkCanceled();

        // Get training KernelMatrix
        final RealMatrix trainingKernelMatrix = kernelCalculator
                .kernelize(trainingData, trainingData, trainingKernelProgMon);
        trainingKernelProgMon.setProgress(1.0);

        exec.checkCanceled();

        // Keep thread approach for possible use later
        // ThreadController threadController = new ThreadController(exec,
        // globalKernelMatrix, trainingKernelMatrix, labels, numberOfNeighbors,
        // m_normalize.getBooleanValue());
        // double[] noveltyScores = threadController.process();

        final LocalNoveltyScorer localNoveltyScorer = new LocalNoveltyScorer(
                calcProgMon, globalKernelMatrix, trainingKernelMatrix, labels,
                numberOfNeighbors, m_normalize.getBooleanValue());
        final double[] noveltyScores =
                localNoveltyScorer.calculateNoveltyScores();
        exec.checkCanceled();

        final ColumnRearranger appendNoveltyRearranger =
                new ColumnRearranger(testIn.getDataTableSpec());
        appendNoveltyRearranger.append(new appendNoveltyScoreCellFactory(
                new DataColumnSpecCreator("Novelty Score", DoubleCell.TYPE)
                        .createSpec(),
                noveltyScores));
        m_data = exec.createColumnRearrangeTable(testIn,
                appendNoveltyRearranger, appendNoveltyExec);

        return new BufferedDataTable[] { m_data };
    }

    private class appendNoveltyScoreCellFactory extends SingleCellFactory {

        private final double[] m_noveltyScores;
        private int m_index;

        public appendNoveltyScoreCellFactory(final DataColumnSpec newColSpec,
                final double[] noveltyScores) {
            super(newColSpec);
            m_noveltyScores = noveltyScores;
            m_index = 0;
            this.setParallelProcessing(false);
        }

        @Override
        public DataCell getCell(final DataRow row) {
            return new DoubleCell(m_noveltyScores[m_index++]);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[] { m_data };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_kernelFunction.loadSettingsFrom(settings);
        m_columnSelection.loadSettingsFrom(settings);
        m_classColumn.loadSettingsFrom(settings);
        m_numberOfNeighbors.loadSettingsFrom(settings);
        m_sortTable.loadSettingsFrom(settings);
        m_sigma.loadSettingsFrom(settings);
        m_gamma.loadSettingsFrom(settings);
        m_bias.loadSettingsFrom(settings);
        m_power.loadSettingsFrom(settings);
        m_normalize.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_kernelFunction.saveSettingsTo(settings);
        m_columnSelection.saveSettingsTo(settings);
        m_classColumn.saveSettingsTo(settings);
        m_numberOfNeighbors.saveSettingsTo(settings);
        m_sortTable.saveSettingsTo(settings);
        m_sigma.saveSettingsTo(settings);
        m_gamma.saveSettingsTo(settings);
        m_bias.saveSettingsTo(settings);
        m_power.saveSettingsTo(settings);
        m_normalize.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        m_data = tables[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_kernelFunction.validateSettings(settings);
        m_columnSelection.validateSettings(settings);
        m_classColumn.validateSettings(settings);
        m_numberOfNeighbors.validateSettings(settings);
        m_sortTable.validateSettings(settings);
        m_sigma.validateSettings(settings);
        m_gamma.validateSettings(settings);
        m_bias.validateSettings(settings);
        m_power.validateSettings(settings);
        m_normalize.validateSettings(settings);
    }
}
