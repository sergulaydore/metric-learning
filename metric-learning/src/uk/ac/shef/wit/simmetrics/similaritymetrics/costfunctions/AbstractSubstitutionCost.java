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

/**
 * Package: cost functions Description: AbstractSubstitutionCost implements a abstract class for substitution costs
 * functions. Date: 24-Mar-2004 Time: 13:34:45
 *
 * @author Sam Chapman <a href="http://www.dcs.shef.ac.uk/~sam/">Website</a>, <a href="mailto:sam@dcs.shef.ac.uk">Email</a>.
 * @version 1.1
 */
public abstract class AbstractSubstitutionCost implements InterfaceSubstitutionCost {

        String source, target;

    /**
     * returns the name of the cost function.
     *
     * @return the name of the cost function
     */
    public abstract String getShortDescriptionString();

    /**
     * get cost between characters.
     *
     * @param string1Index - the index within the source to test
     * @param string2Index - the index within the target to test
     *
     * @return the cost of a given substitution d(i,j)
     */
    public abstract double getSubstitutionCost(final int string1Index, final int string2Index);

    /**
     * returns the maximum possible cost.
     *
     * @return the maximum possible cost
     */
    public abstract double getMaxCost();

    /**
     * returns the minimum possible cost.
     *
     * @return the minimum possible cost
     */
    public abstract double getMinCost();

    public abstract String[] initialize(String source, String target);
    public abstract int[][] getCountMatrix();
    public abstract double getDeleteCost(int i, int i0);
    public abstract double getInsertCost(int i, int i0);
    
    public abstract String getSourceAlphabet();
    public abstract String getTargetAlphabet();


}
