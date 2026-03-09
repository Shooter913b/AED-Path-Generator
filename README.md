# AED-Path-Generator

## `generatePath` algorithm

The `generatePath` method computes a collision‑avoiding route from a starting point to a target point while respecting circular “zones” (regions the path must route around). Conceptually, it works in three major phases on each iteration: **initialization**, **local obstacle enclosure**, and **routing along the enclosure**.

### 1. Initialization

- **Trivial straight‑line case**
  - If there are no zones, the algorithm immediately returns the straight segment from `start` to `target`.
  - This is the shortest possible path and requires no additional geometry.

- **Working sets**
  - A mutable list of zones, `remainingZones`, is created so circles can be removed once they have been fully handled; this prevents repeating work.
  - A stack `points` is used to store the sequence of waypoints along the final path.
    - Initially, only the `start` point is pushed.
    - The main loop continues until the top of this stack has the same coordinates as `target`.
  - **Time complexity:** O(1) for this setup.

### 2. Local obstacle enclosure around the current segment

Each iteration considers the line segment from the current point at the top of the stack to the target and builds a polygonal “envelope” around any blocking circles on that segment.

- **2.1. Find nearest blocking circle using line–circle intersections**
  - For each remaining zone, the algorithm computes the intersection of the segment from `current` to `target` with the circle surrounding that zone.
    - This uses the standard analytic line–circle intersection:
      - Parameterize the line segment as P(t) = L + t(T − L) for t in [0, 1], where L is `current` and T is `target`.
      - Substitute P(t) into the circle equation |P(t) − C|² = r², where C is the circle center and r is the (inflated) radius.
      - This produces a quadratic At² + Bt + C = 0; the discriminant determines whether there is an intersection, and the roots t1, t2 within [0, 1] give the actual intersection points.
  - Zones that do not intersect the current segment are ignored.
  - Among intersecting zones, a priority queue orders circles by the squared distance from `current` to their nearest intersection point, so the algorithm always handles the closest blocking circle first.
  - **Time complexity (per iteration):**
    - Checking intersections for all remaining zones is O(n).
    - Inserting intersecting zones into the priority queue is O(k log k) where k ≤ n.
    - Overall for this step: O(n log n) in the worst case.

- **2.2. Build a circumscribed square around the first blocking circle**
  - For the nearest intersecting circle, the algorithm constructs a square that tightly encloses a larger circle (scaled by sqrt(2)) so that the original zone lies entirely inside the square.
  - One corner of the square is chosen to be the intersection point on the line from `current` to `target` that is closest to `target`. This anchors the square relative to the path direction.
  - The remaining three corners are computed by:
    - Measuring the angle from the zone center to this first corner.
    - Rotating this direction by 90°, 180°, and 270° around the center, each at radius r * sqrt(2).
  - This yields an axis‑rotated square centered on the zone whose sides are tangent to the inflated circle.
  - **Time complexity (per iteration):** O(1) to build this first square.

- **2.3. Expand the enclosure to cover neighboring zones**
  - The algorithm keeps a list of all squares built for this iteration and a set of circles already “processed”.
  - While it can still add new squares:
    - It looks for any remaining circle that intersects at least one existing square.
      - Intersection is detected using three geometric checks:
        1. Any square vertex lying inside the circle (distance from center <= r).
        2. The circle center lying inside the square polygon (using a consistent cross‑product sign test).
        3. The minimum distance from the circle center to each square edge being less than or equal to the circle radius (projection of the center onto each edge segment).
    - For each such circle, a new circumscribed square is built, oriented using the line from the center of the intersected square to the circle’s center.
  - When no additional circles intersect any square, all circles that contributed to the enclosure are removed from `remainingZones`. The collection of squares now forms a connected “block” around the relevant obstacles for this iteration.
  - **Time complexity:**
    - In one iteration, each remaining circle is checked against the current list of squares. A single intersection check is O(1), but for m squares this is O(m) per circle.
    - In the worst case, across all iterations, each of the n circles can be paired with O(n) squares before being removed, giving an overall upper bound of O(n²) work for this expansion stage over the entire run.

- **2.4. Compute the convex hull of all square corners**
  - The four corners of every square in the block are collected into a point set.
  - The algorithm runs a 2D monotone chain (a.k.a. Andrew’s algorithm) to compute the convex hull:
    - Points are sorted lexicographically by (x, y).
    - A lower hull is built by scanning from left to right and repeatedly discarding the last point whenever adding the next point would make a non‑left turn (cross product <= 0).
    - An upper hull is built by scanning from right to left with the same left‑turn condition.
    - The last point of each list is removed to avoid duplication, and the remaining lower and upper chains are concatenated.
  - The resulting hull is a counterclockwise polygon that tightly wraps all the squares built in this iteration.
  - **Time complexity (per iteration):**
    - If there are m squares in this enclosure, there are 4m points.
    - Monotone chain convex hull runs in O(p log p) where p is the number of points, so this step is O(m log m).
    - Since m <= n, the worst case per iteration is O(n log n).

### 3. Routing along the convex hull

With the local obstacle region approximated by a convex polygon, the algorithm chooses the shorter of the two possible ways around it and extends the path accordingly.

- **3.1. Choose entry and exit vertices**
  - Among all vertices of the convex hull, the **entry** vertex is the one with minimum squared Euclidean distance to the current point.
  - The **exit** vertex is the one with minimum squared distance to the target.
  - Squared distances are used here because only relative ordering matters, and avoiding the square root slightly simplifies computation.
  - **Time complexity (per iteration):** O(h) where h is the number of hull vertices (h <= 4m <= 4n).

- **3.2. Compare clockwise vs counterclockwise routes**
  - Because the hull vertices are stored in counterclockwise order, there are exactly two simple polygonal routes from entry to exit:
    - Moving forward through the list (wrapping around) corresponds to one direction around the hull.
    - Moving backward through the list (wrapping around) corresponds to the opposite direction.
  - For each direction, the algorithm walks along consecutive vertices from entry to exit and sums the true Euclidean lengths of those edges (square root of squared distances) to approximate the path length around the obstacle.
  - The direction with the smaller total length is chosen as the “better” side to pass the obstacle.
  - **Time complexity (per iteration):** O(h) to walk both directions along the hull and sum the edge lengths.

- **3.3. Update the path stack**
  - The sequence of hull vertices along the chosen route (from entry to exit, inclusive) is pushed onto the `points` stack.
  - These vertices become new waypoints that bend the path around the obstacle region defined by the enclosure.
  - When control returns to the top of the main loop, the new top of the stack becomes the current point for the next iteration, and the process repeats with any zones that remain unprocessed.
  - **Time complexity (per iteration):** O(h) to push the new vertices; in the worst case h is O(n).

Ultimately, when there are no blocking zones left between the current point and the target, the algorithm finishes by adding a straight segment to the target. The stack `points` then encodes a polyline that approximates a short route from `start` to `target` while staying outside all circular zones, using local convex enclosures and shortest‑side traversal around each enclosure.

### Overall time complexity

- Each circle is:
  - Considered in intersection tests a bounded number of times.
  - Added to the enclosure at most once and then removed from `remainingZones`.
- Across all iterations:
  - Line–circle intersection and priority‑queue work contributes roughly O(n log n).
  - Square‑expansion and intersection checks can require up to O(n²) work in the worst case.
  - Convex hull computations add O(n log n) per enclosure; since enclosures grow as circles are consumed, a conservative global bound is O(n² log n), while in many practical layouts the effective cost is closer to O(n²).

Putting these together, a safe asymptotic upper bound for `generatePath` over n zones is:

- **Overall time complexity:** O(n² log n) in the worst case.