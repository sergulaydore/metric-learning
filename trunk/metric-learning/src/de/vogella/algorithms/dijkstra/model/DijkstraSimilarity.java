/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.vogella.algorithms.dijkstra.model;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 *
 * @author tom
 */
public class DijkstraSimilarity {
    
    public static void main(String[] args) {
        
        double[][][] M = new double[64][64][1];
        for(int i=0; i<M.length; i++)
            for(int j=0; j<M[i].length; j++)
                if(i == j)
                    M[i][j][0] = 0.0;
                else
                    M[i][j][0] = 1.0;
        
        String a = "flag";
        String b = "car";
        
        getDijkstraSimilarity(a, b, 0, M, new ArrayList<String>());
        
    }
    
    public static double getDijkstraSimilarity(String a, String b, int k, double[][][] M, ArrayList<String> counts) {
        
        ArrayList<Vertex> vertexes = new ArrayList<Vertex>();
        ArrayList<Edge> edges = new ArrayList<Edge>();
        
        int rowsize = b.length()+1;
        
        for(int i=0; i<a.length()+1; i++) {
            for(int j=0; j<b.length()+1; j++) {
                char I, J;
                int row, col;
                if(i > 0) {
                    I = a.charAt(i-1);
                    row = charToPosition(I);
                } else {
                    I = '*';
                    row = 63;
                }
                if(j > 0) {
                    J = b.charAt(j-1);
                    col = charToPosition(J);
                } else {
                    J = '*';
                    col = 63;
                }
                Vertex v = new Vertex(i+","+j,"Node_"+I+","+J);
                vertexes.add(v);
//                System.out.println("added: "+(vertexes.size()-1)+" Node_"+I+","+J);
                if(i > 0) { // deletion
                    edges.add(new Edge("Del_"+I, vertexes.get((i-1)*rowsize+j), v, M[row][63][k], Edge.DEL));
//                    System.out.println(i+","+j+") Del-linked: "+((i-1)*rowsize+j)+" -> "+(vertexes.size()-1));
                }
                if(j > 0) { // insertion
                    edges.add(new Edge("Ins_"+J, vertexes.get(i*rowsize+j-1), v, M[63][col][k], Edge.INS));
//                    System.out.println(i+","+j+") Add-linked: "+(i*rowsize+j-1)+" -> "+(vertexes.size()-1));
                }
                if(i > 0 && j > 0) { // substitution
                    edges.add(new Edge("Sub_"+I+","+J, vertexes.get((i-1)*rowsize+j-1), v, M[row][col][k], Edge.SUB));
//                    System.out.println(i+","+j+") Sub-linked: "+((i-1)*rowsize+j-1)+" -> "+(vertexes.size()-1));
                }
            }
        }
        
        Graph graph = new Graph(vertexes, edges);
        DijkstraAlgorithm d = new DijkstraAlgorithm(graph);
        
        d.execute(vertexes.get(0));
        LinkedList<Vertex> lv = d.getPath(vertexes.get(vertexes.size()-1));
        
//        for(Vertex v : lv)
//            System.out.println(v.getName());
        
        double l = d.getPathLength(lv, edges, counts);
        
//        System.out.println("DST('"+a+"','"+b+"') = "+l);
        
        return l;
    }
    
    private static int charToPosition(char c) {
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

}
