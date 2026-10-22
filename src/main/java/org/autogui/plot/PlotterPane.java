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
import java.util.List;
import java.util.Objects;

///a Swing component for {@link PlotterForMatrix};
///  it observes property changes of teh plotter and updates painting by those changes.
public class PlotterPane extends JPanel implements PlotterForMatrix.PlotterForMatrixObserver {
    protected PlotterForMatrix plotter;
    protected JPopupMenu menu;
    protected boolean usingPaneColor;

    public PlotterPane() {
        PlotterForMatrix plotter = new PlotterForMatrix();
        setPlotter(plotter);
        setPreferredSize(new Dimension((int) plotter.getWidth(), (int) plotter.getHeight()));
        initMenu();
    }

    public PlotterPane(int width, int height) {
        PlotterForMatrix plotter = new PlotterForMatrix();
        plotter.setSize(width, height);
        setPlotter(plotter);
        setPreferredSize(new Dimension((int) plotter.getWidth(), (int) plotter.getHeight()));
        initMenu();
    }

    public PlotterPane(PlotterForMatrix plotter) {
        setPlotter(plotter);
        setPreferredSize(new Dimension((int) plotter.getWidth(), (int) plotter.getHeight()));
        initMenu();
    }

    protected void initMenu() {
        menu = new JPopupMenu();
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    menu.show(PlotterPane.this, e.getX(), e.getY());
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    menu.show(PlotterPane.this, e.getX(), e.getY());
                }
            }
        });
        menu.add(new PlotImageSaveAction(this));
        menu.add(new PlotPrintAction(this));
    }

    public void setUsingPaneColor(boolean usingPaneColor) {
        this.usingPaneColor = usingPaneColor;
    }

    public boolean isUsingPaneColor() {
        return usingPaneColor;
    }

    public void setPlotter(PlotterForMatrix plotter) {
        boolean eq = Objects.equals(this.plotter, plotter);
        PlotterForMatrix oldPlotter = this.plotter;
        this.plotter = plotter;
        if (!eq) {
            if (oldPlotter != null) {
                oldPlotter.removeObserver(this);
            }
            if (plotter != null) {
                plotter.addObserver(this);
            }
        }
    }

    public PlotterForMatrix getPlotter() {
        return plotter;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Color foreground = getForeground();
        Color background = getBackground();
        if (usingPaneColor && foreground != null && background != null) {
            plotter.setPlotAreaBackground(background);
            plotter.setPlotLinePaint(foreground);
        }
        plotter.setSize(getWidth(), getHeight());
        plotter.plot((Graphics2D) g);
    }

    static List<String> ignoredEvents = Arrays.asList("width", "height", "initializer.setWidth", "initializer.setHeight");

    @Override
    public void plotterForMatrixChanged(PlotterForMatrix p, String name) {
        if (!ignoredEvents.contains(name)) {
            repaint();
        }
    }

    public static class PlotPrintAction extends AbstractAction {
        PlotterPane pane;
        public PlotPrintAction(PlotterPane pane) {
            this.pane = pane;
            putValue(NAME, "Print");
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            pane.getPlotter().print();
        }
    }

    public static class PlotImageSaveAction extends AbstractAction {
        PlotterPane pane;
        JFileChooser chooser;

        public PlotImageSaveAction(PlotterPane pane) {
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
                    int confirmRes = JOptionPane.showConfirmDialog(pane,
                            "File " + file.getName() + " already exists. Do you want to overwrite it?",
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
            pane.getPlotter().plot(g);
            g.dispose();
            try {
                ImageIO.write(img, "png", file);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
