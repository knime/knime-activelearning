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
 */
package org.knime.wsl.labelmodel;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.base.data.filter.column.FilterColumnRow;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.wsl.WeakSupervisionPlugin;

import com.google.common.collect.Iterators;

/**
 * Acts as interface between KNIME and the TensorFlow backend.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class LabelModel implements AutoCloseable {

    private final LabelModelSettings m_settings;

    private LabelModelAdapter m_labelModel;

    private final CorrelationHandler m_correlationGraphHandler;

    private final MetaData m_metaData;

    static final String SAVED_MODEL_PATH = WeakSupervisionPlugin.getDefault().getPluginRootPath() + File.separator
        + "resources" + File.separator + "labelmodel" + File.separator + "labelModel";

    LabelModel(final LabelModelSettings settings, final DataTableSpec spec) {
        m_settings = settings;
        final String[] sourceColumnNames = m_settings.getNoisyLabelsFilter().applyTo(spec).getIncludes();
        m_metaData = new MetaData(sourceColumnNames, spec);
        m_correlationGraphHandler =
            new CorrelationHandler(m_metaData.getNonEmptyColumns(), m_metaData.getCardinality());
    }

    private Set<String> getClassNames() {
        final LinkedHashSet<String> names = new LinkedHashSet<>();
        for (DataCell possibleClass : m_metaData.getPossibleClasses()) {
            names.add(possibleClass.toString());
        }
        return names;
    }

    DataTableSpec createStatisticsSpec() {
        final DataTableSpec probSpecs = createConditionalProbabilitySpecs();
        final DataTableSpecCreator creator = new DataTableSpecCreator();
        final UniqueNameGenerator nameGen = new UniqueNameGenerator(probSpecs);
        creator.addColumns(nameGen.newColumn("Label Source", StringCell.TYPE));
        creator.addColumns(nameGen.newColumn("Latent Label", StringCell.TYPE));
        creator.addColumns(probSpecs);
        return creator.createSpec();
    }

    private DataTableSpec createConditionalProbabilitySpecs() {
        final Set<String> classes = getClassNames();
        final UniqueNameGenerator probNameGen = new UniqueNameGenerator(classes);
        return new DataTableSpec(
            Stream.concat(Stream.of(probNameGen.newName("Abstain")), classes.stream()).toArray(String[]::new),
            IntStream.range(0, classes.size() + 1).mapToObj(i -> DoubleCell.TYPE).toArray(DataType[]::new));
    }

    void loadSavedModel() {
        m_labelModel = new LabelModelAdapter(SAVED_MODEL_PATH);
    }

    BufferedDataTable createStatisticsTable(final ExecutionContext exec) {
        DataTableSpec statisticsSpec = createStatisticsSpec();
        final BufferedDataContainer container = exec.createDataContainer(statisticsSpec);
        final Iterator<String> lfNames = m_metaData.getNonEmptyColumns().iterator();
        final Set<DataCell> possibleClasses = m_metaData.getPossibleClasses();
        final ConditionalProbabilities cProbs = getConditionalProbabilities();
        long rowIdx = 0;
        for (int lf = 0; lf < m_metaData.getNumSources(); lf++) {
            final StringCell lfName = new StringCell(lfNames.next());
            final Iterator<DataCell> labels = possibleClasses.iterator();
            int numClasses = possibleClasses.size();
            for (int label = 1; label <= numClasses; label++) {
                final DataCell[] cells = new DataCell[statisticsSpec.getNumColumns()];
                cells[0] = lfName;
                cells[1] = labels.next();
                for (int i = 0; i < numClasses + 1; i++) {
                    cells[i + 2] = new DoubleCell(cProbs.getConditionalProbability(lf, i, label));
                }
                container.addRowToTable(new DefaultRow(RowKey.createRowKey(rowIdx), cells));
                rowIdx++;
            }
        }

        container.close();
        return container.getTable();
    }

    float[][] createAugmentedLabelMatrix(final BufferedDataTable table) {
        CheckUtils.checkArgument(table.size() <= Integer.MAX_VALUE,
            "The table has more than Integer.MAX_VALUE rows wich is currently not supported.");
        final int[] nonEmptyIndices = m_metaData.getNonEmptyIndices();
        try (CloseableRowIterator iter = table.filter(TableFilter.materializeCols(nonEmptyIndices)).iterator()) {
            final LabelMatrixReader reader =
                new LabelMatrixReader(m_metaData.getLabelToIdxMap(), nonEmptyIndices.length);
            final Iterator<DataRow> filteredIterator =
                Iterators.transform(iter, r -> new FilterColumnRow(r, nonEmptyIndices));
            // the cast to int is save because we checked that table.size() is an integer
            // in the first line of this method
            return reader.readAndAugmentLabelMatrix(filteredIterator, (int)table.size());
        }
    }

    private float[] getInitialPrecisions() {
        final float[] precInit = new float[m_metaData.getNumSources()];
        Arrays.fill(precInit, 0.7f);
        return precInit;
    }

    private float[] getClassBalance() {
        final int length = m_metaData.getCardinality();
        final float[] array = new float[length];
        final float uniform = 1.0f / length;
        Arrays.fill(array, uniform);
        return array;
    }

    float[][] train(final BufferedDataTable table, final ExecutionMonitor monitor) {
        loadSavedModel();
        final float[][] augmentedLabelMatrix = createAugmentedLabelMatrix(table);
        final float[] classBalance = getClassBalance();
        initialize(augmentedLabelMatrix, classBalance);
        final int maxEpoch = m_settings.getEpochs();
        for (int i = 0; i < maxEpoch; i++) {
            final float loss = m_labelModel.trainStep();
            monitor.setProgress((i + 1) / ((double)maxEpoch),
                String.format("Finished epoch %s of %s. Loss: %s", i, maxEpoch, loss));
        }
        return m_labelModel.getProbabilities(augmentedLabelMatrix);
    }

    private void initialize(final float[][] augmentedLabelMatrix, final float[] classBalance) {
        final boolean[][] mask = m_correlationGraphHandler.buildMask();
        m_labelModel.initialize(augmentedLabelMatrix, mask, classBalance, getInitialPrecisions(),
            (float)m_settings.getLearningRate());
    }

    @Override
    public void close() throws Exception {
        if (m_labelModel != null) {
            m_labelModel.close();
        }
        m_labelModel = null;
    }

    List<String> getWarnings() {
        return m_metaData.getEmptyColumns().stream()
            .map(n -> String.format("The column %s was ignored because it has no possible values assigned.", n))
            .collect(Collectors.toList());
    }

    ColumnRearranger createProbabilisticLabelRearranger(final DataTableSpec labelSourceSpec,
        final float[][] probabilisticLabels) {
        final ColumnRearranger rearranger = new ColumnRearranger(labelSourceSpec);
        if (m_settings.isRemoveSourceColumns()) {
            rearranger.remove(m_metaData.getNonEmptyIndices());
        }
        rearranger.append(new AbstractCellFactory(createProbabilisticLabelSpecs(labelSourceSpec)) {

            private int m_idx = -1;

            @Override
            public DataCell[] getCells(final DataRow row) {
                m_idx++;
                return toCells(probabilisticLabels[m_idx]);
            }

        });
        return rearranger;
    }

    private static DoubleCell[] toCells(final float[] values) {
        final DoubleCell[] cells = new DoubleCell[values.length];
        for (int i = 0; i < values.length; i++) {
            cells[i] = new DoubleCell(values[i]);
        }
        return cells;
    }

    private DataColumnSpec[] createProbabilisticLabelSpecs(final DataTableSpec labelSourceSpec) {
        final UniqueNameGenerator nameGen;
        if (m_settings.isRemoveSourceColumns()) {
            nameGen = new UniqueNameGenerator(Collections.emptySet());
        } else {
            nameGen = new UniqueNameGenerator(labelSourceSpec);
        }
        final String classColumnName = m_settings.getLabelColumnName();
        return m_metaData.getPossibleClasses().stream()
            .map(c -> nameGen.newColumn(String.format("P (%s=%s)", classColumnName, c.toString()), DoubleCell.TYPE))
            .toArray(DataColumnSpec[]::new);
    }

    // TODO uncomment once correlated sources are supported
    //    void readCorrelationsTable(final BufferedDataTable correlationsTable) {
    //        CheckUtils.checkArgument(correlationsTable != null,
    //            "Only call this method if the correlations table is not null.");
    //        @SuppressWarnings("null") // the check in the previous line ensures that correlationsTable is not null
    //        final DataTableSpec spec = correlationsTable.getDataTableSpec();
    //		final int firstCorrelationColumnIdx = spec.findColumnIndex(m_settings.getFirstCorrelationColumn());
    //		final int secondCorrelationColumnIdx = spec.findColumnIndex(m_settings.getSecondCorrelationColumn());
    //		final int[] filterIdxs = new int[] {firstCorrelationColumnIdx, secondCorrelationColumnIdx};
    //		try (CloseableRowIterator iter = correlationsTable
    //				.filter(TableFilter.materializeCols(firstCorrelationColumnIdx, secondCorrelationColumnIdx))
    //				.iterator()) {
    //			m_correlationGraphHandler.readCorrelationsTable(Iterators.transform(iter, r -> new FilterColumnRow(r, filterIdxs)));
    //		}
    //    }

    ConditionalProbabilities getConditionalProbabilities() {
        return new ConditionalProbabilities(m_labelModel.getMu());
    }
}