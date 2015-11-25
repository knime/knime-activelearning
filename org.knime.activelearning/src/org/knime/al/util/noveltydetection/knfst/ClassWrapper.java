/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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

package org.knime.al.util.noveltydetection.knfst;

import java.util.ArrayList;

/*
 * This class is used to determine how many different classes the datasets contains
 * and how many samples belong to each class
 */
public class ClassWrapper {
    private final String name;
    private final int count;

    public ClassWrapper(final String name, final int count) {
        this.name = name;
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }

    /*
     * Returns an array of ClassWrapper that contains the number of samples for
     * each class Parameter: labels: String array containing the labels of the
     * samples (NOTE: Labels MUST be ordered by class) Output: ClassWrapper
     * array that contains the number of samples for each class
     */
    public static ClassWrapper[] classes(final String[] labels) {
        final ArrayList<ClassWrapper> ret = new ArrayList<ClassWrapper>();
        int count = 0;
        String nameOld = labels[0];
        for (int i = 0; i < labels.length; i++) {
            final String nameNew = labels[i];
            // check for new class
            if (!nameOld.equals(nameNew)) {
                ret.add(new ClassWrapper(nameOld, count));
                nameOld = nameNew;
                count = 1;
            } else {
                count++;
            }
        }
        // add last class to list
        ret.add(new ClassWrapper(nameOld, count));

        return ret.toArray(new ClassWrapper[ret.size()]);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof ClassWrapper) {
            final ClassWrapper cl = (ClassWrapper) obj;
            if (getName().equals(cl.getName()) && getCount() == cl.getCount()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }

    }
}
