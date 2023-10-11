import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import javax.swing.*;
import javax.swing.event.*;
public class GraphPanel extends JComponent {

    private static int WIDE = 1500;
    private static int HIGH = 700;
    private static final int RADIUS = 15;
    private static final Random rnd = new Random();
    private ControlPanel control = new ControlPanel();
    private int radius = RADIUS;
    private static final int NONODES = 0;
    private int noNodes = 0;

    private static int noEdges = 0;

    public double p;
    private List<Node> nodes = new ArrayList<Node>();
    private List<Node> selected = new ArrayList<Node>();
    private static List<Edge> edges = new ArrayList<Edge>();
    private Point mousePt = new Point(WIDE / 2, HIGH / 2);
    private Rectangle mouseRect = new Rectangle();
    private boolean selecting = false;
    private boolean edgeNodes = false;

    public static void main(String[] args) throws Exception {
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                JFrame f = new JFrame("GraphPanel");
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                GraphPanel gp = new GraphPanel();
                f.add(gp.control, BorderLayout.NORTH);
                f.add(new JScrollPane(gp), BorderLayout.CENTER);
                f.pack();
                f.setLocationByPlatform(true);
                f.setVisible(true);
            }
        });
    }

    public GraphPanel() {
        this.setOpaque(true);
        this.addMouseListener(new MouseHandler());
        this.addMouseMotionListener(new MouseMotionHandler());
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(WIDE, HIGH);
    }
    @Override
    public void paintComponent(Graphics g) {
        g.setColor(new Color(0x00f0f0f0));
        g.fillRect(0, 0, getWidth(), getHeight());
        for (Edge e : edges) {
            e.draw(g);
        }
        for (Node n : nodes) {
            n.draw(g);
        }
        if (selecting) {
            g.setColor(Color.darkGray);
            g.drawRect(mouseRect.x, mouseRect.y,
                    mouseRect.width, mouseRect.height);
        }
    }

    private class MouseHandler extends MouseAdapter {

        @Override
        public void mouseReleased(MouseEvent e) {
            selecting = false;
            mouseRect.setBounds(0, 0, 0, 0);
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
            e.getComponent().repaint();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            mousePt = e.getPoint();
            if (e.isShiftDown()) {
                Node.selectToggle(nodes, mousePt);
            } else if (e.isPopupTrigger()) {
                Node.selectOne(nodes, mousePt);
                showPopup(e);

            } else if (Node.selectOne(nodes, mousePt)) {
                selecting = false;
            } else {
                Node.selectNone(nodes);
                selecting = true;
            }
            e.getComponent().repaint();
        }

        private void showPopup(MouseEvent e) {
            control.popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private class MouseMotionHandler extends MouseMotionAdapter {

        Point delta = new Point();

        @Override
        public void mouseDragged(MouseEvent e) {
            if (selecting) {
                mouseRect.setBounds(
                        Math.min(mousePt.x, e.getX()),
                        Math.min(mousePt.y, e.getY()),
                        Math.abs(mousePt.x - e.getX()),
                        Math.abs(mousePt.y - e.getY()));
                Node.selectRect(nodes, mouseRect);
                Node.updateColor(nodes);
            } else {
                delta.setLocation(
                        e.getX() - mousePt.x,
                        e.getY() - mousePt.y);
                Node.updatePosition(nodes, delta);
                mousePt = e.getPoint();
            }
            e.getComponent().repaint();
        }
    }

    public JToolBar getControlPanel() {
        return control;
    }

    private class ControlPanel extends JToolBar {
        private Action clearAll = new ClearAction("Clear");
        //private Action connect = new ConnectAction("Connect");
        private Action delete = new DeleteAction("Delete");
        private Action random = new RandomAction("Random");
        private JPopupMenu popup = new JPopupMenu();

        ControlPanel() {
            this.setLayout(new FlowLayout(FlowLayout.LEFT));
            this.setBackground(Color.lightGray);
            this.add(new JButton(clearAll));

            JSpinner js = new JSpinner();
            js.setModel(new SpinnerNumberModel(NONODES, 0, 10000, 1));
            js.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    JSpinner s = (JSpinner) e.getSource();
                    noNodes = (Integer) s.getValue();
                    if(noNodes>=500 && noNodes<700)
                    {  radius-=5;
                        Node.updateRadius(nodes, radius);}
                    if(noNodes>=700 && noNodes<1000)
                    {  radius=radius-7;
                        Node.updateRadius(nodes, radius);
                    }
                    if(noNodes>=1000)
                    {
                        radius=radius-9;
                        Node.updateRadius(nodes, radius);

                    }

                    GraphPanel.this.repaint();
                }
            });
            this.add(new JLabel("Nodes:"));
            this.add(js);
            this.add(new JButton(random));
            popup.add(new JMenuItem(delete));
        }

    }

    private class ClearAction extends AbstractAction {

        public ClearAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            nodes.clear();
            edges.clear();
            repaint();
        }
    }
    private class DeleteAction extends AbstractAction {

        public DeleteAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            ListIterator<Node> iter = nodes.listIterator();
            while (iter.hasNext()) {
                Node n = iter.next();
                if (n.isSelected()) {
                    deleteEdges(n);
                    iter.remove();
                }
            }
            repaint();
        }

        private void deleteEdges(Node n) {
            ListIterator<Edge> iter = edges.listIterator();
            while (iter.hasNext()) {
                Edge e = iter.next();
                if (e.n1 == n || e.n2 == n) {
                    iter.remove();
                }
            }
        }
    }

    private class RandomAction extends AbstractAction {

        public RandomAction(String name) {
            super(name);
        }

        private boolean isStacking(Point p)
        {
            for(int index=0; index < nodes.size(); index++)
            {
                int x_distance = p.x - nodes.get(index).getLocation().x;
                int y_distance = p.y - nodes.get(index).getLocation().y;
                double distance = Math.pow(x_distance,2) + Math.pow(y_distance,2);
                if(distance < 5*Math.pow(radius,2)) {
                    return true;
                }
            }
            return false;
        }

        public void actionPerformed(ActionEvent e) {
            int index=0;
            while(index<noNodes) {
                Point p = new Point(rnd.nextInt(WIDE-20-20)+20, rnd.nextInt(HIGH-20)+20);
                if(isStacking(p)==false)
                 {
                    nodes.add(new Node(p, radius,Color.red));
                    index++;
                }
            }
            p=10;
            for(int i = 0; i < noNodes; i++)
            {
                for(int j=i+1;j<noNodes;j++)
                {
                    float edgeProbability=rnd.nextFloat(100);
                    if(edgeProbability<p)
                    {
                        Edge edge = new Edge(nodes.get(i),nodes.get(j));
                        edges.add(edge);
                        noEdges++;
                    }
                }
            }
            repaint();
        }
    }

    private static class Edge {

        private Node n1;
        private Node n2;
        float r = rnd.nextFloat();
        float G = rnd.nextFloat();
        float b = rnd.nextFloat();
        private final Color randomColor = new Color(r, G, b);
        public Edge(Node n1, Node n2) {
            this.n1 = n1;
            this.n2 = n2;
        }
        public void draw(Graphics g) {
            Point p1 = n1.getLocation();
            Point p2 = n2.getLocation();
            g.setColor(randomColor);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }

    private class Node {

        private Point p;
        private int r;
        private Color color;
        private boolean selected = false;
        private Rectangle b = new Rectangle();

        /**
         * Construct a new node.
         */
        public Node(Point p, int r,Color color) {
            this.p = p;
            this.r = r;
            this.color=color;
            setBoundary(b);
        }

        /**
         * Calculate this node's rectangular boundary.
         */
        private void setBoundary(Rectangle b) {
            b.setBounds(p.x - r, p.y - r, 2 * r, 2 * r);
        }

        /**
         * Draw this node.
         */
        public void draw(Graphics g) {
            g.setColor(color);
            g.fillOval(b.x, b.y, b.width, b.height);
            if (selected==true) {
                    g.setColor(Color.black);
                g.fillOval(b.x, b.y, b.width, b.height);
                g.drawRect(b.x, b.y, b.width, b.height);
            }
        }
        /**
         * Return this node's location.
         */
        public Point getLocation() {
            return p;
        }

        /**
         * Return true if this node contains p.
         */
        public boolean contains(Point p) {
            return b.contains(p);
        }

        /**
         * Return true if this node is selected.
         */
        public boolean isSelected() {
            return selected;
        }

        /**
         * Mark this node as selected.
         */
        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        /**
         * Collected all the selected nodes in list.
         */
        public static void getSelected(List<Node> list, List<Node> selected) {
            selected.clear();
            for (Node n : list) {
                if (n.isSelected()) {
                    selected.add(n);
                }
            }
        }

        /**
         * Select no nodes.
         */
        public static void selectNone(List<Node> list) {
            for (Node n : list) {
                n.setSelected(false);
            }
        }

        /**
         * Select a single node; return true if not already selected.
         */
        public static boolean selectOne(List<Node> list, Point p) {
            for (Node n : list) {
                if (n.contains(p)) {
                    if (!n.isSelected()) {
                        Node.selectNone(list);
                        n.setSelected(true);
                    }
                    return true;
                }
            }
            return false;
        }

        /**
         * Select each node in r.
         */
        public static void selectRect(List<Node> list, Rectangle r) {
            for (Node n : list) {
                n.setSelected(r.contains(n.p));
            }
        }

        /**
         * Toggle selected state of each node containing p.
         */
        public static void selectToggle(List<Node> list, Point p) {
            for (Node n : list) {
                if (n.contains(p)) {
                    n.setSelected(!n.isSelected());
                }
            }
        }

        /**
         * Update each node's position by d (delta).
         */
        public static void updatePosition(List<Node> list, Point d) {
            for (Node n : list) {
                if (n.isSelected()) {
                    n.p.x += d.x;
                    n.p.y += d.y;
                    n.setBoundary(n.b);
                }
            }
        }

        /**
         * Update each node's radius r.
         */
        public static void updateRadius(List<Node> list, int r) {
            for (Node n : list) {
                if (n.isSelected()) {
                    n.r = r;
                    n.setBoundary(n.b);
                }
            }
        }
        private static boolean isEdge(Node node1, Node node2)
        {
            for(int i=0; i<noEdges; i++)
            {
                Edge arc = edges.get(i);
                if(arc.n1==node1 && arc.n2==node2)
                {
                    return true;
                }
            }
            return false;
        }

        public static void updateColor(List<Node> list) {
            float r = rnd.nextFloat();
            float G = rnd.nextFloat();
            float b = rnd.nextFloat();
            Color randomColor = new Color(r, G, b);
            for (Node n : list) {
                if (n.isSelected()) {
                    n.color = randomColor;
                    for(Node n1:list)
                    {
                        if(isEdge(n,n1)) n1.color=n.color;
                    }
                }
            }
        }
    }}

