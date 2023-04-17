package org.xiphis.swing;

import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public final class HtmlLayout implements LayoutManager2 {

    private static final Logger LOG = LoggerFactory.getLogger(HtmlLayout.class);
    private static final Level DEBUG = Level.INFO;

    private final Map<Component, Node> nodeMap;

    public HtmlLayout() {
        nodeMap = new IdentityHashMap<>();
    }

    /**
     * Returns a string representation of this grid bag layout's values.
     * @return     a string representation of this grid bag layout.
     */
    public String toString() {
        return getClass().getName();
    }

    /**
     * Adds the specified component to the layout, using the specified
     * constraint object.
     *
     * @param comp        the component to be added
     * @param constraints where/how the component is added to the layout.
     */
    @Override
    public void addLayoutComponent(Component comp, Object constraints) {
        if (constraints instanceof Node) {
            nodeMap.put(comp, (Node) constraints);
        } else {
            throw new IllegalArgumentException("constraint should be of type Node");
        }
    }

    /**
     * Calculates the maximum size dimensions for the specified container,
     * given the components it contains.
     *
     * @param target the target container
     * @return the maximum size of the container
     * @see Component#getMaximumSize
     * @see LayoutManager
     */
    @Override
    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Returns the alignment along the x axis.  This specifies how
     * the component would like to be aligned relative to other
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     *
     * @param target the target container
     * @return the x-axis alignment preference
     */
    @Override
    public float getLayoutAlignmentX(Container target) {
        return 0.5f;
    }

    /**
     * Returns the alignment along the y axis.  This specifies how
     * the component would like to be aligned relative to other
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     *
     * @param target the target container
     * @return the y-axis alignment preference
     */
    @Override
    public float getLayoutAlignmentY(Container target) {
        return 0.5f;
    }

    /**
     * Invalidates the layout, indicating that if the layout manager
     * has cached information it should be discarded.
     *
     * @param target the target container
     */
    @Override
    public void invalidateLayout(Container target) {

    }

    /**
     * If the layout manager uses a per-component string,
     * adds the component {@code comp} to the layout,
     * associating it
     * with the string specified by {@code name}.
     *
     * @param name the string to be associated with the component
     * @param comp the component to be added
     */
    @Override
    public void addLayoutComponent(String name, Component comp) {

    }

    /**
     * Removes the specified component from the layout.
     *
     * @param comp the component to be removed
     */
    @Override
    public void removeLayoutComponent(Component comp) {
        nodeMap.remove(comp);
    }

    class Layout {
        LinkedList<SizeRequirements> totX = new LinkedList<>();
        LinkedList<SizeRequirements> totY = new LinkedList<>();
        LinkedList<Row> rows = new LinkedList<>();

        SizeRequirements width;
        SizeRequirements height;


        class Row {
            LinkedList<SizeRequirements> rowX = new LinkedList<>();
            LinkedList<SizeRequirements> rowY = new LinkedList<>();
            LinkedList<Component> comp = new LinkedList<>();
        }

        void add(Row row) {
            totX.add(SizeRequirements.getTiledSizeRequirements(row.rowX.toArray(new SizeRequirements[0])));
            totY.add(SizeRequirements.getAlignedSizeRequirements(row.rowY.toArray(new SizeRequirements[0])));
            rows.add(row);
        }

    }

    private Element elementOf(Node node) {
        for (;;) {
            if (node instanceof Element) {
                return (Element) node;
            }
            node = node.parentNode();
        }
    }

    private boolean isChild(Element parent, Node child) {
        while (child != null) {
            if (child == parent) {
                return true;
            }
            child = child.parentNode();
        }
        return false;
    }

    private Iterable<Element> unwind(Element prev, Node current) {
        return new Iterable<Element>() {
            @Override
            public Iterator<Element> iterator() {
                return new Iterator<>() {

                    Element e = prev;


                    @Override
                    public boolean hasNext() {
                        if (e == null || isChild(e, current)) {
                            return false;
                        }
                        return true;
                    }

                    @Override
                    public Element next() {
                        Element e = this.e;
                        this.e = e.parent();
                        return e;
                    }
                };
            }
        };
    }

    private Layout computeLayoutSize(Container parent, Dimension limit) {
        Layout layout = new Layout();

        Layout.Row row = layout.new Row();
        Element prevElement = new Element("html"), currElement;

        next: for (Iterator<Component> cIt = Arrays.asList(parent.getComponents()).iterator(); cIt.hasNext(); prevElement = currElement) {
            Component c = cIt.next();
            Node curr = nodeMap.get(c);
            currElement = elementOf(curr);
            for (;;) {
                boolean nextLine = false;
                if (c.isVisible()) {
                    Dimension min = c.getMinimumSize();
                    Dimension pref = c.getPreferredSize();
                    Dimension max = c.getMaximumSize();

                    row.rowX.add(new SizeRequirements(min.width, pref.width, max.width, c.getAlignmentX()));
                    row.rowY.add(new SizeRequirements(min.height, pref.height, max.height, c.getAlignmentY()));

                    if (prevElement != currElement) {
                        switch (currElement.tagName()) {
                            case "p":
                                if (!(curr instanceof Element) && curr.previousSibling() != null) {
                                    break;
                                }
                            case "hr":
                            case "h1":
                            case "h2":
                            case "h3":
                            case "h4":
                            case "h5":
                            case "h6":
                                nextLine = true;
                                break;
                            default:
                                if (currElement.tag().formatAsBlock()) {
                                    nextLine = true;
                                    break;
                                }
                        }
                    }
                } else {
                    row.rowX.add(new SizeRequirements(0, 0, 0, c.getAlignmentX()));
                    row.rowY.add(new SizeRequirements(0, 0, 0, c.getAlignmentY()));
                }

                if (!isChild(prevElement, curr)) {
                    check: for (Element t : unwind(prevElement, curr)) {
                        switch (t.tagName()) {
                            case "h1":
                            case "h2":
                            case "h3":
                            case "h4":
                            case "h5":
                            case "h6":
                                nextLine = true;
                                break check;
                            default:
                                if (t.tag().formatAsBlock()) {
                                    nextLine = true;
                                    break check;
                                }
                        }
                    }
                }

                SizeRequirements totalX = SizeRequirements.getTiledSizeRequirements(row.rowX.toArray(new SizeRequirements[0]));
                //SizeRequirements totalY = SizeRequirements.getAlignedSizeRequirements(rowy.toArray(new SizeRequirements[0]));

                if (!row.comp.isEmpty() && (nextLine || totalX.preferred >= limit.width)) {
                    row.rowX.removeLast();
                    row.rowY.removeLast();
                    layout.add(row);
                    row = layout.new Row();
                    continue;
                }

                row.comp.add(c);


                if (c.isVisible() && curr instanceof Element) {
                    switch (((Element) curr).tagName()) {
                        case "hr":
                        case "br":
                            layout.add(row);
                            row = layout.new Row();
                            break;
                        default:
                            break;
                    }
                }

                continue next;
            }
        }
        if (!row.comp.isEmpty()) {
            layout.add(row);
        }

        layout.width = SizeRequirements.getAlignedSizeRequirements(layout.totX.toArray(new SizeRequirements[0]));
        layout.height = SizeRequirements.getTiledSizeRequirements(layout.totY.toArray(new SizeRequirements[0]));

        return layout;
    }

    /**
     * Calculates the preferred size dimensions for the specified
     * container, given the components it contains.
     *
     * @param parent the container to be laid out
     * @return the preferred dimension for the container
     * @see #minimumLayoutSize
     */
    @Override
    public Dimension preferredLayoutSize(Container parent) {
        Layout layout = computeLayoutSize(parent, foo(parent));
        return new Dimension(layout.width.preferred, layout.height.preferred);
    }

    /**
     * Calculates the minimum size dimensions for the specified
     * container, given the components it contains.
     *
     * @param parent the component to be laid out
     * @return the minimum dimension for the container
     * @see #preferredLayoutSize
     */
    @Override
    public Dimension minimumLayoutSize(Container parent) {
        Layout layout = computeLayoutSize(parent, foo(parent));
        return new Dimension(layout.width.minimum, layout.height.minimum);
    }

    private Dimension foo(Container parent) {
        Dimension screenSize = parent.getToolkit().getScreenSize();
        return new Dimension(screenSize.width / 2, screenSize.height / 2);
    }

    /**
     * Lays out the specified container.
     *
     * @param parent the container to be laid out
     */
    @Override
    public void layoutContainer(Container parent) {
        Layout layout = computeLayoutSize(parent, parent.getSize());

        int[] yOffsets = new int[layout.totY.size()];
        int[] ySpans = new int[layout.totY.size()];
        SizeRequirements.calculateTiledPositions(parent.getHeight(), layout.height, layout.totY.toArray(new SizeRequirements[0]), yOffsets, ySpans);

        Iterator<Layout.Row> rIt = layout.rows.iterator();
        for (int r = 0; rIt.hasNext(); r++) {
            Layout.Row row = rIt.next();
            int[] compyOffsets = new int[row.comp.size()];
            int[] compySpans = new int[row.comp.size()];
            SizeRequirements.calculateAlignedPositions(ySpans[r], layout.totY.get(r), row.rowY.toArray(new SizeRequirements[0]), compyOffsets, compySpans);

            int[] compxOffsets = new int[row.comp.size()];
            int[] compxSpans = new int[row.comp.size()];
            SizeRequirements.calculateTiledPositions(parent.getWidth(), layout.totX.get(r), row.rowX.toArray(new SizeRequirements[0]), compxOffsets, compxSpans);

            Iterator<Component> cIt = row.comp.iterator();
            for (int i = 0; cIt.hasNext(); i++) {
                Component comp = cIt.next();
                comp.setBounds(compxOffsets[i], compyOffsets[i] + yOffsets[r], compxSpans[i], compySpans[i]);
                comp.doLayout();
            }
        }
    }
}
