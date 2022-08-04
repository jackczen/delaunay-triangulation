package com.jackczenszak.delaunay;

import java.awt.geom.Point2D;
import java.util.Objects;

/**
 * An implementation of the QuadEdge data structure, introduced by Leonidas Guibas and Jorge Stolfi
 * in their paper "Primitives for the Manipulation of General Subdivisions and the Computation of
 * Voronoi Diagrams."
 *
 * QuadEdges are primarily used to represent subdivisions of two-dimensional manifold. Each edge
 * lies on the boundary of two face and connects two vertices. Every face is bounded by a closed
 * chain of edges. Every vertex is surrounded by ring of edges.
 */
public class Edge {
  private final int r;
  private final QuadEdge record;

  private Edge next;
  private Point2D data;

  private Edge(int r, QuadEdge record) {
    this.r = r;
    this.record = record;
  }

  /**
   * Constructs a new edge that represents the subdivision of a sphere. This edge does not have a
   * defined origin or destination vertex.
   *
   * @return  a new edge
   */
  public static Edge makeEdge() {
    QuadEdge record = new QuadEdge();
    return record.edgeRing[0];
  }

  private void setNext(Edge e) {
    this.next = e;
  }

  /**
   * Sets the origin vertex of this edge.
   *
   * @param data  a two-dimensional point
   */
  public void setOrg(Point2D data) {
    this.data = data;
  }

  /**
   * Sets the destination vertext of this edge.
   *
   * @param data  a two-dimensional point
   */
  public void setDest(Point2D data) {
    this.sym().setOrg(data);
  }

  /**
   * Retrieves the vertex of origin for this edge.
   *
   * @return  the vertex of origin for this edge
   */
  public Point2D org() {
    return this.data;
  }

  /**
   * Retrieves the vertex of destination for this edge.
   *
   * @return  the vertex of destination for this edge
   */
  public Point2D dest() {
    return this.sym().org();
  }

  /**
   * Computes the symmetric of this edge.
   *
   * @return this edge with the opposite direction and same orientation.
   */
  public Edge sym() {
    return this.record.edgeRing[(this.r + 2) % 4];
  }

  /**
   * Computes the next counterclockwise edge with the same origin.
   *
   * @return  the next counterclockwise edge with the same origin
   */
  public Edge oNext() {
    return this.next;
  }

  /**
   * Computes the next counterclockwise edge with the same destination.
   *
   * @return  the next counterclockwise edge with the same destination
   */
  public Edge dNext() {
    return this.sym().oNext().sym();
  }

  /**
   * Computes the next counterclockwise edge with same left face.
   *
   * @return  the next counterclockwise edge with same left face
   */
  public Edge lNext() {
    return this.rotInverse().oNext().rot();
  }

  /**
   * Computes the next counterclockwise edge with same right face.
   *
   * @return  the next counterclockwise edge with same right face
   */
  public Edge rNext() {
    return this.rot().oNext().rotInverse();
  }

  /**
   * Computes the dual of this edge, directed from the right face to the left face of this
   * edge.
   *
   * @return  the dual of this edge, directed from the right face to the left face
   */
  public Edge rot() {
    return this.record.edgeRing[(this.r + 1) % 4];
  }

  /**
   * Computes the dual of this edge, directed from the left face to the right face of this
   * edge.
   *
   * @return  the dual of this edge, directed from the left face to the right face
   */
  public Edge rotInverse() {
    return this.record.edgeRing[(this.r + 3) % 4];
  }

  /**
   * Computes the next clockwise edge with the same origin.
   *
   * @return  the next clockwise edge with the same origin
   */
  public Edge oPrev() {
    return this.rot().oNext().rot();
  }

  /**
   * Computes the next clockwise edge with the same destination.
   *
   * @return  the next clockwise edge with the same destination
   */
  public Edge dPrev() {
    return this.rotInverse().oNext().rotInverse();
  }

  /**
   * Computes the next clockwise edge with same left face.
   *
   * @return  the next clockwise edge with same left face
   */
  public Edge lPrev() {
    return this.oNext().sym();
  }

  /**
   * Computes the next clockwise edge with same right face.
   *
   * @return  the next clockwise edge with same right face
   */
  public Edge rPrev() {
    return this.sym().oNext();
  }

  /**
   * Affects the two edge rings aOrg and bOrg and, independently, the two edge rings aLeft and
   * bLeft. In each case,
   *  a)  if the two rings are distinct, {@code splice} will combine them into one.
   *  b)  if the two are exactly the same ring, {@code splice} will break it into two separate
   *      pieces.
   *
   * @param a an edge
   * @param b an edge
   */
  public static void splice(Edge a, Edge b) {
    Edge alpha = a.oNext().rot();
    Edge beta = b.oNext().rot();

    Edge aONext = a.oNext();
    Edge bONext = b.oNext();
    Edge alphaONext = alpha.oNext();
    Edge betaONext = beta.oNext();

    a.setNext(bONext);
    b.setNext(aONext);
    alpha.setNext(betaONext);
    beta.setNext(alphaONext);
  }

  /**
   * Adds a new edge e connecting the destination of {@code a} to the origin of {@code b}, such that
   * the edge rings containing both {@code a} and {@code b} are both properly updated.
   *
   * @param a an edge
   * @param b an edge
   * @return  an edge that connects the destination of {@code a} to the origin of {@code b}
   */
  public static Edge connect(Edge a, Edge b) {
    Edge e = makeEdge();
    e.setOrg(a.dest());
    e.setDest(b.org());

    // Updates the ring e Org.
    splice(e, a.lNext());
    // Updates the ring e Dest.
    splice(e.sym(), b);

    return e;
  }

  /**
   * Deletes an edge from its current ring, updating members of the ring appropriately.
   *
   * @param e an edge to be deleted
   */
  public static void deleteEdge(Edge e) {
    splice(e, e.oPrev());
    splice(e.sym(), e.sym().oPrev());
  }

  @Override
  public String toString() {
    if (this.org() == null || this.dest() == null) {
      return "null";
    }
    return String.format("(%f, %f) -> (%f, %f)",
            this.org().getX(), this.org().getY(),
            this.dest().getX(), this.dest().getY());
  }

  /**
   * Determines if the provided object is geometrically equivalent to this edge.
   *
   * @param other an object being compared
   * @return      if object is geometrically equivalent to this edge
   */
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof Edge)) {
      return false;
    }

    Edge o = (Edge)other;

    return this.org().equals(o.org())
            && this.dest().equals(o.dest());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.org(), this.dest());
  }

  private static class QuadEdge {
    private final Edge[] edgeRing = new Edge[4];

    private QuadEdge() {
      this.edgeRing[0] = new Edge(0, this);
      this.edgeRing[1] = new Edge(1, this);
      this.edgeRing[2] = new Edge(2, this);
      this.edgeRing[3] = new Edge(3, this);

      this.edgeRing[0].setNext(this.edgeRing[0]);
      this.edgeRing[1].setNext(this.edgeRing[3]);
      this.edgeRing[2].setNext(this.edgeRing[2]);
      this.edgeRing[3].setNext(this.edgeRing[1]);
    }
  }
}
