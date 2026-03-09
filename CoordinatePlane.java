import javax.swing.*;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

public class CoordinatePlane extends JPanel {

    private List<Drawable> shapes = new ArrayList<>();
    private int scale = 1;

    public CoordinatePlane() {
        setBackground(Color.BLACK);
    }

    // ---------------- PUBLIC API ----------------

    public void addCircle(double x, double y, double r) {
        shapes.add(new Circle(x, y, r));
    }

    public void addLine(double x1, double y1, double x2, double y2) {
        shapes.add(new Line(x1, y1, x2, y2));
    }

    public void addSquare(double cx, double cy, double cornerX, double cornerY) {
        shapes.add(new Square(cx, cy, cornerX, cornerY));
    }

    public void addConvexHull(List<Point2D> points) {
        shapes.add(new ConvexHull(points));
    }

    public void updatePlane() {
        repaint();
    }

    // ---------------- DRAWING ----------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        drawAxes(g2);

        for (Drawable shape : shapes) {
            shape.draw(g2, this);
        }
    }

    private void drawAxes(Graphics2D g) {
        int originX = getWidth() / 2;
        int originY = getHeight() / 2;

        g.setColor(Color.GRAY);

        g.drawLine(0, originY, getWidth(), originY);
        g.drawLine(originX, 0, originX, getHeight());
    }

    // ---------------- COORDINATE CONVERSION ----------------

    public int toScreenX(double x) {
        return getWidth() / 2 + (int) (x * scale);
    }

    public int toScreenY(double y) {
        return getHeight() / 2 - (int) (y * scale);
    }

    public int scale(double v) {
        return (int) (v * scale);
    }

    // ---------------- DRAWABLE INTERFACE ----------------

    interface Drawable {
        void draw(Graphics2D g, CoordinatePlane plane);
    }

    // ---------------- POINT CLASS ----------------

    public static class Point2D {
        public double x, y;

        public Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    // ---------------- CIRCLE ----------------

    static class Circle implements Drawable {

        public double x, y, r;

        Circle(double x, double y, double r) {
            this.x = x;
            this.y = y;
            this.r = r;
        }

        public void draw(Graphics2D g, CoordinatePlane plane) {

            int cx = plane.toScreenX(x);
            int cy = plane.toScreenY(y);
            int pr = plane.scale(r);

            g.setColor(Color.BLUE);

            g.drawOval(cx - pr, cy - pr, pr * 2, pr * 2);
        }
    }

    // ---------------- LINE ----------------

    static class Line implements Drawable {

        double x1, y1, x2, y2;

        Line(double x1, double y1, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        public void draw(Graphics2D g, CoordinatePlane plane) {

            g.setColor(Color.CYAN);

            g.drawLine(
                    plane.toScreenX(x1),
                    plane.toScreenY(y1),
                    plane.toScreenX(x2),
                    plane.toScreenY(y2)
            );
        }
    }

    // ---------------- SQUARE ----------------

    static class Square implements Drawable {

        double cx, cy;
        double cornerX, cornerY;

        Square(double cx, double cy, double cornerX, double cornerY) {
            this.cx = cx;
            this.cy = cy;
            this.cornerX = cornerX;
            this.cornerY = cornerY;
        }

        public void draw(Graphics2D g, CoordinatePlane plane) {

            double dx = cornerX - cx;
            double dy = cornerY - cy;

            double r = Math.sqrt(dx * dx + dy * dy);
            double angle = Math.atan2(dy, dx);

            int[] xs = new int[4];
            int[] ys = new int[4];

            for (int i = 0; i < 4; i++) {

                double a = angle + i * Math.PI / 2;

                double x = cx + r * Math.cos(a);
                double y = cy + r * Math.sin(a);

                xs[i] = plane.toScreenX(x);
                ys[i] = plane.toScreenY(y);
            }

            g.setColor(Color.GREEN);
            g.drawPolygon(xs, ys, 4);
        }
    }

    // ---------------- CONVEX HULL ----------------

    static class ConvexHull implements Drawable {

        List<Point2D> points;
        List<Point2D> hull;

        ConvexHull(List<Point2D> points) {
            this.points = points;
            this.hull = computeHull(points);
        }

        public void draw(Graphics2D g, CoordinatePlane plane) {

            if (hull.size() < 3)
                return;

            int[] xs = new int[hull.size()];
            int[] ys = new int[hull.size()];

            for (int i = 0; i < hull.size(); i++) {
                xs[i] = plane.toScreenX(hull.get(i).x);
                ys[i] = plane.toScreenY(hull.get(i).y);
            }

            g.setColor(Color.MAGENTA);
            g.drawPolygon(xs, ys, hull.size());

            // draw points
            for (Point2D p : points) {
                int x = plane.toScreenX(p.x);
                int y = plane.toScreenY(p.y);
                g.fillOval(x - 3, y - 3, 6, 6);
            }
        }

        private List<Point2D> computeHull(List<Point2D> pts) {

            List<Point2D> sorted = new ArrayList<>(pts);

            sorted.sort((a, b) -> {
                if (a.x == b.x)
                    return Double.compare(a.y, b.y);
                return Double.compare(a.x, b.x);
            });

            List<Point2D> lower = new ArrayList<>();

            for (Point2D p : sorted) {
                while (lower.size() >= 2 &&
                        cross(lower.get(lower.size() - 2),
                                lower.get(lower.size() - 1),
                                p) <= 0) {

                    lower.remove(lower.size() - 1);
                }

                lower.add(p);
            }

            List<Point2D> upper = new ArrayList<>();

            for (int i = sorted.size() - 1; i >= 0; i--) {

                Point2D p = sorted.get(i);

                while (upper.size() >= 2 &&
                        cross(upper.get(upper.size() - 2),
                                upper.get(upper.size() - 1),
                                p) <= 0) {

                    upper.remove(upper.size() - 1);
                }

                upper.add(p);
            }

            lower.remove(lower.size() - 1);
            upper.remove(upper.size() - 1);

            lower.addAll(upper);

            return lower;
        }

        private double cross(Point2D a, Point2D b, Point2D c) {
            return (b.x - a.x) * (c.y - a.y) -
                    (b.y - a.y) * (c.x - a.x);
        }
    }

    // ---------------- DEMO ----------------

    public static void main(String[] args) {

        JFrame frame = new JFrame("Coordinate Plane");

        CoordinatePlane plane = new CoordinatePlane();

        plane.addCircle(2, 1, 1);
        plane.addLine(-3, -2, 4, 3);
        plane.addSquare(2, 0, 3, 1);
        plane.addSquare(0, 0, 0, Math.pow(2, 0.5));


        List<Point2D> pts = new ArrayList<>();
        pts.add(new Point2D(3, 1));
        pts.add(new Point2D(3, -1));
        pts.add(new Point2D(1, 1));
        pts.add(new Point2D(1, -1));
        pts.add(new Point2D(Math.pow(2, 0.5), 0));
        pts.add(new Point2D(-Math.pow(2, 0.5), 0));
        pts.add(new Point2D(0, Math.pow(2, 0.5)));
        pts.add(new Point2D(0, -Math.pow(2, 0.5)));

        plane.addConvexHull(pts);

        frame.add(plane);

        frame.setSize(600, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        plane.updatePlane();
    }
}