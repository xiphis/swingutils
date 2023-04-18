package org.xiphis.swing.intern;

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
        return 0f;
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
        return 0f;
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

        Layout done() {
            width = SizeRequirements.getAlignedSizeRequirements(totX.toArray(new SizeRequirements[0]));
            height = SizeRequirements.getTiledSizeRequirements(totY.toArray(new SizeRequirements[0]));
            return this;
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

                    if (prevElement != currElement && !isChild(currElement, prevElement)) {
                        switch (currElement.tagName()) {
                            case "a":
                                break;
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
                            case "a":
                                break;
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
                                    //nextLine = true;
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

        return layout.done();
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
        Dimension min = minDimension(parent);
        Layout layout = computeLayoutSize(parent, foo(parent));
        Insets insets = parent.getInsets();
        return new Dimension(
                Math.max(min.width, layout.width.preferred + insets.left + insets.right),
                Math.max(min.height, layout.height.preferred + insets.top + insets.bottom));
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
        Dimension min = minDimension(parent);
        Layout layout = computeLayoutSize(parent, foo(parent));
        Insets insets = parent.getInsets();
        return new Dimension(
                Math.max(min.width, layout.width.minimum + insets.left + insets.right),
                Math.max(min.height, layout.height.minimum + insets.top + insets.bottom));
    }

    private Dimension minDimension(Container parent) {
        Dimension dim = new Dimension();
        if (parent instanceof HtmlPanel) {
            Element body = ((HtmlPanel) parent).body();
            if (body.hasAttr("width")) {
                dim.width = Integer.parseUnsignedInt(body.attr("width"));
            }
            if (body.hasAttr("height")) {
                dim.height = Integer.parseUnsignedInt(body.attr("height"));
            }
        }
        return dim;
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
        Insets insets = parent.getInsets();
        Dimension size = parent.getSize();
        size.height -= insets.top + insets.bottom;
        size.width -= insets.left + insets.right;

        Layout layout = computeLayoutSize(parent, size);

        int totYSize = layout.totY.size();
        int[] yOffsets = new int[totYSize];
        int[] ySpans = new int[totYSize];
        SizeRequirements.calculateTiledPositions(size.height, layout.height, layout.totY.toArray(new SizeRequirements[0]), yOffsets, ySpans);

        Iterator<Layout.Row> rIt = layout.rows.iterator();
        Iterator<SizeRequirements> totY = layout.totY.iterator();
        Iterator<SizeRequirements> totX = layout.totX.iterator();
        for (int r = 0; rIt.hasNext(); r++) {
            Layout.Row row = rIt.next();
            int compSize = row.comp.size();
            int[] compyOffsets = new int[compSize];
            int[] compySpans = new int[compSize];
            SizeRequirements.calculateAlignedPositions(ySpans[r], totY.next(), row.rowY.toArray(new SizeRequirements[0]), compyOffsets, compySpans);

            int[] compxOffsets = new int[compSize];
            int[] compxSpans = new int[compSize];
            calculateTiledPositions(size.width, totX.next(), row.rowX.toArray(new SizeRequirements[0]), compxOffsets, compxSpans);

            Iterator<Component> cIt = row.comp.iterator();
            for (int i = 0; cIt.hasNext(); i++) {
                Component comp = cIt.next();
                comp.setBounds(compxOffsets[i] + insets.left, compyOffsets[i] + yOffsets[r] + insets.top, compxSpans[i], compySpans[i]);
                comp.doLayout();
            }
        }
    }

    private void calculateTiledPositions(int allocated, SizeRequirements total, SizeRequirements[] children, int[] offsets, int[] spans) {
        int[] roffsets = new int[offsets.length];
        SizeRequirements.calculateTiledPositions(allocated, total, children, roffsets, spans, false);
        SizeRequirements.calculateTiledPositions(allocated, total, children, offsets, spans);

        // TODO: fix stupid solution with something that would work better later
        for (int i = 0; i < children.length; i++) {
            float alignment = children[i].alignment;
            int offset = (int) ((alignment * roffsets[i]) + ((1f - alignment) * offsets[i]));
            offsets[i] = offset;
        }
    }
}
