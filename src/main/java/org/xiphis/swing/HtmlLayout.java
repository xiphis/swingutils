package org.xiphis.swing;

import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.awt.*;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map;

public class HtmlLayout implements LayoutManager2 {

    private Map<Component, Node> nodes = new IdentityHashMap<>();

    /**
     * Adds the specified component to the layout, using the specified
     * {@code constraints} object.  Note that constraints
     * are mutable and are, therefore, cloned when cached.
     *
     * @param      comp         the component to be added
     * @param      constraints  an object that determines how
     *                          the component is added to the layout
     * @exception IllegalArgumentException if {@code constraints}
     *            is not a {@code GridBagConstraint}
     */
    @Override
    public void addLayoutComponent(Component comp, Object constraints) {
        if (constraints instanceof Node) {
            nodes.put(comp, (Node) constraints);
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Returns the maximum dimensions for this layout given the components
     * in the specified target container.
     * @param target the container which needs to be laid out
     * @see Container
     * @see #minimumLayoutSize(Container)
     * @see #preferredLayoutSize(Container)
     * @return the maximum dimensions for this layout
     */
    public Dimension maximumLayoutSize(Container target) {
        return insets(layout(new Info(), target, Mode.MAXIMUM_SIZE).dimension, target);
    }


    /**
     * Returns the alignment along the x axis.  This specifies how
     * the component would like to be aligned relative to other
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     *
     * @return the value {@code 0.5f} to indicate centered
     */
    public float getLayoutAlignmentX(Container parent) {
        return 0.5f;
    }

    /**
     * Returns the alignment along the y axis.  This specifies how
     * the component would like to be aligned relative to other
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     *
     * @return the value {@code 0.5f} to indicate centered
     */
    public float getLayoutAlignmentY(Container parent) {
        return 0.5f;
    }


    @Override
    public void invalidateLayout(Container target) {
        for (Node node : nodes.values()) {
            invalidateLayout(node);
        }
    }

    private void invalidateLayout(Node node) {

    }

    /**
     * Has no effect, since this layout manager does not use a per-component string.
     */
    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    /**
     * Removes the specified component from this layout.
     * <p>
     * Most applications do not call this method directly.
     * @param    comp   the component to be removed.
     * @see      java.awt.Container#remove(java.awt.Component)
     * @see      java.awt.Container#removeAll()
     */
    @Override
    public void removeLayoutComponent(Component comp) {
        nodes.remove(comp);
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return insets(layout(new Info(), parent, Mode.PREFERRED_SIZE).dimension, parent);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return insets(layout(new Info(), parent, Mode.MINIMUM_SIZE).dimension, parent);
    }

    private Dimension insets(Dimension dim, Container parent) {
        Insets insets = parent.getInsets();
        dim.width = add(dim.width, insets.left + insets.right);
        dim.height = add(dim.height, insets.top + insets.bottom);
        return dim;
    }

    private static int add(int a, int b) {
        try {
            return Math.addExact(a, b);
        } catch (ArithmeticException ae) {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public void layoutContainer(Container parent) {
        Info info = layout(new Info(), parent, Mode.PREFERRED_SIZE);

        layout(info, parent, Mode.TARGET);

        for (Component comp : parent.getComponents()) {
            Rectangle rect = info.layout.get(comp);
            comp.setBounds(rect);
        }
    }

    private boolean isParent(Node node1, Node node2) {
        for (;;) {
            Node parent = node2.parent();
            if (parent == null) {
                return false;
            }
            if (node1 == parent) {
                return true;
            }
            node2 = parent;
        }
    }

    private Info layout(Info info, Container parent, Mode mode) {
        LinkedList<Component> row = new LinkedList<>();
        Dimension bound = mode.getBound(parent);
        int y = 0;
        int xp = 0;
        int hp = 0;
        Node prevNode = new Comment("");
        for (Component component : parent.getComponents()) {
            Node node = nodes.get(component);
            Dimension dim = mode.getSize(component);

            boolean newline = false;

            if (bound.width - dim.width > xp) {
                newline = true;
            }

            if (prevNode != node) {
                if (prevNode instanceof Element && !isParent(prevNode, node)) {
                    switch (((Element) prevNode).tagName()) {
                        case "br":
                        case "hr":
                        case "h1":
                        case "h2":
                        case "h3":
                        case "h4":
                        case "h5":
                        case "h6":
                        case "div":
                            newline = true;
                        default:
                            break;
                    }
                }
                if (node instanceof Element) {
                    switch (((Element) node).tagName()) {
                        case "hr":
                        case "h1":
                        case "h2":
                        case "h3":
                        case "h4":
                        case "h5":
                        case "h6":
                        case "div":
                            newline = true;
                        default:
                            break;
                    }

                    dim = mode.adjustSize((Element) node, component);
                }
            }

            if (newline) {
                y = layoutRow(info, row, y, hp, mode);
                hp = 0;
                xp = 0;
                row.clear();
            }

            row.add(component);
            if (xp < Integer.MAX_VALUE) {
                try {
                    xp = Math.addExact(xp, dim.width);
                } catch (ArithmeticException ae) {
                    xp = Integer.MAX_VALUE;
                }
            }
            hp = Math.max(hp, dim.height);
            prevNode = node;
        }
        if (!row.isEmpty()) {
            layoutRow(info, row, y, hp, mode);
        }
        return info;
    }

    private int layoutRow(Info info, LinkedList<Component> row, int y, int hp, Mode mode) {
        int x = 0;
        for (Component comp : row) {
            Dimension d = mode.getSize(comp);
            info.layout.put(comp, new Rectangle(x, y, d.width, hp));
            try {
                x = Math.addExact(x, d.width);
            } catch (ArithmeticException ae) {
                x = Integer.MAX_VALUE;
                break;
            }
        }
        info.dimension.width = Math.max(info.dimension.width, x);
        try {
            y = Math.addExact(y, hp);
        } catch (ArithmeticException ae) {
            y = Integer.MAX_VALUE;
        }
        info.dimension.height = Math.max(info.dimension.height, y);
        return y;
    }

    enum Mode {
        MINIMUM_SIZE {
            @Override
            Dimension getSize(Component comp) {
                return comp.getMinimumSize();
            }
        },
        PREFERRED_SIZE {
            @Override
            Dimension getSize(Component comp) {
                return comp.getPreferredSize();
            }
        },
        MAXIMUM_SIZE {
            @Override
            Dimension getSize(Component comp) {
                return comp.getMaximumSize();
            }
        },
        TARGET {
            @Override
            Dimension getSize(Component comp) {
                return comp.getSize();
            }

            @Override
            Dimension getBound(Component comp) {
                return comp.getSize();
            }

            Dimension adjustSize(Element element, Component comp) {
                Dimension adjust = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
                if (element.hasAttr("width")) {
                    String width = element.attr("width");
                    if (width.endsWith("%")) {
                        float f = Float.parseFloat(width.substring(0, width.length() - 1));
                        f *= comp.getParent().getSize().width;
                        f /= 100f;
                        adjust.width = (int) f;
                    } else {
                        adjust.width = (int) Float.parseFloat(width);
                    }
                }
                if (element.hasAttr("height")) {
                    String width = element.attr("height");
                    if (width.endsWith("%")) {
                        float f = Float.parseFloat(width.substring(0, width.length() - 1));
                        f *= comp.getParent().getSize().height;
                        f /= 100f;
                        adjust.height = (int) f;
                    } else {
                        adjust.height = (int) Float.parseFloat(width);
                    }
                }
                if (adjust.width != Integer.MAX_VALUE || adjust.height != Integer.MAX_VALUE) {
                    Dimension dim = comp.getSize();
                    if (adjust.width != Integer.MAX_VALUE) {
                        dim.width = adjust.width;
                    }
                    if (adjust.height != Integer.MAX_VALUE) {
                        dim.height = adjust.height;
                    }
                    comp.setSize(dim);
                    return dim;
                }
                return getSize(comp);
            }
        };

        abstract Dimension getSize(Component comp);
        Dimension getBound(Component comp) {
            return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        Dimension adjustSize(Element element, Component comp) {
            return getSize(comp);
        }
    }

    static class Info {
        final Map<Component, Rectangle> layout = new IdentityHashMap<>();
        Dimension dimension = new Dimension(0, 0);

    }


    /**
     * Returns a string representation of this grid bag layout's values.
     * @return     a string representation of this grid bag layout.
     */
    public String toString() {
        return getClass().getName();
    }
}
