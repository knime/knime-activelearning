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
 *   Sep 6, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.wsl.weaklabelmodel;

import javax.swing.JComponent;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;

/**
 * Port object for weak label models.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class WeakLabelModelPortObject extends AbstractSimplePortObject {

    /**
     * @noreference This class is not intended to be referenced by clients.
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public static final class Serializer extends AbstractSimplePortObjectSerializer<WeakLabelModelPortObject> { }

    /**
     * Type of this kind of port object.
     */
    @SuppressWarnings("hiding") // the hiding is intentional
    public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(WeakLabelModelPortObject.class);

    private WeakLabelModelPortObjectSpec m_spec;

    private WeakLabelModelContent m_content;

    /**
     * @param spec the {@link WeakLabelModelPortObjectSpec} spec of this model
     * @param content the content of this model
     */
    public WeakLabelModelPortObject(final WeakLabelModelPortObjectSpec spec, final WeakLabelModelContent content) {
        m_spec = spec;
        m_content = content;
    }

    /**
     * Framework constructor for serialization.
     * @noreference Do not use in node development
     */
    public WeakLabelModelPortObject() {
        // Framework constructor
    }

    /**
     * @return the model content
     */
    public WeakLabelModelContent getContent() {
        return m_content;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSummary() {
        return m_spec.getTrainingSpec().toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WeakLabelModelPortObjectSpec getSpec() {
        return m_spec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        return new JComponent[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model, final ExecutionMonitor exec) throws CanceledExecutionException {
        m_content.save(model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final ModelContentRO model, final PortObjectSpec spec, final ExecutionMonitor exec)
        throws InvalidSettingsException, CanceledExecutionException {
        m_spec = (WeakLabelModelPortObjectSpec)spec;
        m_content = WeakLabelModelContent.load(model);
    }


}
