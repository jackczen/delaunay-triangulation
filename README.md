# Java Delaunay Triangulation Library

### Overview

This program provides an *O(n log n)* algorithm for constructing two-dimensional [Delaunay diagrams](https://en.wikipedia.org/wiki/Delaunay_triangulation). It achieves this by utilizing the [Quad-edge](https://en.wikipedia.org/wiki/Quad-edge) data structure, first introduced by Leonidas Guibas and Jorge Stolfi in their landmark paper ["Primitives for the Manipulation of General Subdivisions and the Computation of Voronoi Diagrams"](https://dl.acm.org/doi/10.1145/282918.282923).

### Usage

Delaunay diagrams can be generated for a *sorted* (first in ascending order by x-coordinate and, in cases of equal x-coordinates, ascending order of y-coordinate) collection of two-dimensional points `Point2D[] s` by the function call

```
Edge[] diagram = delaunay(s);
```

This will return an edge tuple `[le, re]`, which are the counterclockwise convex hull edge out of the leftmost vertex and the clockwise convex hull edge out of the rightmost vertex, respectively.
