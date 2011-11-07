/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.vogella.algorithms.dijkstra.model;

/**
 *
 * @author Lars Vogel
 */
public class Edge  {
	private final String id; 
	private final Vertex source;
	private final Vertex destination;
	private final double weight; 
        
        private int type;

        public final static int SUB = 1;
        public final static int DEL = 2;
        public final static int INS = 3;
	
	public Edge(String id, Vertex source, Vertex destination, double weight, int type) {
		this.id = id;
		this.source = source;
		this.destination = destination;
		this.weight = weight;
                this.type = type;
	}
	
        public int getType() {
            return type;
        }
    
	public String getId() {
		return id;
	}
	public Vertex getDestination() {
		return destination;
	}

	public Vertex getSource() {
		return source;
	}
	public double getWeight() {
		return weight;
	}
	
	@Override
	public String toString() {
		return source + " " + destination;
	}
	
	
}
