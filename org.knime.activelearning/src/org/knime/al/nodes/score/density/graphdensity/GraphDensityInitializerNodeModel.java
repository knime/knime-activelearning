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
package org.knime.al.nodes.score.density.graphdensity;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.al.nodes.score.density.AbstractDensityInitializerNodeModel;
import org.knime.al.nodes.score.density.DensityScorerModelCreator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.util.CheckUtils;

/**
 * Node model of the Graph Density Initializer node.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class GraphDensityInitializerNodeModel extends AbstractDensityInitializerNodeModel {

    private final SettingsModelInteger m_nrNeighbors = createNumNeighborsModel();

    private final SettingsModelDouble m_sigma = createSigmaModel();

    /**
     * @return Settings model for the number of Neighbors
     */
    static SettingsModelInteger createNumNeighborsModel() {
        return new SettingsModelInteger("nrNeighbors", 5);
    }

    /**
     * @return Settings model for the Sigma
     */
    static SettingsModelDouble createSigmaModel() {
        return new SettingsModelDouble("sigma", 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DensityScorerModelCreator createBuilder(final int nrFeatures) {
        return new GraphDensityScorerModelCreator(nrFeatures, m_sigma.getDoubleValue(), m_nrNeighbors.getIntValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<SettingsModel> getSettingsModels() {
        return Stream.of(m_nrNeighbors, m_sigma).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * @throws InvalidSettingsException
     */
    @Override
    protected void checkInputTable(final BufferedDataTable table) throws InvalidSettingsException {
        final int nrNeighbors = m_nrNeighbors.getIntValue();
        CheckUtils.checkSetting(table.size() > nrNeighbors,
            "The table must have at least %s rows (the specified number of neighbors + 1).", nrNeighbors + 1);
    }

}
