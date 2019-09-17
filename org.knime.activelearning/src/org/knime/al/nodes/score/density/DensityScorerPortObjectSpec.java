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
 *   Aug 2, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.al.nodes.score.density;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObjectSpec;
import org.knime.core.node.util.CheckUtils;

/**
 * Spec for a {@link DensityScorerPortObject}. Holds a {@link DataTableSpec} specifying the columns used to create the
 * underlying model.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class DensityScorerPortObjectSpec extends AbstractSimplePortObjectSpec {

    /**
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public static final class Serializer extends AbstractSimplePortObjectSpecSerializer<DensityScorerPortObjectSpec> {

    }

    private DataTableSpec m_featureSpec;

    /**
     * @param featureSpec the {@link DataTableSpec} containing only the features used during model creation
     */
    public DensityScorerPortObjectSpec(final DataTableSpec featureSpec) {
        m_featureSpec = featureSpec;
    }

    /**
     * Serialization constructor.
     */
    public DensityScorerPortObjectSpec() {
        // serialization constructor
    }

    /**
     * @return {@link DataTableSpec} containing only the features used during model creation
     */
    public DataTableSpec getFeatureSpec() {
        return m_featureSpec;
    }

    /**
     * Checks whether {@link DataTableSpec tableSpec} is compatible with this model spec i.e. all feature columns are
     * contained and have the correct type.
     *
     * @param tableSpec the spec of the table on which to apply the density model
     * @throws InvalidSettingsException if any feature column is missing or of the wrong type
     */
    public void checkCompatibility(final DataTableSpec tableSpec) throws InvalidSettingsException {
        for (final DataColumnSpec featureCol : m_featureSpec) {
            final DataColumnSpec tableCol = tableSpec.getColumnSpec(featureCol.getName());
            CheckUtils.checkSetting(tableCol != null,
                "The input table does not contain the required feature column %s.", featureCol);
            @SuppressWarnings("null") // tableCol is explicitly checked to NOT be null above
            DataType type = tableCol.getType();
            CheckUtils.checkSetting(featureCol.getType().isASuperTypeOf(type),
                "The input table contains a column with incompatible type for feature %s. Expected %s but received %s.",
                featureCol.getName(), featureCol.getType(), type);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model) {
        m_featureSpec.save(model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final ModelContentRO model) throws InvalidSettingsException {
        m_featureSpec = DataTableSpec.load(model);
    }

}
