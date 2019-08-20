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
 * Created on 11.03.2013 by dietyc
 */
package org.knime.al.nodes.score.density.nodepotential;

import java.util.Collections;
import java.util.List;

import org.knime.al.nodes.score.density.AbstractDensityInitializerNodeModel;
import org.knime.al.nodes.score.density.DensityScorerModelCreator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class PotentialInitializerNodeModel extends AbstractDensityInitializerNodeModel {

    private final SettingsModelDouble m_radiusAlphaModel = createConstRAlphaModel();

    /**
     * @return Settings model to store the value of the constant R Alpha.
     */
    static SettingsModelDouble createConstRAlphaModel() {
        return new SettingsModelDoubleBounded("radiusAlpha", 0.4, 0, 100);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DensityScorerModelCreator createBuilder(final int nrFeatures) {
        return new PotentialDensityScorerModelCreator(nrFeatures, m_radiusAlphaModel.getDoubleValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<SettingsModel> getSettingsModels() {
        return Collections.singletonList(m_radiusAlphaModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkInputTable(final BufferedDataTable table) throws InvalidSettingsException {
        // nothing to check
    }

}
