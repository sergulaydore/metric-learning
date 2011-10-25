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

package uk.ac.shef.wit.simmetrics.similaritymetrics;

import uk.ac.shef.wit.simmetrics.math.MathFuncs;
import uk.ac.shef.wit.simmetrics.similaritymetrics.costfunctions.SubCost01;


/**
 * Package: uk.ac.shef.wit.simmetrics.similaritymetrics.levensteindistance
 * Description: levensteindistance implements the levenstein distance function.

 * Date: 24-Mar-2004
 * Time: 10:54:06
 * @author Sam Chapman <a href="http://www.dcs.shef.ac.uk/~sam/">Website</a>, <a href="mailto:sam@dcs.shef.ac.uk">Email</a>.
 * @version 1.1
 */
public class Levenshtein {

    /**
     * a constant for calculating the estimated timing cost.
     */
    private final float ESTIMATEDTIMINGCONST = 1.8e-4f;

    /**
     * the private cost function used in the levenstein distance.
     */
    private static SubCost01 dCostFunc;

    public void setWeight(int i, int j, int k, double d) {
        dCostFunc.setWeight(i,j,k,d);
    }
    
    public double getWeight(int i, int j, int k) {
        return dCostFunc.getWeight(i,j,k);
    }
    
    /**
     * constructor - default (empty).
     */
    public Levenshtein(int n) {
        dCostFunc = new SubCost01(n);
    }
    
    /**
     * returns the string identifier for the metric.
     *
     * @return the string identifier for the metric
     */
    public String getShortDescriptionString() {
        return "Levenshtein";
    }

    /**
     * returns the long string identifier for the metric.
     *
     * @return the long string identifier for the metric
     */
    public String getLongDescriptionString() {
        return "Implements the basic Levenshtein algorithm providing a similarity measure between two strings";
    }

    /**
     * gets a div class xhtml similarity explaining the operation of the metric.
     *
     * @param string1 string 1
     * @param string2 string 2
     *
     * @return a div class html section detailing the metric operation.
     */
    public String getSimilarityExplained(String string1, String string2) {
        //todo this should explain the operation of a given comparison
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * gets the estimated time in milliseconds it takes to perform a similarity timing.
     *
     * @param string1 string 1
     * @param string2 string 2
     *
     * @return the estimated time in milliseconds taken to perform the similarity measure
     */
    public double getSimilarityTimingEstimated(String string1, String string2) {
        //timed millisecond times with string lengths from 1 + 50 each increment
        //0	0.31	1.12	2.4	4.41	6.77	11.28	14.5	24.33	31.29	43.6	51	54.5	67.67	68	78	88.67	101.5	109	117.5	140.5	148.5	156	180	187.5	219	203	250	250	312	297	328	297	359	360	406	453	422	437	469	500	516	578	578	578	609	672	656	688	766	765	781	829	843	875	891	984	954	984	1078
        float str1Length = string1.length();
        float str2Length = string2.length();
        return (str1Length * str2Length) * ESTIMATEDTIMINGCONST;
    }

    /**
     * gets the similarity of the two strings using levenstein distance.
     *
     * @param string1
     * @param string2
     * @return a value between 0-1 of the similarity
     */
    public double getSimilarity(String string1, String string2, int k) {
        dCostFunc.initialize(string1, string2, k);
        double levenshteinDistance = getUnNormalisedSimilarity(string1, string2, k);
        //convert into zero to one return

        //get the max possible levenstein distance score for string
        float maxLen = string1.length();
        if (maxLen < string2.length()) {
            maxLen = string2.length();
        }

        //check for 0 maxLen
        if (maxLen == 0) {
            return 1.0f; //as both strings identically zero length
        } else {
            //return actual / possible levenstein distance to get 0-1 range
            return 1.0f - (levenshteinDistance / maxLen);
        }

    }

    /**
     * implements the levenstein distance function
     * <p/>
     * Copy character from string1 over to string2 (cost 0)
     * Delete a character in string1 (cost 1)
     * Insert a character in string2 (cost 1)
     * Substitute one character for another (cost 1)
     * <p/>
     * D(i-1,j-1) + d(si,tj) //subst/copy
     * D(i,j) = min D(i-1,j)+1 //insert
     * D(i,j-1)+1 //delete
     * <p/>
     * d(i,j) is a function whereby d(c,d)=0 if c=d, 1 else. (???)
     *
     * @param s
     * @param t
     * @return the levenshtein distance between given strings
     */
    public double getUnNormalisedSimilarity(String s, String t, int k) {
        String[] str = dCostFunc.initialize(s,t,k);
        
//        System.out.println(str[0]+","+str[1]);
        
        double[][] d; // matrix
        int n; // length of s
        int m; // length of t
        int i; // iterates through s
        int j; // iterates through t
        double cost; // substitution cost
        double delCost; // delete cost (s -> t)
        double insCost; // insert cost (s -> t)

        // Step 1
        n = str[0].length();
        m = str[1].length();
        if (n == 0) {
            return m;
        }
        if (m == 0) {
            return n;
        }
        d = new double[n + 1][m + 1];

        // Step 2
        for (i = 0; i <= n; i++) {
            d[i][0] = i;
        }
        for (j = 0; j <= m; j++) {
            d[0][j] = j;
        }

        // Step 3
        for (i = 1; i <= n; i++) {
            // Step 4
            for (j = 1; j <= m; j++) {
                // Step 5
                cost = dCostFunc.getSubstitutionCost(i - 1, j - 1, k);
                delCost = dCostFunc.getDeleteCost(i - 1, j - 1, k);
                insCost = dCostFunc.getInsertCost(i - 1, j - 1, k);
                // Step 6
                d[i][j] = MathFuncs.min3(d[i - 1][j] + delCost, d[i][j - 1] + 
                        insCost, d[i - 1][j - 1] + cost);
            }
        }

        // Step 7
        return d[n][m];
    }
    
    public String getSourceAlphabet() {
        return dCostFunc.getSourceAlphabet();
    }
    public String getTargetAlphabet() {
        return dCostFunc.getTargetAlphabet();
    }

    public double[] getCostsMatrixAsArray(int k) {
        double[][][] M = dCostFunc.getCostsMatrix();
        double[] Marr = new double[4032];
        int h = 0;
        for(int i=0; i<M.length; i++)
            for(int j=0; j<M[i].length; j++)
                if(i != j) {
                    Marr[h] = M[i][j][k];
                    h++;
                }
        return Marr;
    }

    public int[] getCountMatrixAsArray(int k) {
        int[][][] count = dCostFunc.getCountMatrix();
        int[] Carr = new int[4032];
        int h = 0;
        for(int i=0; i<count.length; i++)
            for(int j=0; j<count[i].length; j++)
                if(i != j) {
                    Carr[h] = count[i][j][k];
                    h++;
                }
        return Carr;
    }

    public int getMatrixCheckSum(int k) {
        return dCostFunc.getMatrixCheckSum(k);
    }

    
    
}
