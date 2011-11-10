/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.vogella.algorithms.dijkstra.model;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.LinkedList;
import metriclearning.Couple;
import metriclearning.Resource;

/**
 *
 * @author tom
 */
public class DijkstraSimilarity {

    /**
     * Costs matrix, dimension: 64x64
     * 64 = 26 characters * 2 cases + 10 digits + space + empty string
     */
    private static double[][][] M;
    private static int[][][] count;
    private static final int MDIM = 64;
    private static final int EMPTY_STRING = MDIM - 1;
    private static String source, target;
    private static int N;

    
    private static double getUnNormalisedDijkstraSimilarity(String s, String t, int k, Couple c) {

        source = filter(s);
        target = filter(t);

        ArrayList<Vertex> vertexes = new ArrayList<Vertex>();
        ArrayList<Edge> edges = new ArrayList<Edge>();

        int rowsize = target.length() + 1;

        for (int i = 0; i < source.length() + 1; i++) {
            for (int j = 0; j < target.length() + 1; j++) {
                char I, J;
                int row, col;
                if (i > 0) {
                    I = source.charAt(i - 1);
                    row = charToPosition(I);
                } else {
                    I = '*';
                    row = 63;
                }
                if (j > 0) {
                    J = target.charAt(j - 1);
                    col = charToPosition(J);
                } else {
                    J = '*';
                    col = 63;
                }
                Vertex v = new Vertex(i + "," + j, "Node_" + I + "," + J);
                vertexes.add(v);
//                System.out.println("added: "+(vertexes.size()-1)+" Node_"+I+","+J);
                if (i > 0) { // deletion
                    edges.add(new Edge("Del_" + I, vertexes.get((i - 1) * rowsize + j), v, M[row][63][k], Edge.DEL));
//                    System.out.println(i+","+j+") Del-linked: "+((i-1)*rowsize+j)+" -> "+(vertexes.size()-1));
                }
                if (j > 0) { // insertion
                    edges.add(new Edge("Ins_" + J, vertexes.get(i * rowsize + j - 1), v, M[63][col][k], Edge.INS));
//                    System.out.println(i+","+j+") Add-linked: "+(i*rowsize+j-1)+" -> "+(vertexes.size()-1));
                }
                if (i > 0 && j > 0) { // substitution
                    edges.add(new Edge("Sub_" + I + "," + J, vertexes.get((i - 1) * rowsize + j - 1), v, M[row][col][k], Edge.SUB));
//                    System.out.println(i+","+j+") Sub-linked: "+((i-1)*rowsize+j-1)+" -> "+(vertexes.size()-1));
                }
            }
        }

        Graph graph = new Graph(vertexes, edges);
        DijkstraAlgorithm d = new DijkstraAlgorithm(graph);

        d.execute(vertexes.get(0));
        // gets the path to the last node (i.e. the target)
        LinkedList<Vertex> lv = d.getPath(vertexes.get(vertexes.size() - 1));

        double l = 0.0;
        for(int i=0; i<lv.size()-1; i++) {
            Vertex v1 = lv.get(i);
            Vertex v2 = lv.get(i+1);
            
            for(Edge e : edges)
                if(e.getSource().equals(v1) && e.getDestination().equals(v2)) {
                    l += e.getWeight();
                    String[] v2id = v2.getId().split(",");
                    if(e.getType() == Edge.SUB)
                        countSub(Integer.parseInt(v2id[0]) - 1, Integer.parseInt(v2id[1]) - 1, k, c);
                    else if (e.getType() == Edge.DEL)
                        countDel(Integer.parseInt(v2id[0]) - 1, k, c);
                    else // Edge.INS:
                        countIns(Integer.parseInt(v2id[1]) - 1, k, c);
                }
        }

        return l;
    }

    private static String filter(String sIn) {
        sIn = Normalizer.normalize(sIn, Form.NFD);
        String sOut = "";
        for (int i = 0; i < sIn.length(); i++) {
            char c = sIn.charAt(i);
            // digits 48-57, uppercase 65-90, lowercase 97-122
            if ((48 <= c && c <= 57) || (65 <= c && c <= 90) || (97 <= c && c <= 122) || c == 32) {
                sOut += c;
            }
        }
        return sOut;
    }

    private static void countSub(int i, int j, int k, Couple c) {
        count[charToPosition(source.charAt(i))][charToPosition(target.charAt(j))][k]++;
        c.count(charToPosition(source.charAt(i)), charToPosition(target.charAt(j)), k);
    }
    
    private static void countDel(int i, int k, Couple c) {
        count[charToPosition(source.charAt(i))][EMPTY_STRING][k]++;
        c.count(charToPosition(source.charAt(i)), EMPTY_STRING, k);
    }
    
    private static void countIns(int j, int k, Couple c) {
        count[EMPTY_STRING][charToPosition(target.charAt(j))][k]++;
        c.count(EMPTY_STRING, charToPosition(target.charAt(j)), k);
    }
        
    private static int charToPosition(char c) {
        // input:  digits 48-57, uppercase 65-90, lowercase 97-122
        // output: digits 0-9,   uppercase 10-35, lowercase 36-61
        if (48 <= c && c <= 57) {
            return c - 48;
        } else if (65 <= c && c <= 90) {
            return c - 55;
        } else if (97 <= c && c <= 122) {
            return c - 61;
        } else {
            return 62; // space
        }
    }
    
    private static char positionToChar(int p) {
        // input: digits 0-9,   uppercase 10-35, lowercase 36-61
        // output:  digits 48-57, uppercase 65-90, lowercase 97-122
        if (0 <= p && p <= 9) {
            return (char)(p + 48);
        } else if (10 <= p && p <= 35) {
            return (char)(p + 55);
        } else if (36 <= p && p <= 61) {
            return (char)(p + 61);
        } else {
            return ' '; // space
        }
    }

    /* ----- public methods ----- */
    
    public static void initialize(int n) {
        N = n;
        M = new double[MDIM][MDIM][N];
        count = new int[MDIM][MDIM][N];
        // initialize the costs matrix
        for (int i = 0; i < MDIM; i++) {
            for (int j = 0; j < MDIM; j++) {
                for (int k = 0; k < N; k++) {
                    if (i == j) {
                        M[i][j][k] = 0.0;
                    } else {
                        M[i][j][k] = 1.0;
                    }
                    count[i][j][k] = 0;
                }
            }
        }
    }

    public static void resetCount() {
        for (int i = 0; i < MDIM; i++) {
            for (int j = 0; j < MDIM; j++) {
                for (int k = 0; k < N; k++) {
                    count[i][j][k] = 0;
                }
            }
        }

    }

    public static double getDijkstraSimilarity(String string1, String string2, int k, Couple c) {
        
        String[] strings1 = string1.split(" ");
        String[] strings2 = string2.split(" ");
        
        double sum1 = 0.0;
        for(String s1: strings1) {
            double max = 0.0;
            for(String s2 : strings2) {
                double dld = getUnNormalisedDijkstraSimilarity(s1, s2, k, c);
                // get the max possible levenstein distance score for string
                double maxLen = s1.length() + s2.length();
                double sim;
                // check for 0 maxLen
                if (maxLen == 0) {
                    // as both strings identically zero length
                    sim = 1.0;
                } else {
                    // return actual / possible levenstein distance to get 0-1 range
                    sim = 1.0 - (dld / maxLen);
                }
                
                if(sim > max)
                    max = sim;
//                System.out.println("sim('"+s1+"','"+s2+"') = "+sim);
            }
            sum1 += max;
//            System.out.println("bestsim('"+s1+"',b) = "+max);
        }
        
        double sum2 = 0.0;
        for(String s2: strings2) {
            double max = 0.0;
            for(String s1 : strings1) {
                double dld = getUnNormalisedDijkstraSimilarity(s1, s2, k, c);
                // get the max possible levenstein distance score for string
                double maxLen = s1.length() + s2.length();
                double sim;
                // check for 0 maxLen
                if (maxLen == 0) {
                    // as both strings identically zero length
                    sim = 1.0;
                } else {
                    // return actual / possible levenstein distance to get 0-1 range
                    sim = 1.0 - (dld / maxLen);
                }
                
                if(sim > max)
                    max = sim;
//                System.out.println("sim('"+s2+"','"+s1+"') = "+sim);
            }
            sum2 += max;
//            System.out.println("bestsim(a,'"+s2+"') = "+max);
        }
        
        return Math.pow(((sum1 / strings1.length) + (sum2 / strings2.length)) / 2, 5.0);
    }
    
    public static double[] getCostsMatrixAsArray(int k) {
        double[] Marr = new double[4032];
        int h = 0;
        for (int i = 0; i < M.length; i++) {
            for (int j = 0; j < M[i].length; j++) {
                if (i != j) {
                    Marr[h] = M[i][j][k];
                    h++;
                }
            }
        }
        return Marr;
    }

    public static int[] getCountMatrixAsArray(int k) {
        int[] Carr = new int[4032];
        int h = 0;
        for (int i = 0; i < count.length; i++) {
            for (int j = 0; j < count[i].length; j++) {
                if (i != j) {
                    Carr[h] = count[i][j][k];
                    h++;
                }
            }
        }
        return Carr;
    }

    public static void setWeight(int i, int j, int k, double d) {
        M[i][j][k] = d;
    }

    public static double getWeight(int i, int j, int k) {
        return M[i][j][k];
    }

    public static void main(String[] args) {
        
        initialize(1);
        
        for (int i = 0; i < M.length; i++) {
            for (int j = 0; j < M[i].length; j++) {
                if (i == j) {
                    M[i][j][0] = 0.0;
                } else {
                    M[i][j][0] = 1.0;
                }
            }
        }

        String a = "The WASA2 object-oriented workflow management system";
        String b = "World Wide Database-integrating the Web, CORBA and databases";
//        String b = "Semantic Integration of Environmental Models for Application to Global Information Systems and Decision-Making";
        Couple c = new Couple(new Resource(a), new Resource(b));
        c.initializeCount(1);
        
        double d = getDijkstraSimilarity(a, b, 0, c);
        System.out.println("a = "+a);
        System.out.println("b = "+b);
        System.out.println("sim(a,b) = "+d);

    }

    public static double getMatrixCheckSum(int k) {
        double sum = 0.0;
        for (int i = 0; i < MDIM; i++) {
            for (int j = 0; j < MDIM; j++) {
                sum += M[i][j][k];
            }
        }
        return sum;
    }

    public static void showCostsMatrix() {
        System.out.println("");
        for(int i=0; i<M.length-1; i++)
            System.out.print("\t"+positionToChar(i));
        System.out.println("\tε");
        for (int i = 0; i < M.length; i++) {
            if(i < 63)
                System.out.print(positionToChar(i)+"\t");
            else
                System.out.print("ε\t");
            for (int j = 0; j < M[i].length; j++) {
                System.out.print(((double) (int) (M[i][j][0] * 1000)) / 1000 + "\t");
            }
            System.out.println("");
        }
    }

}
