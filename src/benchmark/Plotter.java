package benchmark;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/** PNG charts drawn entirely with Java2D: no Python or chart library required. */
public final class Plotter {
    private Plotter() { }
    private static final int WIDTH = 1250, HEIGHT = 780;
    private static final int LEFT = 115, RIGHT = 285, TOP = 115, BOTTOM = 135;
    private static final String[] SERIES = {
            "Kruskal", "Prim", "Topological DAG", "Topological cyclic", "Kosaraju SCC", "Dijkstra"
    };
    private static final Color[] COLORS = {
            new Color(21, 91, 150), new Color(222, 114, 31),
            new Color(40, 136, 101), new Color(142, 91, 163),
            new Color(199, 61, 77), new Color(85, 101, 108)
    };
    public static void createCharts(List<Benchmark.Row> rows) throws IOException {
        Path dir = Path.of("results", "plots");
        Files.createDirectories(dir);
        render(rows, true, dir.resolve("time_vs_vertices.png"));
        render(rows, false, dir.resolve("time_vs_edges.png"));
    }
    private static void render(List<Benchmark.Row> rows, boolean vertices, Path output)
            throws IOException {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE); g.fillRect(0, 0, WIDTH, HEIGHT);
            int plotW = WIDTH - LEFT - RIGHT, plotH = HEIGHT - TOP - BOTTOM;
            g.setFont(new Font("SansSerif", Font.BOLD, 27));
            g.setColor(new Color(31, 46, 63));
            g.drawString("Algorithm execution time vs " + (vertices ? "vertices" : "edges"), LEFT, 49);
            g.setFont(new Font("SansSerif", Font.PLAIN, 15));
            g.drawString("Fixed-seed graphs | 30 JVM warmups + 5 measured runs | logarithmic horizontal axis",
                    LEFT, 76);
            double xMin = Double.POSITIVE_INFINITY, xMax = 0, yMax = 0;
            for (Benchmark.Row r : rows) {
                double x = Math.log10(vertices ? r.vertices() : r.edges());
                xMin = Math.min(xMin, x); xMax = Math.max(xMax, x);
                yMax = Math.max(yMax, r.milliseconds());
            }
            yMax = Math.max(yMax * 1.12, 0.00001);
            g.setStroke(new BasicStroke(1));
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            for (int n = 0; n <= 5; n++) {
                int y = TOP + plotH - n * plotH / 5;
                g.setColor(new Color(228, 233, 240));
                g.drawLine(LEFT, y, LEFT + plotW, y);
                g.setColor(new Color(76, 89, 105));
                String tick = String.format(Locale.US, "%.4f", yMax * n / 5);
                g.drawString(tick, LEFT - 82, y + 5);
            }
            int[] xs = vertices ? new int[]{10, 100, 1000} : new int[]{30, 500, 5000};
            for (int value : xs) {
                int x = LEFT + (int) Math.round((Math.log10(value) - xMin) / (xMax - xMin) * plotW);
                g.setColor(new Color(228, 233, 240));
                g.drawLine(x, TOP, x, TOP + plotH);
                g.setColor(new Color(76, 89, 105));
                String label = String.format(Locale.US, "%,d", value);
                g.drawString(label, x - g.getFontMetrics().stringWidth(label) / 2, TOP + plotH + 30);
            }
            g.setColor(new Color(42, 56, 73));
            g.setStroke(new BasicStroke(2));
            g.drawLine(LEFT, TOP, LEFT, TOP + plotH);
            g.drawLine(LEFT, TOP + plotH, LEFT + plotW, TOP + plotH);
            g.setFont(new Font("SansSerif", Font.BOLD, 17));
            String xLabel = vertices ? "Number of vertices (log scale)" : "Number of edges (log scale)";
            g.drawString(xLabel, LEFT + (plotW - g.getFontMetrics().stringWidth(xLabel)) / 2,
                    HEIGHT - 48);
            AffineTransform old = g.getTransform();
            g.rotate(-Math.PI / 2);
            g.drawString("Mean execution time (milliseconds)", -(TOP + plotH / 2 + 130), 26);
            g.setTransform(old);
            for (int series = 0; series < SERIES.length; series++) {
                List<Benchmark.Row> selected = new ArrayList<>();
                for (Benchmark.Row r : rows) if (r.algorithm().equals(SERIES[series])) selected.add(r);
                selected.sort(Comparator.comparingInt(r -> vertices ? r.vertices() : r.edges()));
                g.setColor(COLORS[series]);
                g.setStroke(new BasicStroke(3.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int oldX = -1, oldY = -1;
                for (Benchmark.Row r : selected) {
                    int x = LEFT + (int) Math.round((Math.log10(vertices ? r.vertices() : r.edges()) - xMin)
                            / (xMax - xMin) * plotW);
                    int y = TOP + plotH - (int) Math.round(r.milliseconds() / yMax * plotH);
                    if (oldX >= 0) g.drawLine(oldX, oldY, x, y);
                    g.fill(new Ellipse2D.Double(x - 5, y - 5, 10, 10));
                    oldX = x; oldY = y;
                }
            }
            int legendX = WIDTH - RIGHT + 30;
            g.setFont(new Font("SansSerif", Font.BOLD, 17));
            g.setColor(new Color(31, 46, 63));
            g.drawString("Algorithms", legendX, TOP + 13);
            g.setFont(new Font("SansSerif", Font.PLAIN, 15));
            for (int i = 0; i < SERIES.length; i++) {
                int y = TOP + 53 + i * 39;
                g.setColor(COLORS[i]);
                g.setStroke(new BasicStroke(3));
                g.drawLine(legendX, y, legendX + 27, y);
                g.fillOval(legendX + 11, y - 4, 8, 8);
                g.setColor(new Color(31, 46, 63));
                g.drawString(SERIES[i], legendX + 38, y + 5);
            }
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(90, 104, 120));
            g.drawString("Timings depend on JVM and machine; values in CSV are authoritative.", LEFT, HEIGHT - 18);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", output.toFile());
    }
}
