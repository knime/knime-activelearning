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
 * History
 *   Jun 29, 2015 (gabriel): created
 */
package org.knime.al.nodes;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortType;

/**
 * Abstract superclass for NodeModels, contains default implementations for
 * commonly used methods.
 *
 * @author gabriel
 */
public abstract class AbstractALNodeModel extends NodeModel {

    /**
     * Instantiates a new abstract al node model.
     *
     * @param nrInDataPorts
     *            the number of in ports
     * @param nrOutDataPorts
     *            the number of out ports
     * @see NodeModel#NodeModel(int, int)
     */
    protected AbstractALNodeModel(final int nrInDataPorts,
            final int nrOutDataPorts) {
        super(nrInDataPorts, nrOutDataPorts);
    }

    /**
     * Instantiates a new abstract al node model.
     *
     * @param inPortTypes
     *            Input Port Types
     * @param outPortTypes
     *            Output Port Types
     * @see NodeModel#NodeModel(PortType[], PortType[])
     */
    protected AbstractALNodeModel(final PortType[] inPortTypes,
            final PortType[] outPortTypes) {
        super(inPortTypes, outPortTypes);
    }

    /** The list of settingsmodels. */
    protected List<SettingsModel> m_settingsModels;

    /**
     * Creates a list of Settings Models.
     *
     * @return a list of the {@link SettingsModel}s used in this node.
     */
    protected abstract List<SettingsModel> collectSettingsModels();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settingsModels = collectSettingsModels();
        for (final SettingsModel model : m_settingsModels) {
            model.saveSettingsTo(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settingsModels = collectSettingsModels();
        for (final SettingsModel model : m_settingsModels) {
            model.validateSettings(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settingsModels = collectSettingsModels();
        for (final SettingsModel model : m_settingsModels) {
            model.loadSettingsFrom(settings);
        }
    }

    /**
     * No-op version of {@link NodeModel#loadInternals}.
     *
     * @param nodeInternDir
     *            the node intern dir
     * @param exec
     *            the exec
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws CanceledExecutionException
     *             the canceled execution exception
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
        // nothing to do here
    }

    /**
     * No-op version of {@link NodeModel#saveInternals}.
     *
     * @param nodeInternDir
     *            the node intern dir
     * @param exec
     *            the exec
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws CanceledExecutionException
     *             the canceled execution exception
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
        // nothing to do here
    }

}
