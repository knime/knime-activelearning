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
 */
package org.knime.wsl.labelmodel;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;

import org.knime.core.node.util.CheckUtils;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session.Runner;
import org.tensorflow.Tensor;

/**
 * Wraps a SavedModelBundle and allows to perform simple interactions with it including setting and fetching specific
 * tensors in the SavedModel.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany, KNIME GmbH, Konstanz, Germany
 */
final class SavedModelAdapter implements AutoCloseable {

    private final SavedModelBundle m_savedModel;

    SavedModelAdapter(final String exportDir, final String... tags) {
        m_savedModel = SavedModelBundle.load(exportDir, tags);
    }

    @Override
    public void close() throws Exception {
        m_savedModel.close();
    }

    void set(final String placeHolderId, final String assignerId, final float scalar) {
        try (Tensor<Float> tensor = Tensor.create(scalar, Float.class)) {
            set(placeHolderId, assignerId, tensor);
        }
    }

    void set(final String placeHolderId, final String assignerId, final float[] vector) {
        try (Tensor<Float> tensor = Tensor.create(vector, Float.class)) {
            set(placeHolderId, assignerId, tensor);
        }
    }

    void set(final String placeHolderId, final String assignerId, final float[][] matrix) {
        try (Tensor<Float> tensor = Tensor.create(matrix, Float.class)) {
            set(placeHolderId, assignerId, tensor);
        }
    }

    void set(final String placeHolderId, final String assignerId, final boolean[][] matrix) {
        try (Tensor<Boolean> tensor = Tensor.create(matrix, Boolean.class)) {
            set(placeHolderId, assignerId, tensor);
        }
    }

    float getFloatScalar(final String id) {
        try (Tensor<Float> result = fetch(id, Float.class)) {
            return result.floatValue();
        }
    }

    float[][] getFloatMatrix(final String id) {
        try (final Tensor<Float> result = fetch(id, Float.class)) {
            return toFloatMatrix(result);
        }
    }

    float[][] getFloatMatrix(final String feedId, final float[][] matrix, final String fetchId) {
        try (final Tensor<Float> tensor = Tensor.create(matrix, Float.class);
                Tensor<Float> result = fetch(feedId, tensor, fetchId, Float.class)) {
            return toFloatMatrix(result);
        }
    }

    float[] getFloatVector(final String id) {
        try (final Tensor<Float> result = fetch(id, Float.class)) {
            CheckUtils.checkState(result.shape().length == 1,
                "The requested tensor is not a vector but a tensor with shape %s", Arrays.toString(result.shape()));
            final FloatBuffer buffer = FloatBuffer.allocate(result.numElements());
            result.writeTo(buffer);
            CheckUtils.checkState(buffer.hasArray(), "The float buffer is not backed by an array.");
            return buffer.array();
        }
    }

    boolean[][] getBooleanMatrix(final String id) {
        try (final Tensor<Boolean> result = fetch(id, Boolean.class)) {
            return toBooleanMatrix(result);
        }
    }

    private static float[][] toFloatMatrix(final Tensor<Float> tensor) {
        final FloatBuffer buffer = FloatBuffer.allocate(tensor.numElements());
        final long[] shape = tensor.shape();
        CheckUtils.checkState(shape.length == 2, "The requested tensor is not a matrix but a tensor with shape %s.",
            Arrays.toString(shape));
        tensor.writeTo(buffer);
        buffer.flip();
        final float[][] array = new float[(int)shape[0]][(int)shape[1]];
        for (int i = 0; i < shape[0]; i++) {
            for (int j = 0; j < shape[1]; j++) {
                array[i][j] = buffer.get();
            }
        }
        return array;
    }

    private static boolean[][] toBooleanMatrix(final Tensor<Boolean> tensor) {
        final ByteBuffer buffer = ByteBuffer.allocate(tensor.numElements());
        final long[] shape = tensor.shape();
        CheckUtils.checkState(shape.length == 2, "The requested tensor is not a matrix but a tensor with shape %s.",
                Arrays.toString(shape));
        tensor.writeTo(buffer);
        buffer.flip();
        final boolean[][] array = new boolean[(int)shape[0]][(int)shape[1]];
        for (int i = 0; i < shape[0]; i++) {
            for (int j = 0; j < shape[1]; j++) {
                array[i][j] = buffer.get() == 1;
            }
        }
        return array;
    }

    private <V> Tensor<V> fetch(final String feedId, final Tensor<?> tensor, final String fetchId,
        final Class<V> expected) {
        final List<Tensor<?>> result = getRunner().feed(feedId, tensor).fetch(fetchId).run();
        CheckUtils.checkState(result.size() == 1, "Fetched only a single tensor but received multiple results.");
        return result.get(0).expect(expected);
    }

    private <V> Tensor<V> fetch(final String id, final Class<V> expected) {
        final List<Tensor<?>> result = getRunner().fetch(id).run();
        CheckUtils.checkState(result.size() == 1, "Fetched only a single tensor but received multiple results.");
        return result.get(0).expect(expected);
    }

    private <T> void set(final String placeHolderId, final String assignerId, final Tensor<T> tensor) {
        getRunner().feed(placeHolderId, tensor).addTarget(assignerId).run();
    }

    private Runner getRunner() {
        return m_savedModel.session().runner();
    }
}
