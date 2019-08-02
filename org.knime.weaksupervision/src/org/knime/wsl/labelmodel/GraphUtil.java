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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.Graph;
import org.jgrapht.GraphTests;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.interfaces.SpanningTreeAlgorithm.SpanningTree;
import org.jgrapht.alg.spanning.KruskalMinimumSpanningTree;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.knime.core.node.util.CheckUtils;

import com.google.common.collect.Sets;

/**
 * Utility class for graph related algorithms.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany, KNIME GmbH, Konstanz, Germany
 */
final class GraphUtil {

    private GraphUtil() {
        // static utility class
    }

    static final class SeparatorEdge<V> {
        private final Set<V> m_members;

        private SeparatorEdge(final Set<V> members) {
            m_members = new HashSet<>(members);
        }

        Set<V> getMembers() {
            return Collections.unmodifiableSet(m_members);
        }

        double getWeight() {
            return m_members.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return String.format("{Weight: %s, Members: [%s]}", getWeight(),
                m_members.stream().map(Object::toString).collect(Collectors.joining(", ")));
        }
    }

    static final class CliqueVertex<V> {
        private final Set<V> m_members;

        private CliqueVertex(final Set<V> members) {
            m_members = new HashSet<>(members);
        }

        Set<V> getMembers() {
            return Collections.unmodifiableSet(m_members);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return String.format("{[%s]}", m_members.stream().map(Object::toString).collect(Collectors.joining(", ")));
        }

    }

    /**
     * Creates a clique tree for <b>graph</b> i.e. each vertex in the tree represents a clique and the edges represent
     * the separators i.e. the vertices that are common to two cliques.
     *
     * @param graph to create the clique tree for. Must be chordal.
     * @return the clique tree (also called junction tree)
     */
    static <V> Graph<CliqueVertex<V>, SeparatorEdge<V>> getCliqueTree(final Graph<V, DefaultEdge> graph) {
        final Graph<CliqueVertex<V>, SeparatorEdge<V>> cliqueGraph = getCliqueGraph(graph);
        KruskalMinimumSpanningTree<CliqueVertex<V>, SeparatorEdge<V>> minimumSpanningTree =
            new KruskalMinimumSpanningTree<>(cliqueGraph);
        SpanningTree<SeparatorEdge<V>> spanningTree = minimumSpanningTree.getSpanningTree();
        return new AsSubgraph<>(cliqueGraph, cliqueGraph.vertexSet(), spanningTree.getEdges());
    }

    static <V> Graph<CliqueVertex<V>, SeparatorEdge<V>> getCliqueGraph(final Graph<V, ?> graph) {
        CheckUtils.checkArgument(GraphTests.isChordal(graph), "Only chordal graphs are supported.");
        return getCliqueGraph(getChordalGraphCliques(graph));
    }

    private static <V> Graph<CliqueVertex<V>, SeparatorEdge<V>> getCliqueGraph(final Set<Set<V>> cliques) {
        Graph<CliqueVertex<V>, SeparatorEdge<V>> cliqueGraph = new SimpleWeightedGraph<>(SeparatorEdge.class);
        cliques.stream().map(CliqueVertex::new).forEach(cliqueGraph::addVertex);

        for (CliqueVertex<V> i : cliqueGraph.vertexSet()) {
            for (CliqueVertex<V> j : cliqueGraph.vertexSet()) {
                addSeparatorEdge(cliqueGraph, i, j);
            }
        }
        return cliqueGraph;
    }

    private static <V> void addSeparatorEdge(final Graph<CliqueVertex<V>, SeparatorEdge<V>> cliqueGraph,
        final CliqueVertex<V> i, final CliqueVertex<V> j) {
        final Set<V> s = Sets.intersection(i.getMembers(), j.getMembers());
        if (!s.isEmpty() && !i.equals(j)) {
            final SeparatorEdge<V> edge = new SeparatorEdge<>(s);
            if (cliqueGraph.addEdge(i, j, edge)) {
                cliqueGraph.setEdgeWeight(edge, edge.getWeight());
            }
        }
    }

    /**
     * This method extracts all maximal cliques from a chordal graph. The implementation is similar to that in the
     * python package networkx.
     *
     * @param graph to search for cliques. Must be chordal.
     * @return a set of cliques where each clique is represented as set of vertices
     */
    static <V> Set<Set<V>> getChordalGraphCliques(final Graph<V, ?> graph) {
        ConnectivityInspector<V, ?> connectivityInspector = new ConnectivityInspector<>(graph);
        Set<Set<V>> cliques = new HashSet<>();
        for (Set<V> connected : connectivityInspector.connectedSets()) {
            Graph<V, ?> connectedComponent = new AsSubgraph<>(graph, connected);
            cliques.addAll(getConnectedChordalGraphCliques(connectedComponent));
        }
        return cliques;
    }

    private static <V> Set<Set<V>> getConnectedChordalGraphCliques(final Graph<V, ?> graph) {
        if (graph.vertexSet().size() == 1) {
            return Collections.singleton(graph.vertexSet());
        }

        final Set<Set<V>> cliques = new HashSet<>();
        final Set<V> unnumbered = new HashSet<>(graph.vertexSet());
        final Set<V> numbered = new HashSet<>();
        V v = unnumbered.iterator().next();
        unnumbered.remove(v);
        numbered.add(v);
        Set<V> cliqueWannaBe = new HashSet<>(numbered);
        while (!unnumbered.isEmpty()) {
            v = maxCardinalityNode(graph, unnumbered, numbered);
            unnumbered.remove(v);
            numbered.add(v);
            Set<V> newCliqueWannaBe = intersection(Graphs.neighborSetOf(graph, v), numbered);
            Graph<V, ?> sg = new AsSubgraph<>(graph, cliqueWannaBe);
            CheckUtils.checkArgument(GraphTests.isComplete(sg), "Input graph is not chordal.");
            newCliqueWannaBe.add(v);
            if (!isSuperSet(newCliqueWannaBe, cliqueWannaBe)) {
                cliques.add(cliqueWannaBe);
            }
            cliqueWannaBe = newCliqueWannaBe;
        }
        cliques.add(cliqueWannaBe);
        return cliques;
    }

    private static boolean isSuperSet(final Set<?> set1, final Set<?> set2) {
        return set1.size() >= set2.size() && set2.stream().allMatch(set1::contains);
    }

    private static <V> Set<V> intersection(final Set<V> set1, final Set<V> set2) {
        return set1.stream().filter(set2::contains).collect(Collectors.toSet());
    }

    private static <V> V maxCardinalityNode(final Graph<V, ?> graph, final Set<V> choices, final Set<V> wannaConnect) {
        long maxNumber = -1;
        V maxCardinalityNode = null;
        for (V x : choices) {
            final long number = Sets.intersection(Graphs.neighborSetOf(graph, x), wannaConnect).size();
            if (number > maxNumber) {
                maxNumber = number;
                maxCardinalityNode = x;
            }
        }
        return maxCardinalityNode;
    }

    static Graph<Integer, DefaultEdge> createGraph(final int[] vertices, final List<int[]> edges) {
        Graph<Integer, DefaultEdge> g = new SimpleGraph<>(DefaultEdge.class);
        for (Integer v : vertices) {
            g.addVertex(v);
        }

        for (final int[] edge : edges) {
            g.addEdge(edge[0], edge[1]);
        }
        return g;
    }
}
