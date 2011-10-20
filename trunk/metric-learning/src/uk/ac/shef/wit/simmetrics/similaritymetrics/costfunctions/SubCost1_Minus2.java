/**
 * SimMetrics - SimMetrics is a java library of Similarity or Distance
 * Metrics, e.g. Levenshtein Distance, that provide float based similarity
 * measures between String Data. All metrics return consistant measures
 * rather than unbounded similarity scores.
 *
 * Copyright (C) 2005 Sam Chapman - Open Source Release v1.1
 *
 * Please Feel free to contact me about this library, I would appreciate
 * knowing quickly what you wish to use it for and any criticisms/comments
 * upon the SimMetric library.
 *
 * email:       s.chapman@dcs.shef.ac.uk
 * www:         http://www.dcs.shef.ac.uk/~sam/
 * www:         http://www.dcs.shef.ac.uk/~sam/stringmetrics.html
 *
 * address:     Sam Chapman,
 *              Department of Computer Science,
 *              University of Sheffield,
 *              Sheffield,
 *              S. Yorks,
 *              S1 4DP
 *              United Kingdom,
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package uk.ac.shef.wit.simmetrics.similaritymetrics.costfunctions;

import java.io.Serializable;

/**
 * Package: costfunctions
 * Description: SubCost1_Minus2 implements a substitution cost function where d(i,j) = 1 if i does not equal j, -2 if i equals j.

 * Date: 24-Mar-2004
 * Time: 13:38:12
 * @author Sam Chapman <a href="http://www.dcs.shef.ac.uk/~sam/">Website</a>, <a href="mailto:sam@dcs.shef.ac.uk">Email</a>.
 * @version 1.1
 */
public final class SubCost1_Minus2 extends AbstractSubstitutionCost implements Serializable {

    /**
     * returns the name of the cost function.
     *
     * @return the name of the cost function
     */
    public final String getShortDescriptionString() {
        return "SubCost1_Minus2";
    }

    /**
     * get cost between characters where d(i,j) = 1 if i does not equal j, -2 if i equals j.
     *
     * @param string1Index - the index within the source to test
     * @param string2Index - the index within the target to test
     * @return  the cost of a given substitution d(i,j) where d(i,j) = 1 if i!=j, -2 if i==j
     */
    public final double getSubstitutionCost(final int string1Index, final int string2Index) {
        //check within range
        if (source.length() <= string1Index || string1Index < 0) {
            return 0;
        }
        if (target.length() <= string2Index || string2Index < 0) {
            return 0;
        }

        if (source.charAt(string1Index) == target.charAt(string2Index)) {
            return 1.0f;
        } else {
            return -2.0f;
        }
    }

    /**
     * returns the maximum possible cost.
     *
     * @return the maximum possible cost
     */
    public final double getMaxCost() {
        return 1.0f;
    }

    /**
     * returns the minimum possible cost.
     *
     * @return the minimum possible cost
     */
    public final double getMinCost() {
        return -2.0f;
    }

    @Override
    public String[] initialize(String source, String target) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getDeleteCost(int i, int i0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getInsertCost(int i, int i0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getSourceAlphabet() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getTargetAlphabet() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int[][] getCountMatrix() {
        throw new UnsupportedOperationException("Not supported yet.");
    }



}

