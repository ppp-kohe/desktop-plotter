package org.autogui.plot;

import java.awt.*;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

///printing utility of plotters implementing {@link PlotterPrintable}
///```java
///  PlotterPrint print = new PlotterPrint();
///  PlotterForMatrix p = ...;
///  print.print(p);
/// ```
public class PlotterPrint {
    protected static AtomicLong count = new AtomicLong();
    /*
    protected boolean showPageDialog = true;

    public void setShowPageDialog(boolean showPageDialog) {
        this.showPageDialog = showPageDialog;
    }

    public boolean isShowPageDialog() {
        return showPageDialog;
    }*/

    public void print(PlotterPrintable plot) {
        try {
            PrinterJob job = PrinterJob.getPrinterJob();

            Book book = new Book();
            PageFormat format = new PageFormat();

            format = job.pageDialog(format);

            book.append((g, f, i) -> {
                if (i == 0) {
                    Graphics2D g2 = (Graphics2D) g;
                    double w = plot.getWidth();
                    double h = plot.getHeight();
                    runWithTransform(g2, w, h, f,
                            () -> plot.print((Graphics2D) g));
                    return Printable.PAGE_EXISTS;
                } else {
                    return Printable.NO_SUCH_PAGE;
                }
            }, format);

            job.setPageable(book);

            job.setJobName(String.format("%s-%d-plot",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")),
                    count.incrementAndGet()));

            if (job.printDialog()) {
                job.print();
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void runWithTransform(Graphics2D g, double w, double h, PageFormat format, Runnable process) {
        double ix = format.getImageableX();
        double iy = format.getImageableY();
        double iw = format.getImageableWidth();
        double ih = format.getImageableHeight();
        double pw = iw / w;
        double ph = ih / h;
        double p = Math.min(pw, ph);
        g.translate(ix, iy);
        g.scale(p, p);
        process.run();
        g.scale(1 / p, 1 / p);
        g.translate(-ix, -iy);
    }

    public interface PlotterPrintable {
        void print(Graphics2D g);
        double getWidth();
        double getHeight();
    }
}
