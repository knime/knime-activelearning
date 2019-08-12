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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.Collections;

import javax.swing.JComponent;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStorePortObject;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;

/**
 * PortObject that encapsulate a {@link DensityScorerModel}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class DensityScorerPortObject extends FileStorePortObject {

    /**
     * Serializer for {@link DensityScorerPortObject}.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public static final class Serializer extends PortObjectSerializer<DensityScorerPortObject> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void savePortObject(final DensityScorerPortObject portObject, final PortObjectZipOutputStream out,
            final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
            ModelContent mc = new ModelContent(CFG_MODELCONTENT);
            mc.addInt(CFG_NUM_FEATURES, portObject.m_nrFeatures);
            mc.addInt(CFG_NUM_DATA_POINTS, portObject.m_nrRows);
            mc.saveToXML(out);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DensityScorerPortObject loadPortObject(final PortObjectZipInputStream in, final PortObjectSpec spec,
            final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
            final DensityScorerPortObject portObject = new DensityScorerPortObject();
            portObject.m_spec = (DensityScorerPortObjectSpec)spec;
            portObject.m_modelRef = new WeakReference<>(null);
            ModelContentRO mc = ModelContent.loadFromXML(in);
            try {
                portObject.m_nrFeatures = mc.getInt(CFG_NUM_FEATURES);
                portObject.m_nrRows = mc.getInt(CFG_NUM_DATA_POINTS);
            } catch (InvalidSettingsException ise) {
                IOException ioe = new IOException("Unable to restore meta information: " + ise.getMessage());
                ioe.initCause(ise);
                throw ioe;
            }
            return portObject;
        }
    }

    /**
     * Type of this port object class.
     */
    @SuppressWarnings("hiding") // the hiding is intended
    public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(DensityScorerPortObject.class);

    private static final String CFG_NUM_FEATURES = "numFeatures";

    private static final String CFG_NUM_DATA_POINTS = "numDataPoints";

    private static final String CFG_MODELCONTENT = "modelContent";

    private DensityScorerPortObjectSpec m_spec;

    private WeakReference<DensityScorerModel> m_modelRef;

    private int m_nrFeatures;

    private int m_nrRows;

    /**
     *
     */
    private DensityScorerPortObject(final DensityScorerPortObjectSpec spec, final DensityScorerModel model,
        final FileStore fileStore) {
        super(Collections.singletonList(fileStore));
        m_spec = spec;
        m_modelRef = new WeakReference<>(model);
        m_nrFeatures = spec.getFeatureSpec().getNumColumns();
        m_nrRows = model.getNrRows();
    }

    /**
     * Framework constructor, not to be used by node code.
     */
    public DensityScorerPortObject() {
        // no op, load method to be called by the framework
    }

    /**
     * @param spec the model spec
     * @param model the {@link DensityScorerModel}
     * @param fileStore to store <b>model</b> in
     * @return a {@link DensityScorerPortObject} that wraps <b>model</b>
     */
    public static DensityScorerPortObject createPortObject(final DensityScorerPortObjectSpec spec,
        final DensityScorerModel model, final FileStore fileStore) {
        final DensityScorerPortObject po = new DensityScorerPortObject(spec, model, fileStore);
        try {
            serialize(model, fileStore);
        } catch (IOException e) {
            throw new IllegalStateException("Model serialization failed.", e);
        }
        return po;
    }

    /**
     * @return the {@link DensityScorerModel}
     */
    public synchronized DensityScorerModel getModel() {
        DensityScorerModel model = m_modelRef.get();
        if (model == null) {
            try {
                model = deserialize();
            } catch (IOException | ClassNotFoundException e) {
                throw new IllegalStateException("Deserialization failed.", e);
            }
            m_modelRef = new WeakReference<>(model);
        }
        return model;
    }

    private DensityScorerModel deserialize() throws IOException, ClassNotFoundException {
        final File file = getFileStore(0).getFile();
        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(file))) {
            return (DensityScorerModel)input.readObject();
        }
    }

    private static void serialize(final DensityScorerModel model, final FileStore fileStore)
        throws IOException {
        final File file = fileStore.getFile();
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(model);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSummary() {
        return String.format("Density Scorer Model consisting of %s %s-dimensional data points.", m_nrRows,
            m_nrFeatures);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DensityScorerPortObjectSpec getSpec() {
        return m_spec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        return new JComponent[0];
    }

}
