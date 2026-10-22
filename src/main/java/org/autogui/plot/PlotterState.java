package org.autogui.plot;

import java.awt.*;

/// setters for plotter.
/// @param <SelfType> the "this" type. all methods return "this"
public interface PlotterState<SelfType extends PlotterState<SelfType>> {
    /// calls {@link #setPlotAreaBackground(Paint)} and {@link #setPlotLinePaint(Paint)} with dark and light colors for dark-theme
    /// @return this
    @SuppressWarnings("unchecked")
    default SelfType setPlotPaintForDark() {
        setPlotAreaBackground(new Color(30, 30, 30));
        setPlotLinePaint(new Color(200, 200, 200));
        return (SelfType) this;
    }

    /// calls {@link #setPlotAreaBackground(Paint)} and {@link #setPlotLinePaint(Paint)} with light and dark colors for light-theme
    /// @return this
    @SuppressWarnings("unchecked")
    default SelfType setPlotPaintForLight() {
        setPlotAreaBackground(new Color(230, 230, 230));
        setPlotLinePaint(new Color(0.45f, 0.45f, 0.45f));
        return (SelfType) this;
    }

    /// calls {@link #setPlotAreaBackground(Paint)} and {@link #setPlotLinePaint(Paint)} with white and black colors for printing to white papers
    /// @return this
    @SuppressWarnings("unchecked")
    default SelfType setPlotPrintForPrint() {
        setPlotAreaBackground(Color.WHITE);
        setPlotLinePaint(Color.BLACK);
        return (SelfType) this;
    }

    SelfType setWidth(double width);
    SelfType setHeight(double height);
    SelfType setSize(double w, double h);

    SelfType setPlotAreaLeftXRatio(double r);
    SelfType setPlotAreaRightXRatio(double r);
    SelfType setPlotAreaBaseXRatio(double r);
    SelfType setPlotAreaTopYRatio(double r);
    SelfType setPlotAreaBottomYRatio(double r);
    SelfType setPlotAreaBaseYRatio(double r);
    SelfType setPlotAreaBackground(Paint paint);
    SelfType setPlotLinePaint(Paint paint);
    SelfType setPlotIndexColor(Plotter.PlotIndexColor c);
    SelfType setPlotStroke(Stroke s);
    SelfType setPlotLineStroke(Stroke s);
    SelfType setTargetMoving(Plotter.PlotTargetMoving m);
    SelfType setPlotFont(Font font);

    SelfType setPlotIndexShape(PlotterPoints.PlotIndexShape plotIndexShape);
}
