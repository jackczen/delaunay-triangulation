package com.jackczenszak.delaunay;

import java.awt.geom.Point2D;
import java.util.Arrays;

import static com.jackczenszak.delaunay.Edge.connect;
import static com.jackczenszak.delaunay.Edge.deleteEdge;
import static com.jackczenszak.delaunay.Edge.makeEdge;
import static com.jackczenszak.delaunay.Edge.splice;

/**
 * Provides methods used to generate Delaunay diagrams and planar graphs from collections of
 * two-dimensional points.
 *
 * Many methods are adapted from the paper "Primitives for the Manipulation of General Subdivisions
 * and the Computation of Voronoi Diagrams" written by Leonidas Guibas and Jorge Stolfi and linked
 * below:
 *
 * https://dl.acm.org/doi/10.1145/282918.282923
 */
public class Delaunay {
  /**
   * An efficient algorithm for computing a Delaunay diagram for a collection of two-dimensional
   * points. A Delaunay diagram is a partition of a plane into triangles such that the minimum value
   * for all angles is maximized.
   *
   * @param s a collection of two-dimensional points; points should be sorted in ascending order by
   *          x-coordinate and ties should be broken by sorting in ascending order of y-coordinate;
   *          there should be no duplicate points in the set
   * @return  an edge tuple [le, re], which are the counterclockwise convex hull edge out of the
   *          leftmost vertex and the clockwise convex hull edge out of the rightmost vertex,
   *          respectively.
   */
  public static Edge[] delaunay(Point2D[] s) {
    if (s.length == 2) {
      // Let s[0] and s[1] be two sites, in sorted order. Create an edge a from s[0] to s[1].
      Edge a = makeEdge();
      a.setOrg(s[0]);
      a.setDest(s[1]);
      return new Edge[] {a, a.sym()};
    } else if (s.length == 3) {
      // Let s[0], s[1], and s[2] are three sites in sorted order. Create edges a connecting
      // s[0] to s[1] and b connecting s[1] to s[2].
      Edge a = makeEdge(); Edge b = makeEdge();
      a.setOrg(s[0]); a.setDest(s[1]); b.setOrg(s[1]); b.setDest(s[2]);
      splice(a.sym(), b);

      // Now, close the triangle.
      if (ccw(s[0], s[1], s[2])) {
        Edge c = connect(b, a);
        return new Edge[] {a, b.sym()};
      } else if (ccw(s[0], s[2], s[1])) {
        Edge c = connect(b, a);
        return new Edge[] {c.sym(), c};
      } else {
        // The three points are collinear. Therefore, there is no need to construct an edge that
        // closes the triangle.
        return new Edge[] {a, b.sym()};
      }
    } else if (s.length >= 4) {
      // Generates a Delaunay diagram for the left half of the points in s.
      Edge[] l = delaunay(Arrays.copyOfRange(s, 0, s.length / 2));
      Edge ldo = l[0]; // Outer edge of the left half.
      Edge ldi = l[1]; // Inner edge of the left half.
      // Generates a Delaunay diagram for the right half of the points in s.
      Edge[] r = delaunay(Arrays.copyOfRange(s, s.length / 2, s.length));
      Edge rdi = r[0]; // Inner edge of the right half.
      Edge rdo = r[1]; // Outer edge of the right half.

      // Compute the lower tangent of l and r.
      while (true) {
        if (leftOf(rdi.org(), ldi)) {
          ldi = ldi.lNext();
        } else if (rightOf(ldi.org(), rdi)) {
          rdi = rdi.rPrev();
        } else {
          break;
        }
      }

      // Create a first cross edge basel from rdi.org() to ldi.org()
      Edge basel = connect(rdi.sym(), ldi);
      if (ldi.org().equals(ldo.org())) {
        ldo = basel.sym();
      }
      if (rdi.org().equals(rdo.org())) {
        rdo = basel;
      }

      // This is the merge loop.
      while (true) {
        // Locate the first L point (lcand.dest()) to be encountered by the rising bubble,
        // and delete L edges out of basel.dest() that fail the circle test.
        Edge lcand = basel.sym().oNext();
        if (valid(lcand, basel)) {
          while (inCircle(basel.dest(), basel.org(), lcand.dest(), lcand.oNext().dest())) {
            Edge t = lcand.oNext();
            deleteEdge(lcand);
            lcand = t;
          }
        }

        // Symmetrically, locate the first R point to be hit, and delete R edges.
        Edge rcand = basel.oPrev();
        if (valid(rcand, basel)) {
          while (inCircle(basel.dest(), basel.org(), rcand.dest(), rcand.oPrev().dest())) {
            Edge t = rcand.oPrev();
            deleteEdge(rcand);
            rcand = t;
          }
        }

        // If both lcand and rcand are invalid, then basel is the upper common tangent, and we
        // exit the merge loop.
        if (!valid(lcand, basel) && !valid(rcand, basel)) {
          break;
        }

        // The next cross edge is to be connected to either lcand.dest() or rcand.dest(). If both
        // are valid, then chose the appropriate one using the inCircle test.
        if (!valid(lcand, basel)
                || (valid(rcand, basel)
                && inCircle(lcand.dest(), lcand.org(), rcand.org(), rcand.dest()))) {
          // Add cross edge basel from rcand.dest() to basel.dest()
          basel = connect(rcand, basel.sym());
        } else {
          // Add cross edge basel from basel.org() to lcand.dest().
          basel = connect(basel.sym(), lcand.sym());
        }
      }
      return new Edge[] {ldo, rdo};
    } else {
      // If the provided set contains less than two points, the Delaunay diagram does not contain
      // any edges, so we return an empty array.
      return new Edge[0];
    }
  }

  /**
   * Determines if the provided point lies to the right of the provided edge.
   *
   * @param x a two-dimensional point
   * @param e an edge
   * @return  if {@code x} lies to the right of {@code e}
   */
  public static boolean rightOf(Point2D x, Edge e) {
    return ccw(x, e.dest(), e.org());
  }

  /**
   * Determines if the provided point lies to the left of the provided edge.
   *
   * @param x a two-dimensional point
   * @param e an edge
   * @return  if {@code x} lies to the left of {@code e}
   */
  public static boolean leftOf(Point2D x, Edge e) {
    return ccw(x, e.org(), e.dest());
  }

  /**
   * Determines if the provided edge {@code e} is above the provided edge {@code basel}.
   *
   * @param e     an edge being tested
   * @param basel a base edge
   * @return      if {@code e} is above {@code basel}
   */
  public static boolean valid(Edge e, Edge basel) {
    return rightOf(e.dest(), basel);
  }

  /**
   * Determines if the provided points {@code a}, {@code b}, and {@code c} form a counterclockwise-
   * oriented triangle.
   *
   * @param a a two-dimensional point
   * @param b a two-dimensional point
   * @param c a two-dimensional point
   * @return  if {@code a}, {@code b}, and {@code c} form a counterclockwise-oriented triangle
   */
  public static boolean ccw(Point2D a, Point2D b, Point2D c) {
    return threePointDet(a, b, c) > 0;
  }

  /**
   * Determines if the point {@code d} is interior to the region of the plane that is bounded
   * by the oriented circle {@code abc} and lies to the left of it. In other words, {@code d}
   * should be inside the circle {@code abc} if the points {@code a}, {@code b}, and {@code c}
   * define a counterclockwise-oriented triangle and outside if the points {@code a}, {@code b},
   * and {@code c} define a clockwise-oriented triangle.
   *
   * @param a a two-dimensional point used to define the boundary of a circle
   * @param b a two-dimensional point used to define the boundary of a circle
   * @param c a two-dimensional point used to define the boundary of a circle
   * @param d a two-dimensional point being tested
   * @return  if {@code d} is interior to the region of the plane bounded by the oriented circle
   *          {@code abc} and lies to the left of it
   */
  public static boolean inCircle(Point2D a, Point2D b, Point2D c, Point2D d) {
    // Equivalent to taking the determinant of the matrix
    //
    //  | a.getX()    a.getY()    a.getX()^2 + a.getY()^2   1 |
    //  | b.getX()    b.getY()    b.getX()^2 + b.getY()^2   1 |
    //  | c.getX()    c.getY()    c.getX()^2 + c.getY()^2   1 |
    //  | d.getX()    d.getY()    d.getX()^2 + d.getY()^2   1 |
    //
    // and testing that it is greater than zero.

    return ((a.getX() * a.getX() + a.getY() * a.getY()) * threePointDet(b, c, d))
            - ((b.getX() * b.getX() + b.getY() * b.getY()) * threePointDet(a, c, d))
            + ((c.getX() * c.getX() + c.getY() * c.getY()) * threePointDet(a, b, d))
            - ((d.getX() * d.getX() + d.getY() * d.getY()) * threePointDet(a, b, c)) > 0;
  }

  private static double threePointDet(Point2D a, Point2D b, Point2D c) {
    // Equivalent to taking the determinant of the matrix
    //
    //  | a.getX()    a.getY()    1 |
    //  | b.getX()    b.getY()    1 |
    //  | c.getX()    c.getY()    1 |

    return (b.getX() * c.getY() - c.getX() * b.getY())
            - (a.getX() * c.getY() - c.getX() * a.getY())
            + (a.getX() * b.getY() - b.getX() * a.getY());
  }

}
