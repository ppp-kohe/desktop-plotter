package org.autogui.plot;

import org.autogui.plot.data.PlotterLists;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

///
///```java
///     PlotterForMatrix p = new PlotterForMatrix();
///     p.setData(data, true, true); //data: [ ["head", "col1", "col2",...], ["headRow1", c1, c2, ...], ...]
///     p.setMaxValue(1000);
///     p.saveCsv(filePath);
///     p.savePlot(imagePath);
///
///     //or
///     p.plot((Graphics2D) g);
///```
///
public class PlotterForMatrix implements PlotterState<PlotterForMatrix>, PlotterPrint.PlotterPrintable {
    protected double width = Plotter.defaultWidth;
    protected double height = Plotter.defaultHeight;
    protected boolean topRowIsLabel;
    protected boolean topColumnIsLabel;
    protected List<List<Object>> data;

    protected double[] minMaxValues = {Double.NaN, Double.NaN, Double.NaN, Double.NaN};
    protected boolean logarithm = false;

    protected Plotter.PlotterFactory factory = PlotMode.Bar.factory();

    protected Map<String, Consumer<Plotter>> inits = new LinkedHashMap<>();

    protected String title;
    protected String xAxisLabel;
    protected String yAxisLabel;
    protected boolean valueLabel;
    protected boolean xGridLine;

    protected List<PlotterForMatrixObserver> observers = new ArrayList<>(1);

    protected PlotMode mode = PlotMode.Bar;

    public enum PlotMode {
        /**
         * bar-chart: each column of each row become a bar, grouped by the row
         */
        Bar(false, false, false),
        /**
         * point-plotting: each data in a row aligns with a same x-grid. rows construct x-grids at regular intervals.
         */
        Point(true, false, false),
        /**
         * line-chart: plotting points same as {@link #Point} with connecting lines of the points for each columns
         */
        Line(true, true, false),
        /**
         * scatter-plot: data becomes (x,y) columns (2n columns), and plot points by the x and y locations
         */
        Scatter(true, false, true),
        /**
         * scatter-plot with lines: plotting points same as {@link #Scatter} with connecting lines of the points for each columns
         */
        ScatterLine(true, true, true);

        protected final boolean scatter;
        protected final boolean points;
        protected final boolean line;

        PlotMode(boolean points, boolean line, boolean scatter) {
            this.scatter = scatter;
            this.points = points;
            this.line = line;
        }

        Plotter.PlotterFactory factory() {
            return points ? PlotterPoints.factory() : Plotter.factory();
        }

        public boolean isPoints() {
            return points;
        }

        public boolean isLine() {
            return line;
        }

        /**
         * if true,
         *   the data must be
         *   <pre>
         *       {r1c1x, r1c1y, r1c2x, r1c2y, ...},
         *       {r2c1x, r2c1y, r2c2x, r1c2y, ...},
         *       ...
         *   </pre>
         *    , and scatter points are
         *        (r1c1x, r1c1y), (r2c1x, r2c1y), ... and
         *        (r1c2x, r2c2y), (r2c2x, r2c2y), ...
         * @return scatter mode
         */
        public boolean isScatter() {
            return scatter;
        }
    }

    /**
     * set the mode of chart. The default value {@link PlotMode#Bar}.
     *  It also sets an appropriate factory for the mode by {@link #setFactory(Plotter.PlotterFactory)}
     * @param mode the mode
     * @return this
     */
    public PlotterForMatrix setMode(PlotMode mode) {
        boolean eq = Objects.equals(this.mode, mode);
        this.mode = mode;
        if (!eq) {
            observeChange("mode");
        }
        setFactory(mode.factory());
        return this;
    }

    /**
     *  set both width and height by {@link #setWidth(double)} and {@link #setHeight(double)}
     * @param width the width
     * @param height the height
     * @return this
     */
    public PlotterForMatrix setSize(double width, double height) {
        setWidth(width);
        setHeight(height);
        return this;
    }

    public PlotterForMatrix setWidth(double width) {
        boolean eq = Objects.equals(this.width, width);
        this.width = width;
        if (!eq) {
            observeChange("width");
        }
        addInitializer("setWidth", p -> p.setWidth(width));
        return this;
    }

    public PlotterForMatrix setHeight(double height) {
        boolean eq = Objects.equals(this.height, height);
        this.height = height;
        if (!eq) {
            observeChange("height");
        }
        addInitializer("setHeight", p -> p.setHeight(height));
        return this;
    }

    /**
     * set the plotting data; immediately constructing a matrix data by combining the data, the headerRow and the headerColumn.
     * @param data a matrix consisting of {@link Number} or {@link String} of double representation.
     *               { {row1_col1, row1_col2, ...}, {row2_col1, row2_col2, ...}, ...}
     * @param headerRow labels of rows, whose size must be the same size of columns, or null meaning no labels
     * @param headerColumn labels of columns (a series of values), whose size must be the same size of rows (data.size()),
     *                      or null meaning no labels
     * @return this
     */
    public <E,H> PlotterForMatrix setData(List<List<E>> data, List<H> headerRow, List<H> headerColumn) {
        setData(dataWithHeader(data, headerRow, headerColumn),
                headerRow != null, headerColumn != null);
        return this;
    }

    @SuppressWarnings("unchecked")
    public static <E,H> List<List<E>> dataWithHeader(List<List<E>> data, List<H> headerRow, List<H> headerColumn) {
        //reconstruct data
        int newRows = data.size() + (headerRow != null ? 1 : 0);
        List<List<E>> newData = new ArrayList<>(newRows);
        if (headerRow != null) {
            newData.add(List.class.cast(headerRow));
        }
        newData.addAll(data);

        E empty;
        try {
            empty = (E) "";
        } catch (ClassCastException e) { //by type erasure never happen
            empty = null;
        }

        if (headerColumn != null) {
            for (int i = 0; i < newRows; ++i) {
                ArrayList<E> newRow = new ArrayList<>(newData.get(i));
                if (i == 0 && headerRow != null) {
                    newRow.add(0, empty);
                } else {
                    newRow.add(0, (E) headerColumn.get(i + (headerRow == null ? 0 : -1)));
                }
                newData.set(i, newRow);
            }
        }
        return newData;
    }

    /**
     * set the plotting data
     * @param data a matrix consisting of {@link Number} or {@link String} of double representation;
     *              { {row1_col1, row1_col2, ...}, {row2_col1, row2_col2, ...}, ...}
     * @param topRowIsLabel if true, it regards the top of data  as the headerRow, and thus the row index of data shifts;
     *                         { {headerRow_col1, headerRow_col2, ...}, {row1_col1, row1_col2, ...}, ...}
     * @param topColumnIsLabel if true, it regards the top of each row as headerColumn, and thus the column index of data shifts;
     *                         { {row1_headerCol, row1_col1, row1_col2, ...}, {row2_headerCol, row2_col1, row2_col2, ...}, ...}.
     *                         <p>
     *                         Note; if both topRowIsLabel and topColumnIsLabel are true,
     *                         then both indices of row and column shift,
     *                         and the top element of the data (data.get(0).get(0)) will be ignored;
     *                           { {"", headerRow_col1, header_col2, ...}, {row1_headerCol, row1_col1, row1_col2, ...}, ...}
     *                         </p>
     * @return this
     */
    public <E> PlotterForMatrix setData(List<List<E>> data, boolean topRowIsLabel, boolean topColumnIsLabel) {
        setData(data);
        setTopColumnIsLabel(topColumnIsLabel);
        setTopRowIsLabel(topRowIsLabel);
        return this;
    }

    /**
     * set the plotting data
     * @param data a matrix consisting of {@link Number} or {@link String} of double representation;
     *                    { {row1_col1, row1_col2, ...}, {row2_col1, row2_col2, ...}, ...}.
     *             <p>
     *             Flags of {@link #isTopColumnIsLabel()} and {@link #isTopRowIsLabel()} determines
     *                whether the top row and/or column are labels or not. This will cause shift of indices.
     *             (Defaults are false.)
     *             </p>
     * @return this
     */
    @SuppressWarnings("all")
    public <E> PlotterForMatrix setData(List<List<E>> data) {
        boolean eq = (((Object)this.data) == (Object) data);
        this.data = List.class.cast(data);
        if (!eq) {
            observeChange("data");
        }
        return this;
    }

    public void observeChange(String name) {
        observers.forEach(o ->
                o.plotterForMatrixChanged(this, name));
    }

    public PlotterForMatrix addObserver(PlotterForMatrixObserver observer) {
        observers.add(observer);
        return this;
    }

    public PlotterForMatrix removeObserver(PlotterForMatrixObserver observer) {
        observers.remove(observer);
        return this;
    }

    public interface PlotterForMatrixObserver {
        void plotterForMatrixChanged(PlotterForMatrix p, String name);
    }

    public PlotterForMatrix setTopColumnIsLabel(boolean topColumnIsLabel) {
        boolean eq = Objects.equals(this.topColumnIsLabel, topColumnIsLabel);
        this.topColumnIsLabel = topColumnIsLabel;
        if (!eq) {
            observeChange("topColumnIsLabel");
        }
        return this;
    }

    public PlotterForMatrix setTopRowIsLabel(boolean topRowIsLabel) {
        boolean eq = Objects.equals(this.topRowIsLabel, topRowIsLabel);
        this.topRowIsLabel = topRowIsLabel;
        if (!eq) {
            observeChange("topRowIsLabel");
        }
        return this;
    }

    /**
     * set both minValue and maxValue by {@link #setMinValue(double)} and {@link #setMaxValue(double)}.
     *  The default values are NaN, meaning automatically obtaining the min or max value from data.
     * @param minValue max value of y for non-scatter plot, or max value of x for scatter plot
     * @param maxValue min value of y for non-scatter plot, or min value of x for scatter plot
     * @return this
     */
    public PlotterForMatrix setMinMaxValues(double minValue, double maxValue) {
        setMinValue(minValue);
        setMaxValue(maxValue);
        return this;
    }

    /**
     * @param maxValue max value of y for non-scatter plot, or max value of x for scatter plot
     * @return this
     */
    public PlotterForMatrix setMaxValue(double maxValue) {
        boolean eq = Objects.equals(this.minMaxValues[1], maxValue);
        minMaxValues[1] = maxValue;
        if (!eq) {
            observeChange("maxValue");
        }
        return this;
    }

    /**
     * @param minValue min value of y for non-scatter plot, or min value of x for scatter plot
     * @return this
     */
    public PlotterForMatrix setMinValue(double minValue) {
        boolean eq = Objects.equals(this.minMaxValues[0], minValue);
        minMaxValues[0] = minValue;
        if (!eq) {
            observeChange("minValue");
        }
        return this;
    }


    /**
     * set both minValue and maxValue for y of scatter plot by {@link #setMinValueScatterY(double)} and {@link #setMaxValueScatterY(double)} .
     *  The default values are NaN, meaning automatically obtaining the min or max value from data.
     * @param minValue the min value of y for scatter plot
     * @param maxValue the max value of y for scatter plot
     * @return this
     */
    public PlotterForMatrix setMinMaxValuesForScatterY(double minValue, double maxValue) {
        setMinValueScatterY(minValue);
        setMaxValueScatterY(maxValue);
        return this;
    }

    /**
     * @param maxValue max value of y for scatter plot
     * @return this
     */
    public PlotterForMatrix setMaxValueScatterY(double maxValue) {
        boolean eq = Objects.equals(this.minMaxValues[2], maxValue);
        minMaxValues[2] = maxValue;
        if (!eq) {
            observeChange("maxValueScatterY");
        }
        return this;
    }

    /**
     * @param minValue max value of y for scatter plot
     * @return this
     */
    public PlotterForMatrix setMinValueScatterY(double minValue) {
        boolean eq = Objects.equals(this.minMaxValues[3], minValue);
        minMaxValues[3] = minValue;
        if (!eq) {
            observeChange("minValueScatterY");
        }
        return this;
    }

    /**
     * @param logarithm if true, use log-scaling. The default value is false
     * @return this
     */
    public PlotterForMatrix setLogarithm(boolean logarithm) {
        boolean eq = Objects.equals(this.logarithm, logarithm);
        this.logarithm = logarithm;
        if (!eq) {
            observeChange("logarithm");
        }
        return this;
    }

    public PlotterForMatrix setTitle(String title) {
        boolean eq = Objects.equals(this.title, title);
        this.title = title;
        if (!eq) {
            observeChange("title");
        }
        return this;
    }

    public PlotterForMatrix setXAxisLabel(String xAxisLabel) {
        boolean eq = Objects.equals(this.xAxisLabel, xAxisLabel);
        this.xAxisLabel = xAxisLabel;
        if (!eq) {
            observeChange("xAxisLabel");
        }
        return this;
    }

    public PlotterForMatrix setYAxisLabel(String yAxisLabel) {
        boolean eq = Objects.equals(this.yAxisLabel, yAxisLabel);
        this.yAxisLabel = yAxisLabel;
        if (!eq) {
            observeChange("yAxisLabel");
        }
        return this;
    }

    public PlotterForMatrix setValueLabel(boolean valueLabel) {
        boolean eq = Objects.equals(this.valueLabel, valueLabel);
        this.valueLabel = valueLabel;
        if (!eq) {
            observeChange("valueLabel");
        }
        return this;
    }

    public PlotterForMatrix setXGridLine(boolean xGridLine) {
        boolean eq = Objects.equals(this.xGridLine, xGridLine);
        this.xGridLine = xGridLine;
        if (!eq) {
            observeChange("xGridLine");
        }
        return this;
    }


    public PlotterForMatrix setFactory(Plotter.PlotterFactory factory) {
        boolean eq = Objects.equals(this.factory, factory);
        this.factory = factory;
        if (!eq) {
            observeChange("factory");
        }
        return this;
    }

    //----- plotter properties


    @Override public PlotterForMatrix setPlotAreaLeftXRatio(double r) {
        addInitializer("setPlotAreaLeftXRatio", p -> p.setPlotAreaLeftXRatio(r));
        return this;
    }

    @Override public PlotterForMatrix setPlotAreaRightXRatio(double r) {
        addInitializer("setPlotAreaRightXRatio", p -> p.setPlotAreaRightXRatio(r));
        return this;
    }

    @Override public PlotterForMatrix setPlotAreaBaseXRatio(double r) {
        addInitializer("setPlotAreaBaseXRatio", p -> p.setPlotAreaBaseXRatio(r));
        return this;
    }

    @Override public PlotterForMatrix setPlotAreaTopYRatio(double r) {
        addInitializer("setPlotAreaTopYRatio", p -> p.setPlotAreaTopYRatio(r));
        return this;
    }

    @Override public PlotterForMatrix setPlotAreaBottomYRatio(double r) {
        addInitializer("setPlotAreaBottomYRatio", p -> p.setPlotAreaBottomYRatio(r));
        return this;
    }

    @Override public PlotterForMatrix setPlotAreaBaseYRatio(double r) {
        addInitializer("setPlotAreaBaseYRatio", p -> p.setPlotAreaBaseYRatio(r));
        return this;
    }

    @Override public PlotterForMatrix setPlotAreaBackground(Paint paint) {
        addInitializer("setPlotAreaBackground", p -> p.setPlotAreaBackground(paint));
        return this;
    }

    @Override public PlotterForMatrix setPlotLinePaint(Paint paint) {
        addInitializer("setPlotLinePaint", p -> p.setPlotLinePaint(paint));
        return this;
    }

    @Override public PlotterForMatrix setPlotIndexColor(Plotter.PlotIndexColor c) {
        addInitializer("setPlotIndexColor", p -> p.setPlotIndexColor(c));
        return this;
    }

    @Override public PlotterForMatrix setPlotStroke(Stroke s) {
        addInitializer("setPlotStroke", p -> p.setPlotStroke(s));
        return this;
    }

    @Override public PlotterForMatrix setPlotLineStroke(Stroke s) {
        addInitializer("setPlotLineStroke", p -> p.setPlotLineStroke(s));
        return this;
    }

    @Override public PlotterForMatrix setTargetMoving(Plotter.PlotTargetMoving m) {
        addInitializer("setTargetMoving", p -> p.setTargetMoving(m));
        return this;
    }

    @Override public PlotterForMatrix setPlotIndexShape(PlotterPoints.PlotIndexShape plotIndexShape) {
        addInitializer("setPlotIndexShape", p -> {
            if (p instanceof PlotterPoints pp) {
                pp.setPlotIndexShape(plotIndexShape);
            }
        });
        return this;
    }

    @Override public PlotterForMatrix setPlotFont(Font font) {
        addInitializer("setPlotFont", p -> p.setPlotFont(font));
        return this;
    }

    public PlotterForMatrix addInitializer(String name, Consumer<Plotter> init) {
        inits.put(name, init);
        observeChange("initializer." + name);
        return this;
    }

    //----------- main processes

    /**
     * write data to CSV file
     * @param path the saved file path
     * @return this
     */
    public PlotterForMatrix saveCsv(Path path) {
        try (Writer w = Files.newBufferedWriter(path)) {
            saveCsv(w);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public PlotterForMatrix saveCsv(Writer w) throws IOException {
        int[] size = getDataSize();
        for (int r = 0, n = size[0]; r < n; ++r) {
            var row = (data != null && r < data.size()) ?
                    data.get(r) : null;
            for (int c = 0, m = size[1]; c < m; ++c) {
                if (c > 0) {
                    w.write(',');
                }
                if (row != null && c < row.size()) {
                    var cell = toCsvCell(row.get(c));
                    if (c == m - 1 && cell.isEmpty()) {
                        w.write("\"\"");
                    } else {
                        w.write(cell);
                    }
                }
            }
            w.write("\r\n");
        }
        return this;
    }

    private final Pattern csvEscapingPattern = Pattern.compile("[\\n\\r,\"]");

    protected String toCsvCell(Object data) {
        if (data instanceof String str) {
            var m = csvEscapingPattern.matcher(str);
            if (m.find()) {
                return "\"" + m.replaceAll(r -> r.group().equals("\"") ? "\"\"" : r.group()) + "\"";
            } else {
                return str;
            }
        } else if (data instanceof Number) {
            return data.toString();
        } else if (data == null) {
            return "";
        } else {
            return toCsvCell(data.toString());
        }
    }

    /**
     * write plot image as a .png file
     * @param path the saved image file path
     * @return this
     */
    public PlotterForMatrix savePlot(Path path) {
        try {
            ImageIO.write(plot(), "png", path.toFile());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return this;
    }

    /**
     * plots and returns as an image
     * @return a created image.
     */
    public BufferedImage plot() {
        BufferedImage image = Plotter.createImage((int) width, (int) height);
        Plotter plotter = factory.create(image.createGraphics());

        plot(plotter);

        plotter.getGraphics().dispose();
        return image;
    }

    /**
     * shows page and print dialogs and prints the plot, by {@link PlotterPrint}
     * @return this
     */
    public PlotterForMatrix print() {
        new PlotterPrint().print(this);
        return this;
    }

    @Override
    public void print(Graphics2D g) {
        plot(g);
    }

    /**
     * plots data with a given context
     * @param g the graphics context
     * @return the created plotter object
     */
    public Plotter plot(Graphics2D g) {
        return plot(factory.create(g));
    }

    public Plotter plot(Plotter plotter) {
        plotInit(plotter);

        int[] size = getDataSize();
        int rows = size[0];
        int cols = size[1];
        List<String> columnLabels = createColumnLabels(rows, cols);
        double[] minMaxValues = mode.isScatter() ? getMinMaxValuesForScatterPlot() : getMinMaxValuesForPlot();

        plotArea(plotter, rows, cols, columnLabels);
        if (!mode.equals(PlotMode.Bar)) {
            plotGridLines(plotter, minMaxValues);
        }

        IntStream.range(topRowIsLabel ?  1 : 0, rows)
                .forEach(r -> plotRow(plotter, r, cols, columnLabels, minMaxValues));

        if (mode.equals(PlotMode.Bar)) { //bar-chart: write grid over bars
            plotGridLines(plotter, minMaxValues);
        }

        plotLabels(plotter);
        return plotter;
    }

    protected void plotInit(Plotter plotter) {
        if (mode.isLine() && plotter instanceof PlotterPoints) {
            ((PlotterPoints) plotter).setPlotIndexShapeForLine();
        }
        inits.values().forEach(c ->
                c.accept(plotter));
        plotter.initArea();
        plotter.plotAreaBackground();
    }

    protected void plotArea(Plotter plotter, int rows, int cols, List<String> columnLabels) {
        plotter.splitArea(rows + (topRowIsLabel ? -1 : 0), toAreaColumns(cols));
        plotter.plotFrame();

        if (columnLabels != null) {
            plotter.plotColumnLabels(columnLabels);
        }
    }

    protected void plotGridLines(Plotter plotter, double[] minMaxValues) {
        plotYGridLines(plotter,
                mode.isScatter() ? minMaxValues[2] : minMaxValues[0],
                mode.isScatter() ? minMaxValues[3] : minMaxValues[1]);
        if (xGridLine) {
            if (mode.isScatter()) {
                //log scaling
                plotXGridLinesForScatter(plotter, minMaxValues[0], minMaxValues[1]);
            } else if (!mode.isPoints()) { //for lines, plot by each plotRow
                plotter.plotLineVerticalRows();
            }
        }
    }

    protected void plotLabels(Plotter plotter) {
        if (title != null) {
            plotter.plotTitleLabel(title);
        }

        if (xAxisLabel != null) {
            plotter.plotXAxisLabel(xAxisLabel);
        }

        if (yAxisLabel != null) {
            plotter.plotYAxisLabel(yAxisLabel);
        }
    }

    private int toAreaColumns(int cols) {
        if (mode.isScatter()) {
            return (cols + (topColumnIsLabel ? -1 : 0)) / 2;
        } else {
            return cols + (topColumnIsLabel ? -1 : 0);
        }
    }

    //on scatter mode, return x of c
    private int toDataColumnIndex(int c) {
        return c * (mode.isScatter() ? 2 : 1) + (topColumnIsLabel ? 1 : 0);
    }

    public double[] getMinMaxValuesForPlot() {
        if (!Double.isNaN(minMaxValues[0]) && !Double.isNaN(minMaxValues[1])) { //both provided
            return minMaxValues;
        } else {
            double[] mm = getMinMaxValuesFromData();
            if (Double.isNaN(minMaxValues[1])) { //max
                if (Double.isNaN(minMaxValues[0])) {
                    return mm;
                } else {
                    return new double[]{minMaxValues[0], mm[1]};
                }
            } else {
                return new double[] {mm[0], minMaxValues[1]};
            }
        }
    }

    public double[] getMinMaxValuesForScatterPlot() {
        if (!Double.isNaN(minMaxValues[0]) && !Double.isNaN(minMaxValues[1]) &&
                !Double.isNaN(minMaxValues[2]) && !Double.isNaN(minMaxValues[3])) {
            return minMaxValues;
        } else {
            double[] mm = getMinMaxValuesFromScatterData();
            for (int i = 0; i < 4; ++i) {
                mm[i] = nonNan(minMaxValues[i], mm[i]);
            }
            return mm;
        }
    }

    private double nonNan(double v, double v2) {
        return Double.isNaN(v) ? v2 : v;
    }

    public double[] getMinMaxValuesFromData() {
        double[] mm = {Double.MAX_VALUE, Double.MIN_VALUE, 0, 0};
        if (data != null) {
            for (int r = (topRowIsLabel ? 1 : 0), rs = data.size(); r < rs; ++r) {
                List<?> cols = data.get(r);
                for (int c = (topColumnIsLabel ? 1 : 0), cs = cols.size(); c < cs; ++c) {
                    double v = toData(cols.get(c));
                    mm[0] = Math.min(v, mm[0]);
                    mm[1] = Math.max(v, mm[1]);
                }
            }
        }
        mm[0] = Math.min(0, mm[0]); //use 0
        if (mm[1] < mm[0]) {
            mm[1] = mm[0];
        }
        return mm;
    }

    public double[] getMinMaxValuesFromScatterData() {
        double[] mm = {Double.MAX_VALUE, Double.MIN_VALUE, Double.MAX_VALUE, Double.MIN_VALUE};
        for (int r = (topRowIsLabel ? 1 : 0), rs = data.size(); r < rs; ++r) {
            List<?> cols = data.get(r);
            boolean x = true;
            for (int c = (topColumnIsLabel ? 1 : 0), cs = cols.size(); c < cs; ++c) {
                double v = toData(cols.get(c));
                if (x) {
                    mm[0] = Math.min(v, mm[0]);
                    mm[1] = Math.max(v, mm[1]);
                } else {
                    mm[2] = Math.min(v, mm[2]);
                    mm[3] = Math.max(v, mm[3]);
                }
                x = !x;
            }
        }
        mm[0] = Math.min(0, mm[0]);
        if (mm[1] < mm[0]) {
            mm[1] = mm[0];
        }
        mm[2] = Math.min(0, mm[2]);
        if (mm[3] < mm[2]) {
            mm[3] = mm[2];
        }
        return mm;
    }

    public double toData(Object v) {
        try {
            if (v instanceof Number) {
                return ((Number) v).doubleValue();
            } else if (v instanceof String) {
                String s = (String) v;
                return Double.parseDouble(s);
            } else {
                return 0;
            }
        } catch (Exception ex) {
            return 0;
        }
    }

    /**
     * @return {rows, columns} computed from data; if data==null, then {0,0}
     */
    public int[] getDataSize() {
        if (data == null) {
            return new int[] {0, 0};
        }
        return new int[] {
                data.size(),
                data.stream()
                        .mapToInt(List::size)
                        .max()
                        .orElse(0)
        };
    }

    public List<String> createColumnLabels(int rows, int cols) {
        if (topRowIsLabel && rows >= 1) {
            List<?> headerRow = data.get(0);
            return IntStream.range(0, toAreaColumns(cols))
                    .mapToObj(c -> {
                        int ci = toDataColumnIndex(c);
                        if (ci < headerRow.size()) {
                            return String.format("%s", headerRow.get(ci));
                        } else {
                            return Integer.toString(c);
                        }
                    })
                    .collect(Collectors.toList());
        } else {
            /*
            return IntStream.range(topColumnIsLabel ? 1 : 0, cols)
                    .map(c -> c + (topColumnIsLabel ? -1 : 0))
                    .mapToObj(Integer::toString)
                    .collect(Collectors.toList());
             */
            return null;
        }
    }

    public void plotYGridLines(Plotter plotter, double minValue, double maxValue) {
        plotGridLinesForScatter(minValue, maxValue, plotter::plotLineHorizontal);
    }

    public void plotXGridLinesForScatter(Plotter plotter, double minValue, double maxValue) {
        plotGridLinesForScatter(minValue, maxValue, plotter::plotLineVertical);
    }

    public interface GridPlotter {
        void plot(String label, double p);
    }
    public void plotGridLinesForScatter(double minValue, double maxValue, GridPlotter plotter) {
        String valueFormat = getValueFormat(maxValue);

        double diff = maxValue - minValue;

        int lineStep = (int) Math.max(1, Math.log10(diff) + 1);

        double n = 1.0; //from top(1.0) to bottom(0.0)
        for (int i = 0; i <= lineStep; ++i) {
            double p; //lineY: approximately, height - plotAreaHeight * p
            if (logarithm) {
                p = getLogProportion(n);
            } else {
                p = n;
            }
            plotter.plot(String.format(valueFormat, maxValue * n + minValue), p);
            if (logarithm) {
                n /= 10.0;   //1.0, 0.1, 0.01, ...
            } else {
                n -= 1.0 / lineStep; //split by a fixed space
            }
        }
        if (logarithm) {
            plotter.plot(String.format(valueFormat, minValue), 0);
        }
    }

    public String getValueFormat(double maxValue) {
        if (maxValue >= 100) {
            return "%,.0f"; //omit fraction: X.NNN... -> X
        } else if (maxValue >= 10) {
            return "%,.1f";  //use 1 fraction number: X.NNN... -> X.N
        } else {
            return "%,.3f";  //use 3 fraction numbers: X.NNN... -> X.NNN
        }
    }

    public double getLogProportion(double p) {
        double base = 10.0 / 3.0;
        return Math.log10(p) / base + 1.0;
        //return (Math.log10(p + 0.1) + 1) / (Math.log10(1.1) + 1);
    }

    public void plotRow(Plotter plotter, int r, int cols, List<String> columnLabels, double[] minMaxValues) {
        String rowLabel = getRowLabel(r);
        List<?> row = data.get(r);
        IntStream.range(0, toAreaColumns(cols))
                .forEach(c -> plotRowColumn(plotter, row, c, rowLabel, columnLabels, minMaxValues));
        if (!mode.isScatter() && mode.isPoints() && xGridLine &&
                rowLabel != null && !rowLabel.isEmpty()) {
            plotter.plotLineVerticalColumnCurrent("");
        }
        plotter.nextRow();
    }

    public <E> void plotRowColumn(Plotter plotter, List<E> row, int c, String rowLabel, List<String> columnLabels, double[] minMaxValues) {
        int ci = toDataColumnIndex(c);
        //String colLabel = columnLabels.get(c); //columnLabels is not value labels
        Object v = row.get(ci);

        double value = toData(v);
        double p = toP(value, minMaxValues[0], minMaxValues[1]);

        if (mode.isScatter() && plotter instanceof PlotterPoints) {
            Object vy = row.get(ci + 1);
            double valueY = toData(vy);

            String vl = "";
            if (valueLabel) {
                vl = String.format("%s(" + getValueFormat(value) + "," + getValueFormat(valueY) + ")", rowLabel, value, valueY);
            }

            double py = toP(valueY, minMaxValues[2], minMaxValues[3]);
            ((PlotterPoints) plotter).plot(vl, p, py);
        } else {
            String vl = "";
            if (valueLabel) {
                vl = String.format(getValueFormat(value), value);
            }
            plotter.plot(c == 0 ? rowLabel : "", vl, p);
        }

        if (mode.isLine() && plotter instanceof PlotterPoints) {
            ((PlotterPoints) plotter).plotLine();
        }
        plotter.nextColumn();
    }

    private double toP(double value, double minValue, double maxValue) {
        double p = value;
        double diff = maxValue - minValue;
        if (diff != 0) {
            p = (p - minValue) / diff;
        }
        if (logarithm && p != 0) {
            p = getLogProportion(p);
        }
        return p;
    }

    public String getRowLabel(int r) {
        List<?> cols = data.get(r);
        if (topColumnIsLabel && cols.size() > 1) {
            return String.format("%s", cols.get(0));
        } else {
            //return Integer.toString(r + (topRowIsLabel ? -1 : 0));
            return null;
        }
    }

    //--- support of extra data types

    public <E,H> PlotterForMatrix setData(List<List<E>> data,
                        BiFunction<List<List<E>>, Integer, H> headerRowGen,
                        BiFunction<List<E>, Integer, H> headerColumnGen) {
        return setData(data,
                headerRowGen == null ? null : new PlotterLists.HeaderRowList<>(data, headerRowGen),
                headerColumnGen == null ? null : new PlotterLists.HeaderColumnList<>(data, headerColumnGen));
    }

    @SuppressWarnings("unchecked")
    public PlotterForMatrix setData(float[][] data) {
        return setData(new PlotterLists.FloatMatrixList(data));
    }

    @SuppressWarnings("unchecked")
    public <H> PlotterForMatrix setData(float[][] data, List<H> headerRow, List<H> headerColumn) {
        setData(new PlotterLists.FloatMatrixList(data, headerRow, headerColumn));
        setTopColumnIsLabel(headerColumn != null);
        setTopRowIsLabel(headerRow != null);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <H> PlotterForMatrix setData(float[][] data, BiFunction<float[][], Integer, H> headerRowGen, BiFunction<float[], Integer, H> headerColumnGen) {
        return setData(data,
                headerRowGen == null ? null : new PlotterLists.FloatHeaderRowList(data, headerRowGen),
                headerColumnGen == null ? null : new PlotterLists.FloatHeaderColumnList(data, headerColumnGen));
    }

    @SuppressWarnings("unchecked")
    public PlotterForMatrix setData(double[][] data) {
        return setData(new PlotterLists.DoubleMatrixList(data));
    }

    @SuppressWarnings("unchecked")
    public <H> PlotterForMatrix setData(double[][] data, List<H> headerRow, List<H> headerColumn) {
        setData(new PlotterLists.DoubleMatrixList(data, headerRow, headerColumn));
        setTopColumnIsLabel(headerColumn != null);
        setTopRowIsLabel(headerRow != null);
        return this;
    }


    public PlotterForMatrix setData(double[][] data, BiFunction<double[][], Integer, Object> headerRowGen, BiFunction<double[], Integer, Object> headerColumnGen) {
        return setData(data,
                headerRowGen == null ? null : new PlotterLists.DoubleHeaderRowList(data, headerRowGen),
                headerColumnGen == null ? null : new PlotterLists.DoubleHeaderColumnList(data, headerColumnGen));
    }


    public <E> PlotterForMatrix setDataOfSingleColumn(List<E> singleColumnRows) {
        return setData(toDataOfSingleColumn(singleColumnRows));
    }

    public static <E> List<List<E>> toDataOfSingleColumn(List<E> singleColumnRows) {
        return singleColumnRows.stream()
                .map(Collections::singletonList)
                .collect(Collectors.toList());
    }

    public PlotterForMatrix setDataOfSingleColumn(float[] singleColumnRows) {
        return setDataOfSingleColumn(new PlotterLists.FloatVectorList(singleColumnRows));
    }

    public PlotterForMatrix setDataOfSingleColumn(double[] singleColumnRows) {
        return setDataOfSingleColumn(new PlotterLists.DoubleVectorList(singleColumnRows));
    }

    public <E,H> PlotterForMatrix setDataOfSingleColumn(List<E> singleColumnRows, H headerRow, List<H> headerColumns) {
        return setData(toDataOfSingleColumn(singleColumnRows),
                headerRow == null ? null : Collections.singletonList(headerRow),
                headerColumns);
    }

    public <H> PlotterForMatrix setDataOfSingleColumn(float[] singleColumnRows, H headerRow, List<H> headerColumns) {
        return setData(toDataOfSingleColumnFloat(singleColumnRows),
                headerRow == null ? null : Collections.singletonList(headerRow),
                headerColumns);
    }

    public <H> PlotterForMatrix setDataOfSingleColumn(double[] singleColumnRows, H headerRow, List<H> headerColumns) {
        return setData(toDataOfSingleColumnDouble(singleColumnRows),
                headerRow == null ? null : Collections.singletonList(headerRow),
                headerColumns);
    }

    public <E,H> PlotterForMatrix setDataOfSingleColumn(List<E> singleColumnRows, Function<List<List<E>>,H> headerRowGen, BiFunction<E,Integer,H> headerColumnsGen) {
        return setData(toDataOfSingleColumn(singleColumnRows),
                headerRowGen == null ? null : (table,i) -> headerRowGen.apply(table),
                headerColumnsGen == null ? null : (row, i) -> headerColumnsGen.apply(row.get(0), i));
    }

    public <H> PlotterForMatrix setDataOfSingleColumn(float[] singleColumnRows, Function<float[][],H> headerRowGen, BiFunction<Float,Integer,H> headerColumnsGen) {
        return setData(toDataOfSingleColumnFloat(singleColumnRows),
                headerRowGen == null ? null : (table,i) -> headerRowGen.apply(table),
                headerColumnsGen == null ? null : (row, i) -> headerColumnsGen.apply(row[0], i));
    }

    public <H> PlotterForMatrix setDataOfSingleColumn(double[] singleColumnRows, Function<double[][],H> headerRowGen, BiFunction<Double,Integer,H> headerColumnsGen) {
        return setData(toDataOfSingleColumnDouble(singleColumnRows),
                headerRowGen == null ? null : (table,i) -> headerRowGen.apply(table),
                headerColumnsGen == null ? null : (row, i) -> headerColumnsGen.apply(row[0], i));
    }

    public float[][] toDataOfSingleColumnFloat(float[] singleColumnRows) {
        float[][] t = new float[singleColumnRows.length][1];
        for (int i = 0, l = t.length; i < l; ++i) {
            t[i][0] = singleColumnRows[i];
        }
        return t;
    }

    public double[][] toDataOfSingleColumnDouble(double[] singleColumnRows) {
        double[][] t = new double[singleColumnRows.length][1];
        for (int i = 0, l = t.length; i < l; ++i) {
            t[i][0] = singleColumnRows[i];
        }
        return t;
    }

    //--------------- getters

    @Override
    public double getWidth() {
        return width;
    }

    @Override
    public double getHeight() {
        return height;
    }

    public boolean isTopRowIsLabel() {
        return topRowIsLabel;
    }

    public boolean isTopColumnIsLabel() {
        return topColumnIsLabel;
    }

    public List<List<Object>> getData() {
        return data;
    }

    public double[] getMinMaxValues() {
        return minMaxValues;
    }

    public boolean isLogarithm() {
        return logarithm;
    }

    public Plotter.PlotterFactory getFactory() {
        return factory;
    }

    public String getTitle() {
        return title;
    }

    public String getxAxisLabel() {
        return xAxisLabel;
    }

    public String getyAxisLabel() {
        return yAxisLabel;
    }

    public boolean isValueLabel() {
        return valueLabel;
    }

    public boolean isxGridLine() {
        return xGridLine;
    }

    public PlotMode getMode() {
        return mode;
    }
}
