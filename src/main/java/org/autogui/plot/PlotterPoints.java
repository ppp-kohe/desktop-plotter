package org.autogui.plot;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/// the subclass of {@link Plotter} for line-charts or plotting points.
/// * A column is a series of x-y points; {@link #plot(String, double, double)} puts a point at x,y
/// * {@link #plot(String, String, double)} put a point y at equal intervals in the horizontal direction.
/// * After one of plot call, {@link #nextColumn()} moves to the next point. Also, {@link #nextRow()} switches the series of data.
/// * Each plot call can insert {@link #plotLine()} and it connects those points by lines.
public class PlotterPoints extends Plotter {
    protected List<Shape> columnShapes;

    {
        plotAreaRowWidth = plotAreaColumnWidth;
        plotAreaRowMarginX = 0;
        targetMoving = PlotTargetMoving.NextRowOfSameColumn;
    }

    protected double[][] plotPrevious; //{col0:{x,y}, col1:{x,y}, ...}
    protected double[][] plotCurrent;

    public interface PlotIndexShape {
        Shape indexShape(int i, int maxColumns);
    }

    protected PlotIndexShape plotIndexShape = this::indexShape;
    protected boolean plotColumnLabelShapeLine = false;

    protected Stroke plotPointsLineStroke = new BasicStroke(1.5f);

    public PlotterPoints setPlotIndexShape(PlotIndexShape plotIndexShape) {
        this.plotIndexShape = plotIndexShape;
        return this;
    }

    public PlotterPoints setPlotColumnLabelShapeLine(boolean plotColumnLabelShapeLine) {
        this.plotColumnLabelShapeLine = plotColumnLabelShapeLine;
        return this;
    }

    public PlotterPoints setPlotIndexShapeForLine() {
        setPlotIndexShape(this::indexShapeDot);
        setPlotColumnLabelShapeLine(true);
        return this;
    }

    public static class PlotterFactoryPoints extends PlotterFactory {
        public Plotter createImage(int width, int height) {
            return new PlotterPoints(width, height);
        }

        public Plotter createImage(BufferedImage image, int width, int height) {
            return new PlotterPoints(image, width, height);
        }

        public Plotter create(Graphics2D g, double width, double height) {
            return new PlotterPoints(g, width, height);
        }

        public Plotter create(Graphics2D g) {
            return new PlotterPoints(g);
        }
    }

    protected static PlotterFactoryPoints factoryPoints = new PlotterFactoryPoints();

    public static PlotterFactoryPoints factory() {
        return factoryPoints;
    }

    public PlotterPoints(int width, int height) {
        super(width, height);
    }

    public PlotterPoints(BufferedImage image, int width, int height) {
        super(image, width, height);
    }

    public PlotterPoints(Graphics2D g, double width, double height) {
        super(g, width, height);
    }

    public PlotterPoints(Graphics2D g) {
        super(g);
    }

    public Shape indexShape(int n, int cols) {
        double size = Math.max(3, getWidth() * 0.01);
        return new Ellipse2D.Double(0, 0, size, size);
    }

    public Shape indexShapeDot(int n, int cols) {
        return new Ellipse2D.Double(0, 0, 1, 1);
    }

    public PlotterPoints setColumnShapes(List<Shape> columnShapes) {
        this.columnShapes = columnShapes;
        return this;
    }

    public PlotterPoints setPlotPointsLineStroke(Stroke plotPointsLineStroke) {
        this.plotPointsLineStroke = plotPointsLineStroke;
        return this;
    }

    @Override
    public void plotAreaBackground() {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        super.plotAreaBackground();
    }

    @Override
    public void splitArea(int rows, int cols) {
        super.splitArea(rows, cols);

        //reset the column to row size
        setPlotAreaColumnWidth(plotAreaRowWidth);
        setPlotAreaRowMarginX(0);

        plotPrevious = new double[cols][2];
        plotCurrent = new double[cols][2];

        setColumnShapes(IntStream.range(0, cols)
                .mapToObj(i -> plotIndexShape.indexShape(i, cols))
                .collect(Collectors.toList()));
    }

    @Override
    protected Shape plotColumnLabelShape(int col, double legendWidth, double legendHeight) {
        if (plotColumnLabelShapeLine) {
            return new Line2D.Double(0, 0, legendWidth, 0);
        } else if (columnShapes!= null && col < columnShapes.size()) {
            return columnShapes.get(col % columnShapes.size());
        } else {
            return super.plotColumnLabelShape(col, legendWidth, legendHeight);
        }
    }

    @Override
    protected void plotPointDrawShape(Shape shape) {
        graphics.draw(shape);
    }

    @Override
    public void plot(String xAxisLabel, String valueLabel, double p) {
        if (targetNotMovedYet) {
            plotTargetMove();
        }
        double colStartX = plotAreaLeftX + currentRow * plotAreaRowWidth;
        double colHeight = plotAreaHeight * p;
        double colStartY = height - (plotAreaBottomY + colHeight);

        double plotCurrentX = colStartX + plotAreaColumnWidth / 2;

        plotXY(xAxisLabel, valueLabel, colStartX, plotCurrentX, colStartY);
    }

    public void plot(String valueLabel, double px, double py) {
        double colWidth = plotAreaWidth * px;
        double colHeight = plotAreaHeight * py;
        double colStartX = plotAreaLeftX + colWidth;
        double colStartY = height - (plotAreaBottomY + colHeight);

        plotXY("", valueLabel, colStartX, colStartX, colStartY);
    }

    protected void plotXY(String xAxisLabel, String valueLabel, double colStartX, double plotCurrentX, double plotCurrentY) {
        if (columnPaints != null && currentColumn < columnPaints.size()) {
            graphics.setPaint(columnPaints.get(currentColumn));
        }
        graphics.setStroke(plotPointsLineStroke);
        Shape shape;
        if (columnShapes != null && currentColumn < columnShapes.size()) {
            shape = columnShapes.get(currentColumn);
        } else {
            shape = indexShape(currentColumn, columns);
        }
        if (hasLineData(currentColumn)) {
            plotPrevious[currentColumn][0] = plotCurrent[currentColumn][0];
            plotPrevious[currentColumn][1] = plotCurrent[currentColumn][1];
            plotCurrent[currentColumn][0] = plotCurrentX;
            plotCurrent[currentColumn][1] = plotCurrentY;
        }

        Rectangle bounds = shape.getBounds();
        double px = plotCurrentX - bounds.getWidth() / 2;
        double py = plotCurrentY - bounds.getWidth() / 2;
        graphics.translate(px, py);
        plotPointDrawShape(shape);
        graphics.translate(-px, -py);

        plotColumnLabel(xAxisLabel, valueLabel, colStartX, plotCurrentY);
        targetNotMovedYet = true;
    }

    private boolean hasLineData(int column) {
        return (plotPrevious != null && plotCurrent != null
                && column < plotPrevious.length && column < plotCurrent.length);
    }

    public void plotLine() {
        int column = currentColumn;
        if (column >= 0 && hasLineData(column) &&
                plotPrevious[column][0] != 0 &&
                plotCurrent[column][0] != 0) { //zero means invalid data
            Line2D line = new Line2D.Double(
                                plotPrevious[column][0],
                                plotPrevious[column][1],
                                plotCurrent[column][0],
                                plotCurrent[column][1]);
            graphics.setStroke(plotPointsLineStroke);
            if (columnPaints != null && column < columnPaints.size()) {
                graphics.setPaint(columnPaints.get(column));
            }
            graphics.draw(line);
        }
    }

    @Override
    public void plotLineVerticalColumnCurrent(String label) {
        double rowBase = 1.0 / rows;
        double colBase = rowBase / 2;
        plotLineVertical(label, rowBase * currentRow + colBase);
    }


    //---------- getters


    public List<Shape> getColumnShapes() {
        return columnShapes;
    }

    public PlotIndexShape getPlotIndexShape() {
        return plotIndexShape;
    }

    public boolean isPlotColumnLabelShapeLine() {
        return plotColumnLabelShapeLine;
    }

    public Stroke getPlotPointsLineStroke() {
        return plotPointsLineStroke;
    }

    //----------- inherited setters
    public PlotterPoints setWidth(double width) {
        super.setWidth(width);
        return this;
    }
    public PlotterPoints setHeight(double height) {
        super.setHeight(height);
        return this;
    }
    public PlotterPoints setSize(double w, double h) {
        super.setSize(w, h);
        return this;
    }
    public PlotterPoints setPlotAreaLeftXRatio(double r) {
        super.setPlotAreaLeftXRatio(r);
        return this;
    }
    public PlotterPoints setPlotAreaRightXRatio(double r) {
        super.setPlotAreaRightXRatio(r);
        return this;
    }
    public PlotterPoints setPlotAreaBaseXRatio(double r) {
        super.setPlotAreaBaseXRatio(r);
        return this;
    }
    public PlotterPoints setPlotAreaTopYRatio(double r) {
        super.setPlotAreaTopYRatio(r);
        return this;
    }
    public PlotterPoints setPlotAreaBottomYRatio(double r) {
        super.setPlotAreaBottomYRatio(r);
        return this;
    }
    public PlotterPoints setPlotAreaBaseYRatio(double r) {
        super.setPlotAreaBaseYRatio(r);
        return this;
    }
    public PlotterPoints setPlotAreaBackground(Paint paint) {
        super.setPlotAreaBackground(paint);
        return this;
    }
    public PlotterPoints setPlotLinePaint(Paint paint) {
        super.setPlotLinePaint(paint);
        return this;
    }
    public PlotterPoints setPlotIndexColor(Plotter.PlotIndexColor c) {
        super.setPlotIndexColor(c);
        return this;
    }
    public PlotterPoints setPlotStroke(Stroke s) {
        super.setPlotStroke(s);
        return this;
    }
    public PlotterPoints setPlotLineStroke(Stroke s) {
        super.setPlotLineStroke(s);
        return this;
    }
    public PlotterPoints setTargetMoving(Plotter.PlotTargetMoving m) {
        super.setTargetMoving(m);
        return this;
    }
    public PlotterPoints setPlotFont(Font font) {
        super.setPlotFont(font);
        return this;
    }

}
