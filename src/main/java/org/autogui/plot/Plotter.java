package org.autogui.plot;

import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/// stateful class for plotting bars with Java 2D graphics context.
/// The class provides bar-chart and basic functionalities of plot rendering.
///
/// For most cases, {@link PlotterForMatrix} is convenient.
///
/// * It splits the graphics area into rows and columns;
///   a row contains columns.
///   In the bar-chart, columns are a group of bars.
/// * `set...` methods changes parameters and graphics primitives for rendering, like {@link Paint} ({@link Color} is a subtype)
///     and {@link Stroke}
/// * `plot...` methods actually do painting graphics
/// ```java
///     var plotter = new Plotter(1000, 1000);    //craetes BufferedImage w x h
///     plotter.plotAreaBackground();             //fills background
///     plotter.plotFrame();                      //draws background-frame rect
///     plotter.splitArea(3, 1);                  //sets the size into 3 rows and 1 columns
///
///     plotter.plot("d1", "100", 0.5);           //1st row
///     plotter.plot("d2", "140", 0.7);           //2nd row: the plot calls automatically change the row to the next one
///     plotter.plot("d3", "80", 0.4);            //3rd row
///
///     BufferedImage image = plotter.endImage();
/// ```
///
public class Plotter implements PlotterState<Plotter> {
    /** optional image object */
    protected BufferedImage image;
    protected Graphics2D graphics;

    public static double defaultWidth = 1600;
    public static double defaultHeight = 1200;

    protected double width = defaultWidth;
    protected double height = defaultHeight;

    protected int rows;
    protected int columns = 1;

    protected double plotAreaLeftXRatio = 0.1;
    protected double plotAreaRightXRatio = 0.1;

    protected double plotAreaTopYRatio = 0.15;
    protected double plotAreaBottomYRatio = 0.1;

    /**
     * plotAreaLeftX can be computed from {@link #width} * {@link #plotAreaLeftXRatio}
     */
    protected double plotAreaLeftX = 20;
    protected double plotAreaRightX = 20;

    /**
     *  <pre>
     *   ------------------ y=0
     *    | plotAreaTopY
     *   ------------------ height-plotAreaBottomY-plotAreaHeight = plotAreaTopY: p=1.0
     *    |
     *    | plotAreaHeight
     *    |
     *   ------------------ height-plotAreaBottomY: p=0
     *    | plotAreaBottomY
     *   ------------------ height
     * </pre>
     *  plotAreaTopY can be computed from {@link #height} * {@link #plotAreaTopYRatio}
     */
    protected double plotAreaTopY = 100;
    protected double plotAreaBottomY = 100;

    protected double plotAreaWidth = 500;
    protected double plotAreaHeight = 500;

    protected Paint plotAreaBackground = Color.white;
    protected Stroke plotStoke = new BasicStroke(1f);

    protected Paint plotLinePaint = new Color(0.45f, 0.45f, 0.45f);

    protected Stroke plotLineStroke = new BasicStroke(1f,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] {2f, 2f}, 2f);

    public interface PlotIndexColor {
        Color indexColor(int i, int maxColumns);
    }

    protected PlotIndexColor plotIndexColor = Plotter::indexColor;

    protected Font plotFont;

    /**
     * <pre>
     * | &lt;-col1-&gt; &lt;-col2-&gt; &lt;-col3-&gt; | &lt;-col1-&gt; &lt;-col2-&gt; &lt;-col3-&gt; |
     * | &lt;----------row1----------&gt; | &lt;----------row1----------&gt; |
     * </pre>
     */
    protected double plotAreaColumnWidth = 15;
    protected double plotAreaRowWidth = 60;
    protected double plotAreaRowMarginX = 5;

    protected int currentRow;
    protected int currentColumn;

    protected List<Paint> columnPaints;

    protected boolean targetNotMovedYet = false;

    protected PlotTargetMoving targetMoving = PlotTargetMoving.NextColumn;

    /**
     * strategy of automatic target moving by plot calls.
     *  an explicit call of {@link #nextColumn()}, {@link #nextRow()}, {@link #nextRowOfSameColumn(boolean)} or {@link #topRow()}
     *     suppresses the feature.
     */
    public enum PlotTargetMoving {
        NextColumn,
        NextRow,
        NextRowOfSameColumn,
        None,
    }

    public static class PlotterFactory {
        public Plotter createImage(int width, int height) {
            return new Plotter(width, height);
        }

        public Plotter createImage(BufferedImage image, int width, int height) {
            return new Plotter(image, width, height);
        }

        public Plotter create(Graphics2D g, double width, double height) {
            return new Plotter(g, width, height);
        }

        public Plotter create(Graphics2D g) {
            return new Plotter(g);
        }
    }

    protected static PlotterFactory factory = new PlotterFactory();

    public static PlotterFactory factory() {
        return factory;
    }

    /**
     * initialize ({@link #initArea()}) with a new image
     * @param width the width
     * @param height the height
     */
    public Plotter(int width, int height) {
        this.image = createImage(width, height);
        init(image.createGraphics(), width, height);
    }

    public static BufferedImage createImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    }

    /**
     * initialize ({@link #initArea()}) with an image
     * @param image the image
     * @param width the width
     * @param height the height
     */
    public Plotter(BufferedImage image, int width, int height) {
        this.image = image;
        init(image.createGraphics(), width, height);
    }

    public Graphics2D getGraphics() {
        return graphics;
    }

    /**
     * @return image created by {@link #Plotter(int, int)}
     */
    public BufferedImage endImage() {
        graphics.dispose();
        graphics = null;
        return image;
    }

    /**
     * initialize ({@link #init(Graphics2D, double, double)}) with a graphics
     * @param graphics the graphics
     * @param width the width
     * @param height the height
     */
    public Plotter(Graphics2D graphics, double width, double height) {
        init(graphics, width, height);
    }

    /**
     * initialize internal state, called from the constructor.
     *  calling {@link #initArea()}
     * @param g the graphics context
     * @param width the width
     * @param height the height
     */
    public void init(Graphics2D g, double width, double height) {
        this.graphics = g;
        setSize(width, height);
        initArea();
    }

    /**
     * no initialization of area
     * @param graphics the graphics
     */
    public Plotter(Graphics2D graphics) {
        this.graphics = graphics;
    }

    @Override public Plotter setWidth(double width) {
        this.width = width;
        return this;
    }

    @Override public Plotter setHeight(double height) {
        this.height = height;
        return this;
    }

    /**
     *  set the size by {@link #setWidth(double)} and {@link #setHeight(double)}
     * @param w the width
     * @param h the height
     */
    @Override public Plotter setSize(double w, double h) {
        setWidth(w);
        setHeight(h);
        return this;
    }

    @Override public Plotter setPlotAreaBackground(Paint plotAreaBackground) {
        this.plotAreaBackground = plotAreaBackground;
        return this;
    }

    @Override public Plotter setPlotLinePaint(Paint plotLinePaint) {
        this.plotLinePaint = plotLinePaint;
        return this;
    }

    @Override public Plotter setPlotAreaBaseXRatio(double plotAreaBaseXRatio) {
        setPlotAreaLeftXRatio(plotAreaBaseXRatio);
        setPlotAreaRightXRatio(plotAreaBaseXRatio);
        return this;
    }

    @Override public Plotter setPlotAreaLeftXRatio(double plotAreaLeftXRatio) {
        this.plotAreaLeftXRatio = plotAreaLeftXRatio;
        return this;
    }

    @Override public Plotter setPlotAreaRightXRatio(double plotAreaRightXRatio) {
        this.plotAreaRightXRatio = plotAreaRightXRatio;
        return this;
    }

    @Override public Plotter setPlotAreaBaseYRatio(double plotAreaBaseYRatio) {
        setPlotAreaTopYRatio(plotAreaBaseYRatio);
        setPlotAreaBottomYRatio(plotAreaBaseYRatio);
        return this;
    }

    @Override public Plotter setPlotAreaTopYRatio(double plotAreaTopYRatio) {
        this.plotAreaTopYRatio = plotAreaTopYRatio;
        return this;
    }

    @Override public Plotter setPlotAreaBottomYRatio(double plotAreaBottomYRatio) {
        this.plotAreaBottomYRatio = plotAreaBottomYRatio;
        return this;
    }

    @Override public Plotter setPlotIndexColor(PlotIndexColor plotIndexColor) {
        this.plotIndexColor = plotIndexColor;
        return this;
    }

    @Override public Plotter setPlotStroke(Stroke plotStoke) {
        this.plotStoke = plotStoke;
        return this;
    }

    @Override public Plotter setPlotLineStroke(Stroke plotLineStroke) {
        this.plotLineStroke = plotLineStroke;
        return this;
    }

    @Override public Plotter setPlotFont(Font plotFont) {
        this.plotFont = plotFont;
        return this;
    }

    @Override public Plotter setTargetMoving(PlotTargetMoving targetMoving) {
        this.targetMoving = targetMoving;
        return this;
    }

    @Override public Plotter setPlotIndexShape(PlotterPoints.PlotIndexShape plotIndexShape) {
        return this;
    }

    /**
     * setting up entire area,
     * called from {@link #init(Graphics2D, double, double)}
     */
    public void initArea() {
        setPlotAreaLeftX(Math.max(width * plotAreaLeftXRatio, 20));
        setPlotAreaRightX(Math.max(width * plotAreaRightXRatio, 20));

        setPlotAreaTopY(Math.max(height * plotAreaTopYRatio, 20));
        setPlotAreaBottomY(Math.max(height * plotAreaBottomYRatio, 20));

        setPlotAreaWidth(width - plotAreaLeftX - plotAreaRightX);
        setPlotAreaHeight(height - plotAreaTopY - plotAreaBottomY);
    }

    public void plotAreaBackground() {
        //for precise rendering, skip: g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        graphics.setStroke(plotStoke);
        graphics.setPaint(plotAreaBackground);
        graphics.fill(new Rectangle2D.Double(0.0, 0.0, width, height));
    }

    public void splitArea(int rows, int cols) {
        setRows(rows);
        setColumns(cols);
        setPlotAreaRowWidth(plotAreaWidth / rows);
        setPlotAreaRowMarginX(plotAreaRowWidth * 0.1);
        setPlotAreaColumnWidth((plotAreaRowWidth - plotAreaRowMarginX) / cols);
        setColumnPaints(IntStream.range(0, cols)
                .mapToObj(i -> plotIndexColor.indexColor(i, cols))
                .collect(Collectors.toList()));
    }

    public static Color indexColor(int n, int cols) {
        float start = 0.55f;
        float h = (1.0f / (float) cols * n) + start;
        if (h > 1.0f) {
            h %= 1.0f;
        }
        return Color.getHSBColor(h, 0.65f, 0.70f);
    }

    /**
     * set by {@link #splitArea(int, int)}
     * @param columnPaints paints for columns
     */
    public void setColumnPaints(List<Paint> columnPaints) {
        this.columnPaints = columnPaints;
    }

    public void plotColumnLabels(String... colNames) {
        plotColumnLabels(Arrays.asList(colNames));
    }

    /**
     * <pre>
     *   ----------------------------------------- y=0
     *                          | &lt;-center        plotAreaTopY*0.5
     *     (startX,startY)----------------------    ^
     *          |  [ ] l1  |  [ ] l2  |  [ ] l3 |   | plotAreaTopY*0.4
     *           -------------------------------    v
     *           &lt;---plotAreaWidth*0.7---------&gt;
     *   ----------------------------------------- plotAreaTopY
     *             plot-area
     * </pre>
     *
     * <pre>
     *                |       [legendBox ]            |label          | ...
     *     startX=colX        |height*0.5|   colX+colWidth/3         startX+ colWidth=width/columnNames.size()
     * </pre>
     */
    public void plotColumnLabels(List<String> columnNames) {
        double width = plotAreaWidth * 0.7;
        double height = Math.max(10, plotAreaTopY * 0.4);
        double startX = plotAreaLeftX + (plotAreaWidth - width) / 2.0;
        double startY = Math.max(5, plotAreaTopY / 2.0);
        Rectangle2D frame = new Rectangle2D.Double(
                startX,
                startY,
                width,
                height);

        int n = columnNames.size();
        double colWidth = width / n;

        double colX = startX;
        double colY = startY;

        double legendHeight = height * 0.5;
        double legendY = colY + (height - legendHeight) / 2.0;
        double legendWidth = Math.min(legendHeight, colWidth);
        double legendX = Math.max((colWidth / 3.0 - legendWidth) / 2.0, 0);

        if (plotFont != null) {
            graphics.setFont(plotFont);
        }
        graphics.setPaint(plotLinePaint);
        graphics.setStroke(plotStoke);
        graphics.draw(frame);

        for (int i = 0; i < n; ++i) {
            String name = columnNames.get(i);
            TextLayout layout = new TextLayout(name, graphics.getFont(), graphics.getFontRenderContext());
            if (columnPaints != null && !columnPaints.isEmpty()) {
                Paint p = columnPaints.get(i % columnPaints.size());
                graphics.setPaint(p);
            }
            graphics.translate(colX + legendX, legendY);
            {
                graphics.setStroke(plotStoke);

                Shape shape = plotColumnLabelShape(i, legendWidth, legendHeight);
                Rectangle bounds = shape.getBounds();
                double lx = (legendWidth - bounds.getWidth()) / 2;
                double ly = (legendHeight - bounds.getHeight()) / 2;
                graphics.translate(lx, ly);
                plotPointDrawShape(shape);
                graphics.translate(-lx, -ly);
            }
            graphics.translate(-(colX + legendX), -legendY);

            graphics.setPaint(plotLinePaint);
            layout.draw(graphics,
                    (float) (colX + colWidth / 3.0 /*- layout.getAdvance() / 2.0*/),
                    (float) (colY + height / 2.0 + layout.getBounds().getHeight() / 2.0));
            colX += colWidth;
        }
    }

    protected Shape plotColumnLabelShape(int col, double legendWidth, double legendHeight) {
        return new Rectangle2D.Double(0, 0, legendWidth, legendHeight);
    }

    protected void plotPointDrawShape(Shape shape) {
        graphics.fill(shape);
    }

    /**
     * plot bar-chart; each plot calls first runs {@link #plotTargetMove()}, if not yet
     * @param xAxisLabel label for x-axis
     * @param valueLabel label for y-axis
     * @param p the value (0..1)
     */
    public void plot(String xAxisLabel, String valueLabel, double p) {
        if (targetNotMovedYet) {
            plotTargetMove();
        }
        double rowStartX = plotAreaLeftX + currentRow * plotAreaRowWidth;
        double colStartX = rowStartX + plotAreaRowMarginX / 2.0 + currentColumn * plotAreaColumnWidth;
        double colWidth = Math.max(1, plotAreaColumnWidth * 0.8);
        double colHeight = plotAreaHeight * p;
        double colStartY = height - (plotAreaBottomY + colHeight);
        Rectangle2D bar = new Rectangle2D.Double(
                colStartX + plotAreaColumnWidth / 2.0 - colWidth / 2.0,
                colStartY,
                colWidth,
                colHeight);

        if (plotFont != null) {
            graphics.setFont(plotFont);
        }
        if (columnPaints != null && currentColumn < columnPaints.size()) {
            graphics.setPaint(columnPaints.get(currentColumn));
        }
        graphics.setStroke(plotStoke);
        graphics.fill(bar);

        plotColumnLabel(xAxisLabel, valueLabel, colStartX, colStartY);
        targetNotMovedYet = true;
    }

    protected void plotColumnLabel(String xAxisLabel, String valueLabel, double colStartX, double colStartY) {
        if (xAxisLabel != null && !xAxisLabel.isEmpty()) {
            TextLayout layout = new TextLayout(xAxisLabel, graphics.getFont(), graphics.getFontRenderContext());
            graphics.setPaint(plotLinePaint);
            layout.draw(graphics,
                    labelXInBox(colStartX, plotAreaColumnWidth, layout),
                    labelYBottom(layout));
        }

        if (valueLabel != null && !valueLabel.isEmpty()) {
            TextLayout layout = new TextLayout(valueLabel, graphics.getFont(), graphics.getFontRenderContext());
            graphics.setPaint(plotLinePaint);
            layout.draw(graphics,
                    labelXInBox(colStartX, plotAreaColumnWidth, layout),
                    (float) Math.max(plotAreaTopY + layout.getAscent() + 1,
                            colStartY - layout.getDescent() - 2)); //draw(leftX,baseLineY) //seems same as bounding-box height
        }
    }

    protected float labelXInBox(double startX, double boxWidth, TextLayout layout) {
        return (float) (startX + Math.max(0, boxWidth - layout.getAdvance()) / 2.0);
    }

    protected float labelYBottom(TextLayout layout) {
        return (float) (height - plotAreaBottomY + layout.getBounds().getHeight() + 5);
    }

    /**
     * draw horizontal line; the default settings is a dotted-marker line for Y values
     * @param label label for y-axis
     * @param p the line Y position (0..1)
     */
    public void plotLineHorizontal(String label, double p) {
        if (p < 0 || p > 1) {
            return;
        }
        double colHeight = plotAreaHeight * p;
        double lineY = height - (plotAreaBottomY + colHeight);
        double lineX = plotAreaLeftX;
        double lineWidth = plotAreaWidth;

        Line2D.Double line = new Line2D.Double(lineX, lineY, lineX + lineWidth, lineY);
        graphics.setStroke(plotLineStroke);
        graphics.setPaint(plotLinePaint);
        graphics.draw(line);

        if (label != null && !label.isEmpty()) {
            TextLayout layout = new TextLayout(label, graphics.getFont(), graphics.getFontRenderContext());
            double labelX = lineX - layout.getAdvance() - 5;
            double labelY = lineY + layout.getDescent();
            layout.draw(graphics, (float) labelX, (float) labelY);
        }
    }

    public void plotLineVerticalColumnCurrent(String label) {
        double rowBase = 1.0 / rows;
        double colBase = rowBase / columns;
        plotLineVertical(label, rowBase * currentRow + colBase * currentColumn);
    }

    public void plotLineVerticalRows() {
        double base = 1.0 / rows;
        for (int i = 0; i < (rows - 1); ++i) {
            plotLineVertical("", (i + 1) * base);
        }
    }

    /**
     * draw vertical line; the default settings is a dotted-marker line for X values
     * @param label label for x-axis
     * @param p the line X position (0..1)
     */
    public void plotLineVertical(String label, double p) {
        if (p < 0 || p > 1) {
            return;
        }
        double lineWidth = plotAreaWidth * p;
        double lineX = plotAreaLeftX + lineWidth;
        double lineY = plotAreaTopY;
        double lineHeight = plotAreaHeight;

        Line2D.Double line = new Line2D.Double(lineX, lineY, lineX, lineY + lineHeight);
        graphics.setStroke(plotLineStroke);
        graphics.setPaint(plotLinePaint);
        graphics.draw(line);

        if (label != null && !label.isEmpty()) {
            TextLayout layout = new TextLayout(label, graphics.getFont(), graphics.getFontRenderContext());
            double labelX = lineX - layout.getAdvance() / 2;
            layout.draw(graphics, (float) labelX, labelYBottom(layout));
        }
    }

    public void plotTitleLabel(String label) {
        TextLayout layout = new TextLayout(label, graphics.getFont(), graphics.getFontRenderContext());
        graphics.setStroke(plotStoke);
        graphics.setPaint(plotLinePaint);
        layout.draw(graphics,
                labelXInBox(plotAreaLeftX, plotAreaWidth, layout),
                (float) (layout.getBounds().getHeight() + 5));
    }

    public void plotXAxisLabel(String label) {
        TextLayout layout = new TextLayout(label, graphics.getFont(), graphics.getFontRenderContext());
        graphics.setStroke(plotStoke);
        graphics.setPaint(plotLinePaint);
        layout.draw(graphics,
                labelXInBox(plotAreaLeftX, plotAreaWidth, layout),
                (float) (height - layout.getDescent() - 5));
    }

    public void plotYAxisLabel(String label) {
        TextLayout layout = new TextLayout(label, graphics.getFont(), graphics.getFontRenderContext());
        graphics.setStroke(plotStoke);
        graphics.setPaint(plotLinePaint);

        double labelX = layout.getBounds().getHeight() + 5;
        double labelY = plotAreaTopY + plotAreaHeight / 2 + layout.getAdvance() / 2;
        double r = Math.toRadians(-90);
        graphics.translate(labelX, labelY);
        graphics.rotate(r);
        layout.draw(graphics, 0, 0);
        graphics.rotate(-r);
        graphics.translate(-labelX, -labelY);
    }

    public void plotFrame() {
        Rectangle2D b = new Rectangle2D.Double(plotAreaLeftX, plotAreaTopY,
                plotAreaWidth, plotAreaHeight);

        graphics.setStroke(plotStoke);
        graphics.setPaint(plotLinePaint);
        graphics.draw(b);
    }

    public void plotTargetMove() {
        switch (targetMoving) {
            case NextColumn:
                nextColumn(true);
                break;
            case NextRow:
                nextRow();
                break;
            case NextRowOfSameColumn:
                nextRowOfSameColumn(true);
                break;
            case None:
                targetNotMovedYet = false;
                break;
        }
    }

    public void nextColumn() {
        nextColumn(false);
    }

    public void nextColumn(boolean moveRow) {
        ++currentColumn;
        if (currentColumn >= columns) {
            currentColumn = 0;
            if (moveRow) {
                ++currentRow;
                if (currentRow >= rows) {
                    currentRow = 0;
                }
            }
        }
        targetNotMovedYet = false;
    }

    public void nextRow() {
        ++currentRow;
        if (currentRow >= rows) {
            currentRow = 0;
        }
        currentColumn = 0;
        targetNotMovedYet = false;
    }

    public void nextRowOfSameColumn(boolean moveCol) {
        ++currentRow;
        if (currentRow >= rows) {
            currentRow = 0;
            if (moveCol) {
                ++currentColumn;
                if (currentColumn >= columns) {
                    currentColumn = 0;
                }
            }
        }
        targetNotMovedYet = false;
    }

    public void topRow() {
        currentRow = 0;
        currentColumn = 0;
        targetNotMovedYet = false;
    }

    //-------------------------- setters and getters

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public int getRows() {
        return rows;
    }

    /**
     *  set by {@link #splitArea(int, int)}
     * @param rows the rows
     */
    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getColumns() {
        return columns;
    }

    /**
     * set by {@link #splitArea(int, int)}
     * @param columns the columns within a row
     */
    public void setColumns(int columns) {
        this.columns = columns;
    }

    public double getPlotAreaLeftXRatio() {
        return plotAreaLeftXRatio;
    }

    public double getPlotAreaRightXRatio() {
        return plotAreaRightXRatio;
    }

    public double getPlotAreaTopYRatio() {
        return plotAreaTopYRatio;
    }

    public double getPlotAreaBottomYRatio() {
        return plotAreaBottomYRatio;
    }

    public double getPlotAreaLeftX() {
        return plotAreaLeftX;
    }

    /**
     * set by {@link #initArea()}
     * @param plotAreaLeftX the left margin
     */
    public void setPlotAreaLeftX(double plotAreaLeftX) {
        this.plotAreaLeftX = plotAreaLeftX;
    }

    public double getPlotAreaRightX() {
        return plotAreaRightX;
    }

    /**
     * set by {@link #initArea()}
     * @param plotAreaRightX the right margin
     */
    public void setPlotAreaRightX(double plotAreaRightX) {
        this.plotAreaRightX = plotAreaRightX;
    }

    public double getPlotAreaTopY() {
        return plotAreaTopY;
    }

    /**
     * set by {@link #initArea()}
     * @param plotAreaTopY the top margin
     */
    public void setPlotAreaTopY(double plotAreaTopY) {
        this.plotAreaTopY = plotAreaTopY;
    }

    public double getPlotAreaBottomY() {
        return plotAreaBottomY;
    }

    /**
     * set by {@link #initArea()}
     * @param plotAreaBottomY the bottom margin
     */
    public void setPlotAreaBottomY(double plotAreaBottomY) {
        this.plotAreaBottomY = plotAreaBottomY;
    }

    public double getPlotAreaWidth() {
        return plotAreaWidth;
    }

    /**
     * set by {@link #initArea()}
     * @param plotAreaWidth the plot-area width
     */
    public void setPlotAreaWidth(double plotAreaWidth) {
        this.plotAreaWidth = plotAreaWidth;
    }

    public double getPlotAreaHeight() {
        return plotAreaHeight;
    }

    /**
     * set by {@link #initArea()}
     * @param plotAreaHeight the plot-area height
     */
    public void setPlotAreaHeight(double plotAreaHeight) {
        this.plotAreaHeight = plotAreaHeight;
    }

    public Paint getPlotAreaBackground() {
        return plotAreaBackground;
    }

    public Stroke getPlotStoke() {
        return plotStoke;
    }

    public Paint getPlotLinePaint() {
        return plotLinePaint;
    }

    public Stroke getPlotLineStroke() {
        return plotLineStroke;
    }

    public PlotIndexColor getPlotIndexColor() {
        return plotIndexColor;
    }

    public double getPlotAreaColumnWidth() {
        return plotAreaColumnWidth;
    }

    /**
     * set by {@link #splitArea(int, int)}
     * @param plotAreaColumnWidth the width of each data
     */
    public void setPlotAreaColumnWidth(double plotAreaColumnWidth) {
        this.plotAreaColumnWidth = plotAreaColumnWidth;
    }

    public double getPlotAreaRowWidth() {
        return plotAreaRowWidth;
    }

    /**
     * set by {@link #splitArea(int, int)}
     * @param plotAreaRowWidth the width of each group of data
     */
    public void setPlotAreaRowWidth(double plotAreaRowWidth) {
        this.plotAreaRowWidth = plotAreaRowWidth;
    }

    public double getPlotAreaRowMarginX() {
        return plotAreaRowMarginX;
    }

    /**
     * set by {@link #splitArea(int, int)}
     * @param plotAreaRowMarginX the x margin of group of data
     */
    public void setPlotAreaRowMarginX(double plotAreaRowMarginX) {
        this.plotAreaRowMarginX = plotAreaRowMarginX;
    }

    public int getCurrentRow() {
        return currentRow;
    }

    public void setCurrentRow(int currentRow) {
        this.currentRow = currentRow;
    }

    public int getCurrentColumn() {
        return currentColumn;
    }

    public void setCurrentColumn(int currentColumn) {
        this.currentColumn = currentColumn;
    }

    public List<Paint> getColumnPaints() {
        return columnPaints;
    }

    public Font getPlotFont() {
        return plotFont;
    }

    public PlotTargetMoving getTargetMoving() {
        return targetMoving;
    }
}
