/*
 * Copyright (c) 2006-2025 Chris Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.hitorro.util.graph;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 */
public class GraphPanel extends Panel
        implements Runnable, MouseListener, MouseMotionListener {
    final Color fixedColor = Color.red;
    final Color selectColor = Color.pink;
    final Color edgeColor = Color.black;
    final Color nodeColor = new Color(250, 220, 100);
    final Color stressColor = Color.darkGray;
    final Color arcColor1 = Color.black;
    final Color arcColor2 = Color.pink;
    final Color arcColor3 = Color.red;
    Graph graph;
    int nnodes;
    Node nodes[] = new Node[100];
    int nedges;
    Edge edges[] = new Edge[200];
    Thread relaxer;
    boolean stress;
    boolean description;
    boolean random;
    int numMouseButtonsDown = 0;
    Node pick;
    boolean pickfixed;
    Image offscreen;
    Dimension offscreensize;
    Graphics offgraphics;
    GraphPanel(Graph graph) {
        this.graph = graph;
        addMouseListener(this);
    }

    int findNode(String lbl) {
        for (int i = 0; i < nnodes; i++) {
            if (nodes[i].lbl.equals(lbl)) {
                return i;
            }
        }
        return addNode(lbl);
    }

    int addNode(String lbl) {
        Node n = new Node();
        n.x = 10 + 380 * Math.random();
        n.y = 10 + 380 * Math.random();
        n.lbl = lbl;
        nodes[nnodes] = n;
        return nnodes++;
    }

    public void addEdge(String from, String to, int len, String description) {
        Edge e = new Edge();
        e.description = description;
        e.from = findNode(from);
        e.to = findNode(to);
        e.len = len;
        edges[nedges++] = e;
    }

    public void run() {
        Thread me = Thread.currentThread();
        while (relaxer == me) {
            relax();
            if (random && (Math.random() < 0.03)) {
                Node n = nodes[(int) (Math.random() * nnodes)];
                if (!n.fixed) {
                    n.x += 100 * Math.random() - 50;
                    n.y += 100 * Math.random() - 50;
                }
                //graph.play(graph.getCodeBase(), "audio/drip.au");
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    synchronized void relax() {
        for (int i = 0; i < nedges; i++) {
            Edge e = edges[i];
            double vx = nodes[e.to].x - nodes[e.from].x;
            double vy = nodes[e.to].y - nodes[e.from].y;
            double len = Math.sqrt(vx * vx + vy * vy);
            len = (len == 0) ? .0001 : len;
            double f = (edges[i].len - len) / (len * 3);
            double dx = f * vx;
            double dy = f * vy;

            nodes[e.to].dx += dx;
            nodes[e.to].dy += dy;
            nodes[e.from].dx += -dx;
            nodes[e.from].dy += -dy;
        }

        for (int i = 0; i < nnodes; i++) {
            Node n1 = nodes[i];
            double dx = 0;
            double dy = 0;

            for (int j = 0; j < nnodes; j++) {
                if (i == j) {
                    continue;
                }
                Node n2 = nodes[j];
                double vx = n1.x - n2.x;
                double vy = n1.y - n2.y;
                double len = vx * vx + vy * vy;
                if (len == 0) {
                    dx += Math.random();
                    dy += Math.random();
                } else if (len < 100 * 100) {
                    dx += vx / len;
                    dy += vy / len;
                }
            }
            double dlen = dx * dx + dy * dy;
            if (dlen > 0) {
                dlen = Math.sqrt(dlen) / 2;
                n1.dx += dx / dlen;
                n1.dy += dy / dlen;
            }
        }

        Dimension d = getSize();
        for (int i = 0; i < nnodes; i++) {
            Node n = nodes[i];
            if (!n.fixed) {
                n.x += Math.max(-5, Math.min(5, n.dx));
                n.y += Math.max(-5, Math.min(5, n.dy));
            }
            if (n.x < 0) {
                n.x = 0;
            } else if (n.x > d.width) {
                n.x = d.width;
            }
            if (n.y < 0) {
                n.y = 0;
            } else if (n.y > d.height) {
                n.y = d.height;
            }
            n.dx /= 2;
            n.dy /= 2;
        }
        repaint();
    }

    public void paintNode(Graphics g, Node n, FontMetrics fm) {
        int x = (int) n.x;
        int y = (int) n.y;
        g.setColor((n == pick) ? selectColor : (n.fixed ? fixedColor : nodeColor));
        int w = fm.stringWidth(n.lbl) + 10;
        int h = fm.getHeight() + 4;
        g.fillRect(x - w / 2, y - h / 2, w, h);
        g.setColor(Color.black);
        g.drawRect(x - w / 2, y - h / 2, w - 1, h - 1);
        g.drawString(n.lbl, x - (w - 10) / 2, (y - (h - 4) / 2) + fm.getAscent());
    }

    public synchronized void update(Graphics g) {
        Dimension d = getSize();
        if ((offscreen == null) || (d.width != offscreensize.width) || (d.height != offscreensize.height)) {
            offscreen = createImage(d.width, d.height);
            offscreensize = d;
            if (offgraphics != null) {
                offgraphics.dispose();
            }
            offgraphics = offscreen.getGraphics();
            offgraphics.setFont(getFont());
        }

        offgraphics.setColor(getBackground());
        offgraphics.fillRect(0, 0, d.width, d.height);
        for (int i = 0; i < nedges; i++) {
            Edge e = edges[i];
            int x1 = (int) nodes[e.from].x;
            int y1 = (int) nodes[e.from].y;
            int x2 = (int) nodes[e.to].x;
            int y2 = (int) nodes[e.to].y;
            int len = (int) Math.abs(Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)) - e.len);
            offgraphics.setColor((len < 10) ? arcColor1 : (len < 20 ? arcColor2 : arcColor3));
            offgraphics.drawLine(x1, y1, x2, y2);
            if (stress) {
                String lbl = String.valueOf(len);
                offgraphics.setColor(stressColor);
                offgraphics.drawString(lbl, x1 + (x2 - x1) / 2, y1 + (y2 - y1) / 2);
                offgraphics.setColor(edgeColor);
            }
            if (description) {
                String lbl = e.description;
                offgraphics.setColor(stressColor);
                offgraphics.drawString(lbl, x1 + (x2 - x1) / 2, y1 + (y2 - y1) / 2);
                offgraphics.setColor(edgeColor);
            }
        }

        FontMetrics fm = offgraphics.getFontMetrics();
        for (int i = 0; i < nnodes; i++) {
            paintNode(offgraphics, nodes[i], fm);
        }
        g.drawImage(offscreen, 0, 0, null);
    }

    //1.1 event handling
    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        numMouseButtonsDown++;
        addMouseMotionListener(this);
        double bestdist = Double.MAX_VALUE;

        int x = e.getX();
        int y = e.getY();
        for (int i = 0; i < nnodes; i++) {
            Node n = nodes[i];
            double dist = (n.x - x) * (n.x - x) + (n.y - y) * (n.y - y);
            if (dist < bestdist) {
                pick = n;
                bestdist = dist;
            }
        }
        pickfixed = pick.fixed;
        pick.fixed = true;
        pick.x = x;
        pick.y = y;

        repaint();
        e.consume();
    }

    public void mouseReleased(MouseEvent e) {
        numMouseButtonsDown--;
        removeMouseMotionListener(this);

        pick.fixed = pickfixed;
        pick.x = e.getX();
        pick.y = e.getY();
        if (numMouseButtonsDown == 0) {
            pick = null;
        }

        repaint();
        e.consume();
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
        pick.x = e.getX();
        pick.y = e.getY();
        repaint();
        e.consume();
    }

    public void mouseMoved(MouseEvent e) {
    }

    public void start() {
        relaxer = new Thread(this);
        relaxer.start();
    }

    public void stop() {
        relaxer = null;
    }

}