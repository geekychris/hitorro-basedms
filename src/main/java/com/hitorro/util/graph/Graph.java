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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 */


public class Graph extends Panel implements ActionListener, ItemListener {

    GraphPanel panel = new GraphPanel(this);
    Panel controlPanel;

    Button scramble = new Button("Scramble");
    Button shake = new Button("Shake");
    Checkbox stress = new Checkbox("Stress");
    Checkbox random = new Checkbox("Random");
    Checkbox description = new Checkbox("description");


    public GraphPanel getGraphPanel() {
        return panel;
    }

    public void init() {
        setLayout(new BorderLayout());


        add("Center", panel);
        controlPanel = new Panel();
        add("South", controlPanel);

        controlPanel.add(scramble);
        scramble.addActionListener(this);
        controlPanel.add(shake);
        shake.addActionListener(this);
        controlPanel.add(stress);
        stress.addItemListener(this);
        controlPanel.add(random);
        random.addItemListener(this);
        controlPanel.add(description);
        description.addItemListener(this);


        Dimension d = getSize();
        //String center = getParameter("center");
        String center = null;
        if (center != null) {
            Node n = panel.nodes[panel.findNode(center)];
            n.x = d.width / 2;
            n.y = d.height / 2;
            n.fixed = true;
        }
    }

    public void destroy() {
        remove(panel);
        remove(controlPanel);
    }

    public void start() {
        panel.start();
    }

    public void stop() {
        panel.stop();
    }

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();

        if (src == scramble) {
            //play(getCodeBase(), "audio/computer.au");
            Dimension d = getSize();
            for (int i = 0; i < panel.nnodes; i++) {
                Node n = panel.nodes[i];
                if (!n.fixed) {
                    n.x = 10 + (d.width - 20) * Math.random();
                    n.y = 10 + (d.height - 20) * Math.random();
                }
            }
            return;
        }

        if (src == shake) {
            //play(getCodeBase(), "audio/gong.au");
            Dimension d = getSize();
            for (int i = 0; i < panel.nnodes; i++) {
                Node n = panel.nodes[i];
                if (!n.fixed) {
                    n.x += 80 * Math.random() - 40;
                    n.y += 80 * Math.random() - 40;
                }
            }
        }

    }

    public void itemStateChanged(ItemEvent e) {
        Object src = e.getSource();
        boolean on = e.getStateChange() == ItemEvent.SELECTED;
        if (src == stress) {
            panel.stress = on;
        } else if (src == random) {
            panel.random = on;
        } else if (src == description) {
            panel.description = on;
        }
    }

    public String[][] getParameterInfo() {
        String[][] info = {
                {"edges", "delimited string", "A comma-delimited listFiles of all the edges.  It takes the form of 'C-N1,C-N2,C-N3,C-NX,N1-N2/M12,N2-N3/M23,N3-NX/M3X,...' where C is the name of center node (see 'center' parameter) and NX is a node attached to the center node.  For the edges connecting nodes to each other (and not to the center node) you may (optionally) specify a length MXY separated from the edge name by a forward slash."},
                {"center", "string", "The name of the center node."}
        };
        return info;
    }

}

