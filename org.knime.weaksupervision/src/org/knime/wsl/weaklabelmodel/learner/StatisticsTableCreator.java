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
 *   Sep 16, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.wsl.weaklabelmodel.learner;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.wsl.weaklabelmodel.WeakLabelModelContent;
import org.knime.wsl.weaklabelmodel.WeakLabelModelPortObjectSpec;

import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;

/**
 * Creates the weak label model statistics.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class StatisticsTableCreator {

    private final Set<String> m_labels;

    private final String[] m_sourceColumnNames;

    StatisticsTableCreator(final WeakLabelModelPortObjectSpec spec) {
        m_labels = spec.getPossibleClasses();
        m_sourceColumnNames = spec.getTrainingSpec().getColumnNames();
    }

    DataTableSpec createSpec() {
        final DataTableSpec probSpecs = createConditionalProbabilitySpecs();
        final DataTableSpecCreator creator = new DataTableSpecCreator();
        final UniqueNameGenerator nameGen = new UniqueNameGenerator(probSpecs);
        creator.addColumns(nameGen.newColumn("Label Source", StringCell.TYPE));
        creator.addColumns(nameGen.newColumn("Latent Label", StringCell.TYPE));
        creator.addColumns(probSpecs);
        return creator.createSpec();
    }

    private DataTableSpec createConditionalProbabilitySpecs() {
        final UniqueNameGenerator probNameGen = new UniqueNameGenerator(Sets.newHashSet(m_labels));
        return new DataTableSpec(
            Stream.concat(Stream.of(probNameGen.newName("Abstain")), m_labels.stream()).toArray(String[]::new),
            IntStream.range(0, m_labels.size() + 1).mapToObj(i -> DoubleCell.TYPE).toArray(DataType[]::new));
    }

    BufferedDataTable createStatisticsTable(final WeakLabelModelContent model, final ExecutionContext exec) {
        final DataTableSpec statisticsSpec = createSpec();
        final BufferedDataContainer container = exec.createDataContainer(statisticsSpec);
        final Iterator<String> sourceNames = Iterators.forArray(m_sourceColumnNames);
        final Collection<DataCell> possibleClasses =
            m_labels.stream().map(StringCell::new).collect(Collectors.toList());
        final ConditionalProbabilities cProbs = new ConditionalProbabilities(model.getMu());
        long rowIdx = 0;
        for (int lf = 0; lf < m_sourceColumnNames.length; lf++) {
            final StringCell lfName = new StringCell(sourceNames.next());
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

}
