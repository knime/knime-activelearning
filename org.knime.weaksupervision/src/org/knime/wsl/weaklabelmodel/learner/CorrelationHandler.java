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
package org.knime.wsl.weaklabelmodel.learner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.node.util.CheckUtils;
import org.knime.wsl.weaklabelmodel.learner.GraphUtil.CliqueVertex;
import org.knime.wsl.weaklabelmodel.learner.GraphUtil.SeparatorEdge;

import com.google.common.collect.Sets;

/**
 * Deals with everything related to correlated label sources.
 * This includes the clique-tree, the mask for training and later the junction-tree-mask as well.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany, KNIME GmbH, Konstanz, Germany
 */
final class CorrelationHandler {

	private final List<int[]> m_correlations = new ArrayList<>();

	private final Map<String, Integer> m_nonEmptySources;

	private final int m_cardinality;

	CorrelationHandler(final Set<String> sourceColumnNames, final int cardinality) {
		m_nonEmptySources = new HashMap<>();
		sourceColumnNames.forEach(n -> m_nonEmptySources.put(n, m_nonEmptySources.size()));
		m_cardinality = cardinality;
	}

	void readCorrelationsTable(final Iterator<DataRow> iter) {
		while (iter.hasNext()) {
			final DataRow row = iter.next();
			CheckUtils.checkState(row.getNumCells() == 2,
					"The correlations table fed to CorrelationGraphHandler may have only 2 columns.");
			m_correlations.add(new int[] { getSourceIdx(row.getCell(0)), getSourceIdx(row.getCell(1)) });
		}
	}

	private int getSourceIdx(final DataCell cell) {
        CheckUtils.checkArgument(!cell.isMissing(), "Missing values are not supported in the correlations table.");
        final String name = cell.toString();
        final Integer index = m_nonEmptySources.get(name);
        CheckUtils.checkArgument(index != null, "Unknown noisy label column %s", name);
        @SuppressWarnings("null") // the check in in the previous line ensures that index is not null
        final int idx = index.intValue();
        return idx;
    }

	boolean[][] buildMask() {
        final Graph<CliqueVertex<Integer>, SeparatorEdge<Integer>> cliqueTree = getCliqueTree();
        final CliqueHelper[] cliqueHelpers = createCliqueHelpers(cliqueTree);
        return buildMask(cliqueHelpers);
    }


	private Graph<CliqueVertex<Integer>, SeparatorEdge<Integer>> getCliqueTree() {
		final Graph<Integer, DefaultEdge> depencyGraph = GraphUtil
				.createGraph(IntStream.range(0, getNumSources()).toArray(), m_correlations);
		return GraphUtil.getCliqueTree(depencyGraph);
	}

	private boolean[][] buildMask(final CliqueHelper[] cliqueHelpers) {
		final int d = getOneHotDimension();
		final boolean[][] mask = new boolean[d][d];
		for (int i = 0; i < d; i++) {
			Arrays.fill(mask[i], true);
		}
		for (CliqueHelper ci : cliqueHelpers) {
			for (CliqueHelper cj : cliqueHelpers) {
				if (areInSameClique(ci, cj)) {
					maskOut(ci, cj, mask);
				}
			}
		}
		return mask;
	}

	private int getOneHotDimension() {
		return m_cardinality * getNumSources();
	}

	private int getNumSources() {
		return m_nonEmptySources.size();
	}

	private CliqueHelper[] createCliqueHelpers(final Graph<CliqueVertex<Integer>, SeparatorEdge<Integer>> cliqueTree) {
		return IntStream.range(0, getNumSources()).mapToObj(i -> createCliqueHelper(i, cliqueTree))
				.toArray(CliqueHelper[]::new);
	}

	private CliqueHelper createCliqueHelper(final int i,
			final Graph<CliqueVertex<Integer>, SeparatorEdge<Integer>> cliqueTree) {
		final int cardinality = m_cardinality;
		final int startIdx = i * cardinality;
		final int endIdx = (i + 1) * cardinality;
		final Set<CliqueVertex<Integer>> cliques = getCliquesContaining(i, cliqueTree);
		return new CliqueHelper(startIdx, endIdx, cliques);
	}

	private static void maskOut(final CliqueHelper ci, final CliqueHelper cj, final boolean[][] mask) {
		maskOut(ci.m_startIndex, ci.m_endIndex, cj.m_startIndex, cj.m_endIndex, mask);
		maskOut(cj.m_startIndex, cj.m_endIndex, ci.m_startIndex, ci.m_endIndex, mask);
	}

	private static void maskOut(final int rowStart, final int rowEnd, final int colStart, final int colEnd,
			final boolean[][] mask) {
		for (int i = rowStart; i < rowEnd; i++) {
			Arrays.fill(mask[i], colStart, colEnd, false);
		}
	}

	private static Set<CliqueVertex<Integer>> getCliquesContaining(final Integer i,
			final Graph<CliqueVertex<Integer>, SeparatorEdge<Integer>> cliqueTree) {
		return cliqueTree.vertexSet().stream().filter(v -> v.getMembers().contains(i)).collect(Collectors.toSet());
	}

	private static boolean areInSameClique(final CliqueHelper ci, final CliqueHelper cj) {
		return !Sets.intersection(ci.m_maxCliques, cj.m_maxCliques).isEmpty();
	}

	private static final class CliqueHelper {
		private final int m_startIndex;

		private final int m_endIndex;

		private final Set<CliqueVertex<Integer>> m_maxCliques;

		CliqueHelper(final int startIdx, final int endIdx, final Set<CliqueVertex<Integer>> maxCliques) {
			m_startIndex = startIdx;
			m_endIndex = endIdx;
			m_maxCliques = maxCliques;
		}

	}

}
