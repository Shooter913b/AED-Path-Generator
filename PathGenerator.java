import java.awt.geom.Point2D;
import java.util.*;

import javax.swing.JFrame;

public class PathGenerator{

    public static void main(String[] args){
        JFrame frame = new JFrame("Coordinate Plane");

        CoordinatePlane plane = new CoordinatePlane();

        /*List<CoordinatePlane.Point2D> pts = new ArrayList<>();
        pts.add(new CoordinatePlane.Point2D(3, 1));
        pts.add(new CoordinatePlane.Point2D(3, -1));
        pts.add(new CoordinatePlane.Point2D(1, 1));
        pts.add(new CoordinatePlane.Point2D(1, -1));
        pts.add(new CoordinatePlane.Point2D(1, 1));
        pts.add(new CoordinatePlane.Point2D(1, -1));
        pts.add(new CoordinatePlane.Point2D(2,2 ));
        pts.add(new CoordinatePlane.Point2D(2,3 ));

        plane.addConvexHull(pts);*/

        frame.add(plane);
        frame.setSize(600, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        /*Circle c1 = new Circle(new CoordinatePlane.Point2D(150, 88), 100);
        Square s1 = circumscribedSquare(new CoordinatePlane.Point2D(0, 0), new CoordinatePlane.Point2D(400,450), c1);
        plane.addLine(0, 0, 400, 450);
        plane.addCircle(c1.center.x, c1.center.y, c1.r);
        //plane.addCircle(c1.center.x, c1.center.y, c1.r*Math.pow(2, 0.5));
        plane.addSquare(s1.center.x, s1.center.y, s1.pt1.x, s1.pt1.y);
        plane.updatePlane();*/
        List<Circle> zones = new ArrayList<>();
        Circle c1 = new Circle(new CoordinatePlane.Point2D(0, 0), 50); zones.add(c1);//c1
        Circle c2 = new Circle(new CoordinatePlane.Point2D(-50, 50), 50); zones.add(c2);//c2
        //Circle c3 = new Circle(new CoordinatePlane.Point2D(50, 100), 50); zones.add(c3);//3 convex
        Circle c4 = new Circle(new CoordinatePlane.Point2D(75, 150), 50); zones.add(c4);// seperate conex
        //Circle c5 = new Circle(new CoordinatePlane.Point2D(-75, 150), 50); zones.add(c4);//non intersecting
        generatePath(plane, new CoordinatePlane.Point2D(-100, -200), new CoordinatePlane.Point2D(200, 250), zones);
    }
    
    public static void generatePath(CoordinatePlane plane, CoordinatePlane.Point2D start, CoordinatePlane.Point2D target, List<Circle> zones){
        int sleep = 750;
        
        if(zones.isEmpty()){
            plane.addLine(start.x, start.y, target.x, target.y);
            plane.updatePlane();
        }
        try {
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        //using a for loop, draw all zones onto plane
        for(Circle zone : zones){
            plane.addCircle(zone.center.x, zone.center.y, zone.r);
        }
        plane.updatePlane();

        try {
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // make a mutable working copy of zones so circles are not reprocessed forever
        List<Circle> remainingZones = new ArrayList<>(zones);

        Stack<CoordinatePlane.Point2D> points = new Stack<>();
        points.push(start);

        // loop until the top of the stack reaches the target coordinates
        while(points.peek().x != target.x || points.peek().y != target.y){
            CoordinatePlane.Point2D current = points.peek();
            PriorityQueue<Circle> zonesQ = new PriorityQueue<>(new Comparator<Circle>() {
                @Override
                public int compare(Circle z1, Circle z2) {
                    double d1 = distanceToClosestIntersection(current, target, z1);
                    double d2 = distanceToClosestIntersection(current, target, z2);
                    return Double.compare(d1, d2);
                }
            });

            for(Circle zone : remainingZones){
                List<CoordinatePlane.Point2D> intersections = lineCircleIntersection(current, target, zone);
                if(!intersections.isEmpty()){
                    zonesQ.add(zone);
                }
            }

            if(zonesQ.isEmpty()){
                //draw the final line from the top of stack to target
                CoordinatePlane.Point2D top = points.peek();
                plane.addLine(top.x, top.y, target.x, target.y);
                plane.updatePlane();
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                points.add(target);
                break;
            }
            Circle closestZone = zonesQ.poll();
            Square firstSquare = circumscribedSquare(current, target, closestZone);
            //draw this square. have a sleep of time sleep
            plane.addSquare(firstSquare.center.x, firstSquare.center.y, firstSquare.pt1.x, firstSquare.pt1.y);
            plane.updatePlane();
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            List<Square> squares = new ArrayList<>();
            squares.add(firstSquare);

            Set<Circle> processed = new HashSet<>();
            processed.add(closestZone);

            boolean added = true;
            while(added){
                added = false;
                for(Circle zone : remainingZones){
                    Square intersectingSquare = null;
                    if(processed.contains(zone)){
                        continue;
                    }
                    boolean intersectsAnySquare = false;
                    for(Square square : squares){
                        if(squareIntersectsCircle(square, zone)){
                            intersectsAnySquare = true;
                            intersectingSquare = square;
                            break;
                        }
                    }
                    if(intersectsAnySquare){
                        Square newSquare = circumscribedSquare(zone.center, intersectingSquare.center, zone);
                        //draw this square each iteration. have a sleep of time sleep
                        plane.addSquare(newSquare.center.x, newSquare.center.y, newSquare.pt1.x, newSquare.pt1.y);
                        plane.updatePlane();
                        try {
                            Thread.sleep(sleep);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        squares.add(newSquare);
                        processed.add(zone);
                        added = true;
                    }
                }
            }

            // remove all circles we just processed so they are not considered again
            if(!processed.isEmpty()){
                remainingZones.removeAll(processed);
            }

            Shape hull = convexHullFromSquares(squares);
            //draw this hull. have a sleep of time sleep
            if(hull != null && hull.vertices != null && !hull.vertices.isEmpty()){
                plane.addConvexHull(hull.vertices);
                plane.updatePlane();
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            int beforeSize = points.size();
            addShortestHullPath(points, hull, current, target);
            //draw the new line segments in the path. have a sleep of time sleep
            for(int i = beforeSize - 1; i < points.size() - 1; i++){
                CoordinatePlane.Point2D p1 = points.get(i);
                CoordinatePlane.Point2D p2 = points.get(i + 1);
                plane.addLine(p1.x, p1.y, p2.x, p2.y);
                plane.updatePlane();
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public static Square circumscribedSquare(CoordinatePlane.Point2D last, CoordinatePlane.Point2D target, Circle zone){
        double outerR = Math.pow(2,0.5)*zone.r;
        CoordinatePlane.Point2D corner1 = closestPoint(target, lineCircleIntersection(last, target, zone));

        double angle = Math.atan2(corner1.y-zone.center.y,corner1.x-zone.center.x);
        double ninety = Math.PI/2;
        CoordinatePlane.Point2D corner2 = new CoordinatePlane.Point2D(zone.center.x + outerR*Math.cos(angle + ninety),zone.center.y + outerR*Math.sin(angle + ninety));
        CoordinatePlane.Point2D corner3 = new CoordinatePlane.Point2D(zone.center.x + outerR*Math.cos(angle + 2*ninety),zone.center.y + outerR*Math.sin(angle + 2*ninety));
        CoordinatePlane.Point2D corner4 = new CoordinatePlane.Point2D(zone.center.x + outerR*Math.cos(angle + 3*ninety),zone.center.y + outerR*Math.sin(angle + 3*ninety));


        return new Square(zone.center, corner1, corner2, corner3, corner4);
    }

    public static List<CoordinatePlane.Point2D> lineCircleIntersection(CoordinatePlane.Point2D last, CoordinatePlane.Point2D target, Circle zone){
        double r = Math.sqrt(2) * zone.r;

        double dx = target.x - last.x;
        double dy = target.y - last.y;

        double fx = last.x - zone.center.x;
        double fy = last.y - zone.center.y;

        double A = dx*dx + dy*dy;
        double B = 2*(fx*dx + fy*dy);
        double C = fx*fx + fy*fy - r*r;

        double discriminant = B*B - 4*A*C;

        List<CoordinatePlane.Point2D> solutions = new ArrayList<>();

        //returns an empty list if no solutions(no intersection)
        if(discriminant < 0){
            return solutions;
        }

        if (discriminant <= 0) return solutions;

        discriminant = Math.sqrt(discriminant);

        double t1 = (-B + discriminant) / (2*A);
        double t2 = (-B - discriminant) / (2*A);

        if(t1 <= 1 && t1 >= 0 ){
            CoordinatePlane.Point2D p1 = new CoordinatePlane.Point2D(
                last.x + t1*dx,
                last.y + t1*dy
            );
            solutions.add(p1);
        }

        if(t2 <= 1 && t2 >= 0 ){
            CoordinatePlane.Point2D p2 = new CoordinatePlane.Point2D(
                last.x + t2*dx,
                last.y + t2*dy
            );
            solutions.add(p2);
        }

        return solutions;
    }

    public static CoordinatePlane.Point2D closestPoint(CoordinatePlane.Point2D target, List<CoordinatePlane.Point2D> points){
        double minD = Double.MAX_VALUE;
        CoordinatePlane.Point2D closestPoint = points.get(0);

        for(CoordinatePlane.Point2D point : points){
            double d = Math.pow(target.x - point.x, 2) + Math.pow(target.y - point.y, 2);
            if(d < minD){
                minD = d;
                closestPoint = point;
            }
        }

        return closestPoint;
    }
    
    private static double distanceSquared(CoordinatePlane.Point2D a, CoordinatePlane.Point2D b){
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        return dx*dx + dy*dy;
    }

    private static double distanceToClosestIntersection(CoordinatePlane.Point2D current, CoordinatePlane.Point2D target, Circle zone){
        List<CoordinatePlane.Point2D> intersections = lineCircleIntersection(current, target, zone);
        if(intersections.isEmpty()){
            return Double.MAX_VALUE;
        }
        CoordinatePlane.Point2D closest = closestPoint(current, intersections);
        return distanceSquared(current, closest);
    }
    
    private static boolean squareIntersectsCircle(Square square, Circle circle){
        CoordinatePlane.Point2D[] pts = new CoordinatePlane.Point2D[]{square.pt1, square.pt2, square.pt3, square.pt4};

        for(CoordinatePlane.Point2D p : pts){
            if(distanceSquared(p, circle.center) <= (double)circle.r * (double)circle.r){
                return true;
            }
        }

        if(pointInPolygon(circle.center, pts)){
            return true;
        }

        for(int i = 0; i < pts.length; i++){
            CoordinatePlane.Point2D a = pts[i];
            CoordinatePlane.Point2D b = pts[(i + 1) % pts.length];
            if(distancePointToSegment(circle.center, a, b) <= circle.r){
                return true;
            }
        }

        return false;
    }

    private static boolean pointInPolygon(CoordinatePlane.Point2D p, CoordinatePlane.Point2D[] poly){
        int n = poly.length;
        if(n < 3){
            return false;
        }

        double prevCross = 0;
        for(int i = 0; i < n; i++){
            CoordinatePlane.Point2D a = poly[i];
            CoordinatePlane.Point2D b = poly[(i + 1) % n];
            double cross = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x);
            if(i == 0){
                prevCross = cross;
            }else{
                if(cross * prevCross < 0){
                    return false;
                }
            }
        }
        return true;
    }

    private static double distancePointToSegment(CoordinatePlane.Point2D p, CoordinatePlane.Point2D a, CoordinatePlane.Point2D b){
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        if(dx == 0 && dy == 0){
            return Math.sqrt(distanceSquared(p, a));
        }

        double t = ((p.x - a.x) * dx + (p.y - a.y) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));

        double projX = a.x + t * dx;
        double projY = a.y + t * dy;

        double ddx = p.x - projX;
        double ddy = p.y - projY;

        return Math.sqrt(ddx * ddx + ddy * ddy);
    }

    private static Shape convexHullFromSquares(List<Square> squares){
        List<CoordinatePlane.Point2D> points = new ArrayList<>();
        for(Square s : squares){
            points.add(s.pt1);
            points.add(s.pt2);
            points.add(s.pt3);
            points.add(s.pt4);
        }

        if(points.size() <= 1){
            return new Shape(new ArrayList<>(points));
        }

        // Sort points by x, then by y
        Collections.sort(points, new Comparator<CoordinatePlane.Point2D>() {
            @Override
            public int compare(CoordinatePlane.Point2D p1, CoordinatePlane.Point2D p2) {
                if(p1.x != p2.x){
                    return Double.compare(p1.x, p2.x);
                }
                return Double.compare(p1.y, p2.y);
            }
        });

        List<CoordinatePlane.Point2D> lower = new ArrayList<>();
        for(CoordinatePlane.Point2D p : points){
            while(lower.size() >= 2 && cross(lower.get(lower.size() - 2), lower.get(lower.size() - 1), p) <= 0){
                lower.remove(lower.size() - 1);
            }
            lower.add(p);
        }

        List<CoordinatePlane.Point2D> upper = new ArrayList<>();
        for(int i = points.size() - 1; i >= 0; i--){
            CoordinatePlane.Point2D p = points.get(i);
            while(upper.size() >= 2 && cross(upper.get(upper.size() - 2), upper.get(upper.size() - 1), p) <= 0){
                upper.remove(upper.size() - 1);
            }
            upper.add(p);
        }

        // Remove last point of each list (it's the starting point of the other list)
        if(!lower.isEmpty()){
            lower.remove(lower.size() - 1);
        }
        if(!upper.isEmpty()){
            upper.remove(upper.size() - 1);
        }

        List<CoordinatePlane.Point2D> hullPoints = new ArrayList<>();
        hullPoints.addAll(lower);
        hullPoints.addAll(upper);

        return new Shape(hullPoints);
    }

    private static double cross(CoordinatePlane.Point2D o, CoordinatePlane.Point2D a, CoordinatePlane.Point2D b){
        return (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x);
    }

    private static void addShortestHullPath(Stack<CoordinatePlane.Point2D> points, Shape hull, CoordinatePlane.Point2D current, CoordinatePlane.Point2D target){
        if(hull == null || hull.vertices == null || hull.vertices.isEmpty()){
            return;
        }

        List<CoordinatePlane.Point2D> vs = hull.vertices;
        int n = vs.size();

        // Find entry: vertex closest to current
        int entryIdx = 0;
        double bestEntryDist = distanceSquared(current, vs.get(0));
        for(int i = 1; i < n; i++){
            double d = distanceSquared(current, vs.get(i));
            if(d < bestEntryDist){
                bestEntryDist = d;
                entryIdx = i;
            }
        }

        // Find exit: vertex closest to target
        int exitIdx = 0;
        double bestExitDist = distanceSquared(target, vs.get(0));
        for(int i = 1; i < n; i++){
            double d = distanceSquared(target, vs.get(i));
            if(d < bestExitDist){
                bestExitDist = d;
                exitIdx = i;
            }
        }

        if(n == 1){
            CoordinatePlane.Point2D only = vs.get(0);
            if(points.isEmpty() || points.peek() != only){
                points.push(only);
            }
            return;
        }

        // Compute path length going forward (in vertex order)
        double forwardLen = 0.0;
        int i = entryIdx;
        while(i != exitIdx){
            int j = (i + 1) % n;
            forwardLen += Math.sqrt(distanceSquared(vs.get(i), vs.get(j)));
            i = j;
        }

        // Compute path length going backward (reverse vertex order)
        double backwardLen = 0.0;
        i = entryIdx;
        while(i != exitIdx){
            int j = (i - 1 + n) % n;
            backwardLen += Math.sqrt(distanceSquared(vs.get(i), vs.get(j)));
            i = j;
        }

        // Build the chosen path from entry to exit
        List<CoordinatePlane.Point2D> path = new ArrayList<>();
        if(forwardLen <= backwardLen){
            i = entryIdx;
            path.add(vs.get(i));
            while(i != exitIdx){
                int j = (i + 1) % n;
                path.add(vs.get(j));
                i = j;
            }
        }else{
            i = entryIdx;
            path.add(vs.get(i));
            while(i != exitIdx){
                int j = (i - 1 + n) % n;
                path.add(vs.get(j));
                i = j;
            }
        }

        // Push path points (entry to exit) onto the stack
        for(CoordinatePlane.Point2D p : path){
            points.push(p);
        }
    }
    
    static class Circle{
        public CoordinatePlane.Point2D center;
        public int r;

        public Circle(CoordinatePlane.Point2D center, int r){
            this.center = center;
            this.r = r;
        }
    }

    static class Square{
        public CoordinatePlane.Point2D center, pt1, pt2, pt3, pt4;

        public Square(CoordinatePlane.Point2D center, CoordinatePlane.Point2D pt1, CoordinatePlane.Point2D pt2, CoordinatePlane.Point2D pt3, CoordinatePlane.Point2D pt4){
            this.center = center;
            this.pt1 = pt1;
            this.pt2 = pt2;
            this.pt3 = pt3;
            this.pt4 = pt4;
        }
    }

    static class Shape{
        public List<CoordinatePlane.Point2D> vertices;

        public Shape(List<CoordinatePlane.Point2D> vertices){
            this.vertices = vertices;
        }
    }

}