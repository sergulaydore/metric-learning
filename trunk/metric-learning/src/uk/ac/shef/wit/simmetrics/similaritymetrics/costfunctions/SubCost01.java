/**
 * SimMetrics - SimMetrics is a java library of Similarity or Distance
 * Metrics, e.g. Levenshtein Distance, that provide float based similarity
 * measures between String Data. All metrics return consistent measures
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
import java.text.Normalizer;
import java.text.Normalizer.Form;
import metriclearning.Couple;

/**
 * Package: cost functions
 * Description: SubCost01 implements a substitution cost function.

 * Date: 24-Mar-2004
 * Time: 13:38:12
 * @author Sam Chapman <a href="http://www.dcs.shef.ac.uk/~sam/">Website</a>, <a href="mailto:sam@dcs.shef.ac.uk">Email</a>.
 * @version 1.1
 */
public class SubCost01  {

    /**
     * Costs matrix, dimension: 64x64
     * 64 = 26 characters * 2 cases + 10 digits + space + empty string
     */
    private static double[][][] M;
    private static int[][][] count;
    private static final int MDIM = 64;
    
    private final int EMPTY_STRING = MDIM-1;
    private static String source, target;
    private static int N;
    
    
    public void setWeight(int i, int j, int k, double d) {
        M[i][j][k] = d;
    }
    
    public double getWeight(int i, int j, int k) {
        return M[i][j][k];
    }
    
    public SubCost01(int n) {
        N = n;
        M = new double[MDIM][MDIM][N];
        count = new int[MDIM][MDIM][N];
        // initialize the costs matrix
        for(int i=0; i<MDIM; i++) {
            for(int j=0; j<MDIM; j++) {
                for(int k=0; k<N; k++) {
                    if(i == j)
                        M[i][j][k] = 0.0;
                    else
                        M[i][j][k] = 1.0;
                    count[i][j][k] = 0;
                }
            }
        }
    }
    
    public String[] initialize(String src, String tgt, int k) {
        String[] str = new String[2];
        
        source = filter(src);
        target = filter(tgt);
        str[0] = source;
        str[1] = target;
        
        return str;
    }
    
    public void resetCount() {
        for(int i=0; i<MDIM; i++)
            for(int j=0; j<MDIM; j++)
                for(int k=0; k<N; k++)
                    count[i][j][k] = 0;
        
    }

    public double[][][] getCostsMatrix() { return M; }
    public int[][][] getCountMatrix() { return count; }

    /**
     * returns the name of the cost function.
     *
     * @return the name of the cost function
     */
    public String getShortDescriptionString() {
        return "SubCost01";
    }
    
    
    public double getDeleteCost(int i, int i0, int k) {
        return M[charToPosition(source.charAt(i))][EMPTY_STRING][k];
    }

    
    public double getInsertCost(int i, int i0, int k) {
        return M[EMPTY_STRING][charToPosition(target.charAt(i0))][k];
    }

    private int charToPosition(char c) {
        // input:  digits 48-57, uppercase 65-90, lowercase 97-122
        // output: digits 0-9,   uppercase 10-35, lowercase 36-61
        if(48 <= c && c <= 57)
            return c-48;
        else if(65 <= c && c <= 90)
            return c-55;
        else if(97 <= c && c <= 122)
            return c-61;
        else return 62; // space
    }
    
    /**
     * get cost between characters where d(i,j) = 1 if i does not equals j, 0 if i equals j.
     *
     * @param str1         - the string1 to evaluate the cost
     * @param string1Index - the index within the string1 to test
     * @param str2         - the string2 to evaluate the cost
     * @param string2Index - the index within the string2 to test
     * @return the cost of a given substitution d(i,j) where d(i,j) = 1 if i!=j, 0 if i==j
     */
    
    public double getSubstitutionCost(int string1Index, int string2Index, int k) {
//        System.out.println("between "+str1.charAt(string1Index)+" and "+str2.charAt(string2Index));
        if (source.charAt(string1Index) == target.charAt(string2Index)) {
            return 0.0;
        } else {
            return M[charToPosition(source.charAt(string1Index))]
                    [charToPosition(target.charAt(string2Index))]
                    [k];
        }
    }

    /**
     * returns the maximum possible cost.
     *
     * @return the maximum possible cost
     */
    
//    public double getMaxCost() {
//        double max = M[0][0];
//        for(int i=0; i<M.length; i++)
//            for(int j=0; j<M[i].length; j++)
//                if(M[i][j] > max)
//                    max = M[i][j];
//        return max;
//    }

    /**
     * returns the minimum possible cost.
     *
     * @return the minimum possible cost
     */
    
//    public double getMinCost() {
//        double min = M[0][0];
//        for(int i=0; i<M.length; i++)
//            for(int j=0; j<M[i].length; j++)
//                if(M[i][j] < min)
//                    min = M[i][j];
//        return min;
//    }

    
    // PENDING: do we need the alphabet?
    public String getSourceAlphabet() {
        String alphabet = "";
        for(int i=0; i<source.length(); i++)
            if(!alphabet.contains(""+source.charAt(i)))
                alphabet += source.charAt(i);
        return alphabet;
    }

    
    public String getTargetAlphabet() {
        String alphabet = "";
        for(int i=0; i<target.length(); i++)
            if(!alphabet.contains(""+target.charAt(i)))
                alphabet += target.charAt(i);
        return alphabet;
    }

    private String filter(String sIn) {
        sIn = Normalizer.normalize(sIn, Form.NFD);
        String sOut = "";
        for(int i=0;i<sIn.length();i++) {
            char c = sIn.charAt(i);
            // digits 48-57, uppercase 65-90, lowercase 97-122
            if((48 <= c && c <= 57) || (65 <= c && c <= 90) || (97 <= c && c <= 122) || c == 32)
                sOut += c;
        }
        return sOut;
    }

    public double getMatrixCheckSum(int k) {
        double sum = 0.0;
        for(int i=0; i<MDIM; i++)
            for(int j=0; j<MDIM; j++)
                sum += M[i][j][k];
        return sum;
    }

    public void count(int br, int i, int j, int k, Couple c) {
        switch(br) {
            case 1: // substitution
                count[charToPosition(source.charAt(i))][charToPosition(target.charAt(j))][k] ++;
                c.count(charToPosition(source.charAt(i)), charToPosition(target.charAt(j)), k);
                break;
            case 2: // deletion
                count[charToPosition(source.charAt(i))][EMPTY_STRING][k] ++;
                c.count(charToPosition(source.charAt(i)), EMPTY_STRING, k);
                break;
            case 3: // insertion
                count[EMPTY_STRING][charToPosition(target.charAt(j))][k] ++;
                c.count(EMPTY_STRING, charToPosition(target.charAt(j)), k);
                break;
        }
    }



}
