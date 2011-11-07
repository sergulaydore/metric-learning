/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.vogella.algorithms.dijkstra.model;

/**
 *
 * @author Lars Vogel
 */
import java.util.List;

public class Graph {
	private final List<Vertex> vertexes;
	private final List<Edge> edges;

	public Graph(List<Vertex> vertexes, List<Edge> edges) {
		this.vertexes = vertexes;
		this.edges = edges;
	}

	public List<Vertex> getVertexes() {
		return vertexes;
	}

	public List<Edge> getEdges() {
		return edges;
	}
	
	
	
}