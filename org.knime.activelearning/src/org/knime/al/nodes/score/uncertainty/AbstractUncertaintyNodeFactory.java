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
 *   Jun 28, 2015 (gabriel): created
 */
package org.knime.al.nodes.score.uncertainty;

import org.knime.al.nodes.score.ExceptionHandling;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NodeView;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;

/**
 * Abstract superclass for the Exploitation scorer nodes, contains all the common methods. Subclasses only need to
 * implement the {@link #createNodeModel()} method.
 *
 * @author gabriel
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @param <T>
 */
public abstract class AbstractUncertaintyNodeFactory<T extends AbstractUncertaintyNodeModel> extends NodeFactory<T> {

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract T createNodeModel();

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<T> createNodeView(final int viewIndex, final T nodeModel) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new DefaultNodeSettingsPane() {

            private DataTableSpec m_spec;

            private SettingsModelColumnFilter2 m_createColumnFilterModel =
                AbstractUncertaintyNodeModel.createColumnFilterModel();

            {
                createNewGroup("Column Selection");
                addDialogComponent(new DialogComponentColumnFilter2(m_createColumnFilterModel, 0, false));

                createNewGroup("Output Settings");
                addDialogComponent(new DialogComponentString(AbstractUncertaintyNodeModel.createColumnNameModel(null),
                    "Output column name ", true, 17));

                // placeholder
                addDialogComponent(new DialogComponentLabel(""));

                createNewGroup("Invalid Input Handling");
                addDialogComponent(
                    new DialogComponentButtonGroup(AbstractUncertaintyNodeModel.createExceptionHandlingModel(),
                        null, true, ExceptionHandling.values()));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
                if (m_spec != null && m_createColumnFilterModel.applyTo(m_spec).getIncludes().length < 2) {
                    throw new InvalidSettingsException("At least two columns must be included.");
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
                throws NotConfigurableException {
                m_spec = specs[0];
            }
        };
    }

}
