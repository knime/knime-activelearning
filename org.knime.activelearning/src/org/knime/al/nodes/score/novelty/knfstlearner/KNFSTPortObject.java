/*
 * ------------------------------------------------------------------------
 *
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 * --------------------------------------------------------------------- *
 *
 */

package org.knime.al.nodes.score.novelty.knfstlearner;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.ZipEntry;

import javax.swing.JComponent;

import org.knime.al.util.noveltydetection.knfst.KNFST;
import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.data.util.NonClosableOutputStream;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.AbstractPortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;

public class KNFSTPortObject extends AbstractPortObject {

    public static final class Serializer
            extends AbstractPortObjectSerializer<KNFSTPortObject> {
    }

    /**
     * Define port type of objects of this class when used as PortObjects.
     */
    public static final PortType TYPE =
            PortTypeRegistry.getInstance().getPortType(KNFSTPortObject.class);

    private static final String SUMMARY =
            "Kernel Null Foley Sammon Transformation Object for novelty scoring";

    private KNFST m_knfstModel;
    private KNFSTPortObjectSpec m_spec;

    public KNFSTPortObject() {
    }

    public KNFSTPortObject(final KNFST knfst, final KNFSTPortObjectSpec spec) {
        m_knfstModel = knfst;
        m_spec = spec;
    }

    public KNFST getKNFST() {
        return m_knfstModel;
    }

    // public static class KNFSTPortObjectSerializer extends
    // PortObjectSerializer<KNFSTPortObject> {
    //
    // @Override
    // public void savePortObject(final KNFSTPortObject portObject, final
    // PortObjectZipOutputStream out, final ExecutionMonitor exec) throws
    // IOException,
    // CanceledExecutionException {
    // portObject.save(out, exec);
    //
    // }
    //
    // @Override
    // public KNFSTPortObject loadPortObject(final PortObjectZipInputStream in,
    // final PortObjectSpec spec, final ExecutionMonitor exec) throws
    // IOException,
    // CanceledExecutionException {
    //
    // final KNFSTPortObject po = new KNFSTPortObject();
    // po.load(in, spec, exec);
    //
    // return po;
    // }
    //
    // }

    @Override
    protected void save(final PortObjectZipOutputStream out,
            final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
        ObjectOutputStream oo = null;
        try {
            out.putNextEntry(new ZipEntry("knfst.objectout"));
            oo = new ObjectOutputStream(new NonClosableOutputStream.Zip(out));
            oo.writeUTF(m_knfstModel.getClass().getName());
            m_knfstModel.writeExternal(oo);
        } catch (final IOException ioe) {

        } finally {
            if (oo != null) {
                try {
                    oo.close();
                } catch (final Exception e) {

                }
            }
        }

    }

    @Override
    protected void load(final PortObjectZipInputStream in,
            final PortObjectSpec spec, final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
        ObjectInputStream oi = null;
        KNFST knfst = null;
        try {
            // load classifier
            final ZipEntry zentry = in.getNextEntry();
            assert zentry.getName().equals("knfst.objectout");
            oi = new ObjectInputStream(new NonClosableInputStream.Zip(in));
            knfst = (KNFST) Class.forName(oi.readUTF()).newInstance();
            knfst.readExternal(oi);
        } catch (final IOException ioe) {

        } catch (final ClassNotFoundException cnf) {

        } catch (final InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        } finally {
            if (oi != null) {
                try {
                    oi.close();
                } catch (final Exception e) {

                }
            }
        }
        m_knfstModel = knfst;
        m_spec = (KNFSTPortObjectSpec) spec;
    }

    @Override
    public String getSummary() {
        return SUMMARY;
    }

    @Override
    public KNFSTPortObjectSpec getSpec() {
        return m_spec;
    }

    @Override
    public JComponent[] getViews() {
        return new JComponent[] {};
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((m_knfstModel == null) ? 0 : m_knfstModel.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof KNFSTPortObject)) {
            return false;
        }
        final KNFSTPortObject other = (KNFSTPortObject) obj;
        if (m_knfstModel == null) {
            if (other.m_knfstModel != null) {
                return false;
            }
        } else if (!m_knfstModel.equals(other.m_knfstModel)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "KNFSTPortObject [m_knfstModel=" + m_knfstModel + "]";
    }

}
