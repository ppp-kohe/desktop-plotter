package org.autogui.plot;

import java.awt.*;

public interface PlotterState<SelfType extends PlotterState<SelfType>> {
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
