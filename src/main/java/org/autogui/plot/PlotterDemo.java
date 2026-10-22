package org.autogui.plot;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;

public class PlotterDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new PlotterDemo()::start);
    }

    public void start() {
        JFrame frame = new JFrame("PlotterDemo");
        {
            JPanel pane = new JPanel(new FlowLayout(FlowLayout.LEADING));
            {
                pane.add(new DemoPlotPane((g,w,h) -> this.plot1(g, w, h, this::initLight)));
                pane.add(new DemoPlotPane((g,w,h) -> this.plot2(g, w, h, this::initLight)));
                pane.add(new DemoPlotPane((g,w,h) -> this.plot3(g, w, h, this::initLight)));
                pane.add(new PlotterPane(plot4(700, 400, this::initLight)));
                pane.add(new PlotterPane(plot5(700, 400, this::initLight)));
                pane.add(new PlotterPane(plot6(700, 400, this::initLight)));
                pane.add(new PlotterPane(plot7(700, 400, this::initLight)));
                pane.add(new PlotterPane(plot8(700, 400, this::initLight)));
            }
            JPanel pane2 = new JPanel(new FlowLayout(FlowLayout.LEADING));
            {
                pane2.add(new DemoPlotPane((g, w, h) -> this.plot1(g, w, h, this::initDark)));
                pane2.add(new DemoPlotPane((g, w, h) -> this.plot2(g, w, h, this::initDark)));
                pane2.add(new DemoPlotPane((g, w, h) -> this.plot3(g, w, h, this::initDark)));
                pane2.add(new PlotterPane(plot4(700, 400, this::initDark)));
                pane2.add(new PlotterPane(plot5(700, 400, this::initDark)));
                pane2.add(new PlotterPane(plot6(700, 400, this::initDark)));
                pane2.add(new PlotterPane(plot7(700, 400, this::initDark)));
                pane2.add(new PlotterPane(plot8(700, 400, this::initDark)));
            }
            JPanel pane3 = new JPanel();
            pane3.setLayout(new BoxLayout(pane3, BoxLayout.Y_AXIS));
            pane3.add(pane);
            pane3.add(pane2);
            frame.setContentPane(new JScrollPane(pane3));
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        }
        frame.setVisible(true);
    }

    void initLight(PlotterState<?> p) {
        p.setPlotPaintForLight();
    }

    void initDark(PlotterState<?> p) {
        p.setPlotPaintForDark();
    }


    interface PlotFunction {
        void plot(Graphics2D g, int w, int h);
    }

    static class DemoPlotPane extends JPanel {
        JPopupMenu menu;
        PlotFunction f;
        DemoPlotPane(PlotFunction f) {
            this.f = f;
            setPreferredSize(new Dimension(700, 400));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        menu.show(DemoPlotPane.this, e.getX(), e.getY());
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        menu.show(DemoPlotPane.this, e.getX(), e.getY());
                    }
                }
            });

            menu = new JPopupMenu();
            menu.add(new PlotImageSaveAction(this));
            menu.add(new PlotPrintAction(this));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            paintPlot((Graphics2D) g, getWidth(), getHeight());
        }

        public void paintPlot(Graphics2D g) {
            paintPlot(g, getWidth(), getHeight());
        }

        public void paintPlot(Graphics2D g, int w, int h) {
            f.plot(g, w, h);
        }
    }

    static class PlotPrintAction extends AbstractAction implements PlotterPrint.PlotterPrintable {
        DemoPlotPane pane;
        public PlotPrintAction(DemoPlotPane pane) {
            this.pane = pane;
            putValue(NAME, "Print");
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            PlotterPrint print = new PlotterPrint();
            print.print(this);
        }

        @Override
        public void print(Graphics2D g) {
            pane.paintPlot(g);
        }

        @Override
        public double getWidth() {
            return pane.getWidth();
        }

        @Override
        public double getHeight() {
            return pane.getHeight();
        }
    }

    static class PlotImageSaveAction extends AbstractAction {
        DemoPlotPane pane;
        JFileChooser chooser;

        public PlotImageSaveAction(DemoPlotPane pane) {
            this.pane = pane;
            putValue(NAME, "Save...");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (chooser == null) {
                chooser = new JFileChooser();
            }
            int res = chooser.showSaveDialog(pane);
            if (res == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if (file.exists()) {
                    int confirmRes = JOptionPane.showConfirmDialog(pane, "File " + file.getName() + " already exists. Do you want to overwrite it?",
                            "File overwriting",
                            JOptionPane.YES_NO_OPTION);
                    if (confirmRes == JOptionPane.YES_OPTION) {
                        save(file);
                    }
                } else {
                    save(file);
                }
            }
        }
        private void save(File file) {
            BufferedImage img = Plotter.createImage(pane.getWidth(), pane.getHeight());
            Graphics2D g = img.createGraphics();
            pane.paintPlot(g);
            g.dispose();
            try {
                ImageIO.write(img, "png", file);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    void plot1(Graphics2D g, int w, int h, Consumer<Plotter> init) {
        Random r = new Random(123);
        Plotter p = new Plotter(g, w, h);
        init.accept(p);
        int rows = 5;
        p.splitArea(rows, 3);
        p.plotAreaBackground();
        p.plotFrame();
        p.plotColumnLabels("item-A", "item-B", "item-C");
        for (int i = 0; i < rows; ++i) {
            double a = r.nextDouble();
            double b = r.nextDouble();
            double c = r.nextDouble();
            p.plot("A" + i, String.format("%.2f", a), a);
            p.plot("B" + i, String.format("%.2f", b), b);
            p.plot("C" + i, String.format("%.2f", c), c);
        }
        p.plotLineHorizontal("0", 0);
        p.plotLineHorizontal("0.5", 0.5);
        p.plotLineHorizontal("1.0", 1);
        p.plotLineVerticalRows();
        p.plotLineVertical("!?", 0.19);
        p.plotYAxisLabel("Hello");
        p.plotXAxisLabel("World");
        p.plotTitleLabel("Bar-Chart");
    }

    void plot2(Graphics2D g, int w, int h, Consumer<PlotterPoints> init) {
        Random r = new Random(456);
        PlotterPoints p = new PlotterPoints(g, w, h);
        init.accept(p);
        int rows = 10;

        p.splitArea(rows, 3);
        p.plotAreaBackground();
        p.plotFrame();
        p.plotColumnLabels("item-A", "item-B", "item-C");
        for (int i = 0; i < rows; ++i) {
            double a = r.nextDouble();
            double b = r.nextDouble();
            double c = r.nextDouble();
            p.plot("", String.format("%.2f", a), a);
            p.plotLine();
            p.nextColumn();

            p.plot("", String.format("%.2f", b), b);
            p.plotLine();
            p.nextColumn();

            p.plot("", String.format("%.2f", c), c);
            p.plotLine();
            p.nextColumn();

            p.plotLineVerticalColumnCurrent("R" + i);
            p.nextRow();
        }
        p.plotYAxisLabel("Y");
        p.plotXAxisLabel("X");
        p.plotTitleLabel("Line-Chart");
    }

    void plot3(Graphics2D g, int w, int h, Consumer<PlotterPoints> init) {
        Random r = new Random(789);
        PlotterPoints p = new PlotterPoints(g, w, h);
        init.accept(p);
        int rows = 10;

        p.splitArea(rows, 3);
        p.plotAreaBackground();
        p.plotFrame();
        p.plotColumnLabels("item-A", "item-B", "item-C");
        for (int i = 0; i < rows; ++i) {
            double x = r.nextDouble();
            double y = r.nextDouble();
            p.plot("", x, y);
            p.plotLine();
        }

        p.nextColumn();
        for (int i = 0; i < rows; ++i) {
            double x = r.nextDouble();
            double y = r.nextDouble();
            p.plot("", x, y);
            p.plotLine();
        }
        p.nextColumn();
        for (int i = 0; i < rows; ++i) {
            double x = r.nextDouble();
            double y = r.nextDouble();
            p.plot(String.format("(%.1f,%.1f)", x, y), x, y);
        }

        for (int i = 0; i <= 5; ++i) {
            double v = i / 5.0;
            p.plotLineHorizontal(String.format("%.1f", v), v);
        }
        for (int i = 0; i <= 10; ++i) {
            double v = i / 10.0;
            p.plotLineVertical(String.format("%.1f", v), v);
        }
        p.plotTitleLabel("Scatter");
    }

    PlotterForMatrix plot4(int w, int h, Consumer<PlotterForMatrix> init) {
        PlotterForMatrix p = new PlotterForMatrix();
        p.setSize(w, h);
        init.accept(p);

        Random rand = new Random(1234);
        float factor = 1000;

        p.setData(new float[][] {
                {factor * rand.nextFloat(), factor * rand.nextFloat(), factor * rand.nextFloat()},
                {factor * rand.nextFloat(), factor * rand.nextFloat(), factor * rand.nextFloat()},
                {factor * rand.nextFloat(), factor * rand.nextFloat(), factor * rand.nextFloat()},
                {factor * rand.nextFloat(), factor * rand.nextFloat(), factor * rand.nextFloat()},
        }, Arrays.asList("c1", "c2", "c3"), Arrays.asList("r1", "r2", "r3", "r4"));

        p.setLogarithm(false);
        p.setTitle("float[][] with headers");
        p.setXAxisLabel("hello");
        p.setYAxisLabel("world");
        return p;
    }

    PlotterForMatrix plot5(int w, int h, Consumer<PlotterForMatrix> init) {
        PlotterForMatrix p = new PlotterForMatrix();
        p.setSize(w, h);
        init.accept(p);

        Random rand = new Random(5678);
        float factor = 1000;

        p.setData(new float[][] {
                {factor * rand.nextFloat(), factor * rand.nextFloat(), factor * rand.nextFloat()},
                {factor * rand.nextFloat(), factor * rand.nextFloat(), factor * rand.nextFloat()},
                {factor * rand.nextFloat(), factor * rand.nextFloat(), factor * rand.nextFloat()},
        },
                (table,i) -> "c" + i,
                (row,i) -> "r" + i);
        p.setValueLabel(true);
        p.setXGridLine(true);
        p.setLogarithm(true);
        p.setTitle("float[][] with log-scale headers");
        return p;
    }

    PlotterForMatrix plot6(int w, int h, Consumer<PlotterForMatrix> init) {
        PlotterForMatrix p = new PlotterForMatrix();
        p.setSize(w, h);
        init.accept(p);

        Random rand = new Random(9012);
        float factor = 1000;
        p.setMode(PlotterForMatrix.PlotMode.Line);
        p.setData(new double[][] {
                {factor * rand.nextFloat(), factor * rand.nextFloat(), -factor * rand.nextFloat()},
                {factor * rand.nextFloat(), -factor * rand.nextFloat(), factor * rand.nextFloat()},
                {-factor * rand.nextFloat(), factor * rand.nextFloat(), factor * rand.nextFloat()},
                {factor * rand.nextFloat(), factor * rand.nextFloat(), -factor * rand.nextFloat()},
                {factor * rand.nextFloat(), -factor * rand.nextFloat(), factor * rand.nextFloat()},
                {-factor * rand.nextFloat(), factor * rand.nextFloat(), factor * rand.nextFloat()},
                {factor * rand.nextFloat(), factor * rand.nextFloat(), -factor * rand.nextFloat()},
                {factor * rand.nextFloat(), -factor * rand.nextFloat(), factor * rand.nextFloat()},
                {-factor * rand.nextFloat(), factor * rand.nextFloat(), factor * rand.nextFloat()},
        },
            Arrays.asList("data1", "data2", "data3"),
            Arrays.asList("V1", "", "", "V4", "", "", "V7", "", "V9")); //a non-empty row-label of headerColumns will be grid-line with the label


        p.setXGridLine(true);
        p.setTitle("double[][] lines");
        return p;
    }

    PlotterForMatrix plot7(int w, int h, Consumer<PlotterForMatrix> init) {
        PlotterForMatrix p = new PlotterForMatrix();
        p.setSize(w, h);
        init.accept(p);

        Random rand = new Random(3456);
        float factorX = 1000;
        float factorY = 100;
        p.setMode(PlotterForMatrix.PlotMode.Scatter);
        p.setData(new double[][] {
                {    factorX  * rand.nextFloat(),  factorY * rand.nextFloat(), -factorX * rand.nextFloat(),  factorY * rand.nextFloat()},
                {    factorX  * rand.nextFloat(), -factorY * rand.nextFloat(),  factorX * rand.nextFloat(),  factorY * rand.nextFloat()},
                {   -factorX  * rand.nextFloat(),  factorY * rand.nextFloat(),  factorX * rand.nextFloat(), -factorY * rand.nextFloat()},
                {    factorX  * rand.nextFloat(),  factorY * rand.nextFloat(), -factorX * rand.nextFloat(),  factorY * rand.nextFloat()},
                {    factorX  * rand.nextFloat(), -factorY * rand.nextFloat(),  factorX * rand.nextFloat(),  factorY * rand.nextFloat()},
                {   -factorX  * rand.nextFloat(),  factorY * rand.nextFloat(),  factorX * rand.nextFloat(), -factorY * rand.nextFloat()},
                {    factorX  * rand.nextFloat(),  factorY * rand.nextFloat(), -factorX * rand.nextFloat(),  factorY * rand.nextFloat()},
                {    factorX  * rand.nextFloat(), -factorY * rand.nextFloat(),  factorX * rand.nextFloat(),  factorY * rand.nextFloat()},
                {   -factorX  * rand.nextFloat(),  factorY * rand.nextFloat(),  factorX * rand.nextFloat(), -factorY * rand.nextFloat()},
            },
            Arrays.asList("A", "_A", "B", "_B"), // 2n columns of headerRow are just ignored. n columns of headerRow will be displayed in legend-box
            Arrays.asList("V1", "V2", "V3", "V4", "V5", "V6", "V7", "V8", "V9")); //each row-label of headerColumns will be used as value-label

        double[] minMax = p.getMinMaxValuesFromScatterData();

        p.setValueLabel(true);
        p.setXGridLine(true);
        p.setTitle("double[][] scatter");
        p.setXAxisLabel(String.format("min=%f, max=%f", minMax[0], minMax[1]));
        p.setYAxisLabel(String.format("min=%f, max=%f", minMax[2], minMax[3]));

        return p;
    }

    PlotterForMatrix plot8(int w, int h, Consumer<PlotterForMatrix> init) {
        PlotterForMatrix p = new PlotterForMatrix();
        p.setSize(w, h);
        init.accept(p);

        Random rand = new Random(1111);
        float factor = 1000;
        p.setData(Arrays.asList(
                Arrays.asList(factor * rand.nextFloat(), factor * rand.nextFloat(), factor * rand.nextFloat()),
                Arrays.asList(factor * rand.nextFloat(), factor * rand.nextFloat(), factor * rand.nextFloat()),
                Arrays.asList(factor * rand.nextFloat(), factor * rand.nextFloat(), factor * rand.nextFloat())),
                (table, idx) -> "c" + idx,
                (row, idx) -> "r" + idx);

        p.setXGridLine(true);
        p.setTitle("List bar");
        return p;
    }
}
