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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
package org.knime.al.nodes.score.uncertainty.leastconfident;

import java.util.Arrays;
import java.util.List;

import org.knime.al.nodes.score.uncertainty.AbstractUncertaintyNodeModel;
import org.knime.al.nodes.score.uncertainty.entropy.EntropyScorerNodeModel;
import org.knime.al.util.MathUtils;
import org.knime.al.util.NodeTools;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;

/**
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public class LeastConfidentScorerNodeModel
        extends AbstractUncertaintyNodeModel {

    /**
     * {@inheritDoc}
     */
    @Override
    protected ColumnRearranger createResRearranger(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        final ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        final DataColumnSpec newColSpec =
                new DataColumnSpecCreator("Least Confident Score",
                        DoubleCell.TYPE).createSpec();

        final List<Integer> selectedColumns =
                NodeTools.getIndicesFromFilter(inSpec, m_columnFilterModel,
                        DoubleValue.class, EntropyScorerNodeModel.class);

        if (selectedColumns.size() < 2) {
            throw new InvalidSettingsException(
                    "Need at least 2 DoubleType columns!");
        }
        // utility object that calculates the difference between top scoring
        // class predictions
        rearranger.append(new SingleCellFactory(newColSpec) {
            @Override
            public DataCell getCell(final DataRow row) {
                final double[] values =
                        NodeTools.toDoubleArray(row, selectedColumns);
                Arrays.sort(values);
                // make sure the distribution is valid,
                if (!MathUtils.checkDistribution(values)) {
                    AbstractUncertaintyNodeModel
                            .handleInvalidDistribution(row.getKey());
                }

                // to make this node compatible with the other uncertainty
                // scorers we invert the value.
                return new DoubleCell(1 - (values[values.length - 1]));
            }
        });
        return rearranger;
    }
}
