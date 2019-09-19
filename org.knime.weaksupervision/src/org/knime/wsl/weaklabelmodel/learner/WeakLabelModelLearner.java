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
 *   Sep 6, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.wsl.weaklabelmodel.learner;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.base.data.filter.column.FilterColumnRow;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.util.CheckUtils;
import org.knime.wsl.weaklabelmodel.LabelMatrixReader;
import org.knime.wsl.weaklabelmodel.LabelModelAdapter;
import org.knime.wsl.weaklabelmodel.SourceParser;
import org.knime.wsl.weaklabelmodel.SourceParserFactory;
import org.knime.wsl.weaklabelmodel.WeakLabelModelContent;
import org.knime.wsl.weaklabelmodel.WeakLabelModelPortObjectSpec;

import com.google.common.collect.Iterators;

/**
 * Coordinates the training of a Weak Label Model.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class WeakLabelModelLearner {

    private final MetaData m_metaData;

    private final WeakLabelModelLearnerSettings m_settings;

    private final CorrelationHandler m_correlationGraphHandler;

    WeakLabelModelLearner(final WeakLabelModelLearnerSettings settings, final DataTableSpec spec) {
        m_settings = settings;
        final String[] sourceColumnNames = m_settings.getNoisyLabelsFilter().applyTo(spec).getIncludes();
        m_metaData = new MetaData(sourceColumnNames, spec);
        m_correlationGraphHandler = new CorrelationHandler(m_metaData.getNonEmptyColumns(), m_metaData.getNumClasses());
    }

    WeakLabelModelContent learn(final BufferedDataTable table, final ExecutionMonitor monitor) throws Exception {
        try (final LabelModelAdapter lma = LabelModelAdapter.createDefault()) {
            final float[][] covarianceMatrix = createCovarianceMatrix(table, monitor.createSubProgress(0.8));
            monitor.setMessage("Initializing model.");
            initialize(lma, covarianceMatrix);
            monitor.setProgress(0.8, "Start training.");
            train(lma, monitor.createSubProgress(0.2));
            return new WeakLabelModelContent(lma.getMu(), getClassBalance());
        }
    }

    List<String> getWarnings() {
        return m_metaData.getEmptyColumns().stream()
            .map(n -> String.format("The column %s was ignored because it doesn't specify its classes.", n))
            .collect(Collectors.toList());
    }

    private float[][] createCovarianceMatrix(final BufferedDataTable table, final ExecutionMonitor progress)
        throws CanceledExecutionException {
        CheckUtils.checkArgument(table.size() <= Integer.MAX_VALUE,
            "The table has more than Integer.MAX_VALUE rows wich is currently not supported.");
        final int[] nonEmptyIndices = m_metaData.getNonEmptyIndices();
        try (CloseableRowIterator iter = table.filter(TableFilter.materializeCols(nonEmptyIndices)).iterator()) {
            final LabelMatrixReader reader =
                new LabelMatrixReader(createSourceParsers(table.getDataTableSpec()), m_metaData.getNumClasses());
            final Iterator<DataRow> filteredIterator =
                Iterators.transform(iter, r -> new FilterColumnRow(r, nonEmptyIndices));
            // the cast to int is save because we checked that table.size() is an integer
            // in the first line of this method
            return reader.readCovarianceMatrix(filteredIterator, table.size(), progress);
        }
    }

    private SourceParser[] createSourceParsers(final DataTableSpec tableSpec) {
        final ColumnRearranger cr = new ColumnRearranger(tableSpec);
        cr.keepOnly(m_metaData.getNonEmptyIndices());
        final DataTableSpec filtered = cr.createSpec();
        return SourceParserFactory.createParsers(filtered, m_metaData.getPossibleLabels().stream()
            .map(DataCell::toString).collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    Set<String> getClassNames() {
        final LinkedHashSet<String> names = new LinkedHashSet<>();
        for (DataCell possibleClass : m_metaData.getPossibleLabels()) {
            names.add(possibleClass.toString());
        }
        return names;
    }

    WeakLabelModelPortObjectSpec createPortObjectSpec() {
        return new WeakLabelModelPortObjectSpec(m_metaData.getSourceSpec(),
            m_metaData.getPossibleLabels().stream().map(DataCell::toString).toArray(String[]::new));
    }

    private void train(final LabelModelAdapter lma, final ExecutionMonitor monitor) throws CanceledExecutionException {
        final double maxEpoch = m_settings.getEpochs();
        for (int i = 1; i <= maxEpoch; i++) {
            final float loss = lma.trainStep();
            monitor.checkCanceled();
            final int epoch = i;
            monitor.setProgress(i / maxEpoch,
                () -> String.format("Finished epoch %s of %s. Loss: %s", epoch, (long)maxEpoch, loss));
        }
    }

    private void initialize(final LabelModelAdapter lma, final float[][] covarianceMatrix) {
        final boolean[][] mask = m_correlationGraphHandler.buildMask();
        lma.initialize(covarianceMatrix, mask, getClassBalance(), getInitialPrecisions(),
            (float)m_settings.getLearningRate());
    }

    private float[] getClassBalance() {
        final int length = m_metaData.getNumClasses();
        final float[] array = new float[length];
        final float uniform = 1.0f / length;
        Arrays.fill(array, uniform);
        return array;
    }

    private float[] getInitialPrecisions() {
        final float[] precInit = new float[m_metaData.getNumSources()];
        Arrays.fill(precInit, 0.7f);
        return precInit;
    }
}
