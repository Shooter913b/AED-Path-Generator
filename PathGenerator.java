import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

public class PathGenerator{

    public static void generatePath(CoordinatePlane plane, CoordinatePlane.Point2D target, List<Circle> zones){

    }

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

        Circle c1 = new Circle(new CoordinatePlane.Point2D(150, 88), 100);
        Square s1 = circumscribedSquare(new CoordinatePlane.Point2D(0, 0), new CoordinatePlane.Point2D(400,350), c1);
        plane.addLine(0, 0, 400, 350);
        plane.addCircle(c1.center.x, c1.center.y, c1.r);
        //plane.addCircle(c1.center.x, c1.center.y, c1.r*Math.pow(2, 0.5));
        plane.addSquare(s1.center.x, s1.center.y, s1.pt1.x, s1.pt1.y);
        plane.updatePlane();
        //generatePath(plane, new CoordinatePlane.Point2D(100, 0));
    }

    public static Square circumscribedSquare(CoordinatePlane.Point2D last, CoordinatePlane.Point2D target, Circle zone){
        //sqrt2 = 1.4142
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
        double A,B,C,x0,y0,b,m,h,j,r,x1,x2;

        //(x - h)^2 + (y - j)^2 = r^2
        h = zone.center.x;
        j = zone.center.y;
        r = Math.pow(2,0.5)*zone.r;

        //y = m( x - a) + b
        //m = (y2 - y1 / x2 - x1)
        m = 1.0 * (target.y - last.y) / (target.x - last.x);
        x0 = last.x;
        y0 = last.y;
        b =  y0 - m*x0;

        //x^2 (m^2 + 1) + x (2mb - 2mj - 2h) + (h^2 + j^2 + b^2 - 2bj) = r^2
        // A = m^2 + 1 | B = 2mb - 2mj - 2h | C = h^2 + j^2 + b^2 - 2bj - r^2
        A = Math.pow(m, 2) + 1;
        B = 2*m*b - 2*m*j - 2*h;
        C = Math.pow(h, 2) + Math.pow(j, 2) + Math.pow(b, 2) - Math.pow(r, 2) - 2*b*j;

        // x = (-B +- sqrt(B^2 - 4AC))/2A
        x1 = (-1*B + Math.pow(Math.pow(B, 2) - 4*A*C, 0.5))/(2*A);
        x2 = (-1*B - Math.pow(Math.pow(B, 2) - 4*A*C, 0.5))/(2*A);

        CoordinatePlane.Point2D pt1 = new CoordinatePlane.Point2D(x1, m*(x1 - x0) + y0);
        CoordinatePlane.Point2D pt2 = new CoordinatePlane.Point2D(x2, m*(x2 - x0) + y0);
        List<CoordinatePlane.Point2D> solution = new ArrayList<>();
        solution.add(pt1);
        solution.add(pt2);

        return solution;
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

    class Shape{

    }

}