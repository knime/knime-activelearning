package org.knime.wsl.labelmodel;

import static org.junit.Assert.assertEquals;
import static org.knime.wsl.testing.TestUtil.ia;

import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knime.wsl.labelmodel.GraphUtil.CliqueVertex;
import org.knime.wsl.labelmodel.GraphUtil.SeparatorEdge;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 *
 */
public class GraphUtilTest {

    private static Graph<Integer, DefaultEdge> GRAPH;

    private static Set<Set<Integer>> EXPECTED_CLIQUES;

    @BeforeClass
    public static void initClass() {
        GRAPH = GraphUtil.createGraph(ia(1, 2, 3, 4, 5, 6, 7, 8, 9),
            Lists.newArrayList(ia(1, 2), ia(1, 3), ia(2, 3), ia(2, 4), ia(3, 4),
                ia(3, 5), ia(3, 6), ia(4, 5), ia(4, 6), ia(5, 6), ia(7, 8)));
        EXPECTED_CLIQUES = set(set(1, 2, 3), set(2, 3, 4), set(3, 4, 5, 6), set(7, 8), set(9));
    }

    @SafeVarargs
    private static <V> Set<V> set(final V... values) {
        return Sets.newHashSet(values);
    }

    @Test
    public void testGetChordalGraphCliques() throws Exception {
        final Set<Set<Integer>> cliques = GraphUtil.getChordalGraphCliques(GRAPH);
        assertEquals(EXPECTED_CLIQUES, cliques);
    }

    @Test
    public void testGetCliqueGraph() throws Exception {
        Graph<CliqueVertex<Integer>, SeparatorEdge<Integer>> cliqueGraph = GraphUtil.getCliqueGraph(GRAPH);
        assertEquals(EXPECTED_CLIQUES,
            cliqueGraph.vertexSet().stream().map(CliqueVertex::getMembers).collect(Collectors.toSet()));
        final Set<Set<Integer>> expectedEdges = set(set(2, 3), set(3, 4), set(3));
        assertEquals(expectedEdges,
            cliqueGraph.edgeSet().stream().map(SeparatorEdge::getMembers).collect(Collectors.toSet()));
    }

    @Test
    public void testGetCliqueTree() throws Exception {
        Graph<CliqueVertex<Integer>, SeparatorEdge<Integer>> cliqueTree = GraphUtil.getCliqueTree(GRAPH);
        assertEquals(EXPECTED_CLIQUES,
            cliqueTree.vertexSet().stream().map(CliqueVertex::getMembers).collect(Collectors.toSet()));
        final Set<Set<Integer>> expectedEdges = set(set(3, 4), set(3));
        assertEquals(expectedEdges,
            cliqueTree.edgeSet().stream().map(SeparatorEdge::getMembers).collect(Collectors.toSet()));
    }
}
