import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static ExecutorService tp = Executors.newCachedThreadPool();

    public static Map<String, Socket> client = new HashMap<String, Socket>();
    public static View view = new View();
    public static String curKey = null;
    public static boolean serverLive = true;

    /**
     * 处理客户端消息
     */
    public static class HandleClient implements Runnable {
        private Socket socket;
        private DataInputStream dis = null;
        private String key = null;
        private boolean isLive = true;

        public HandleClient(Socket socket) {
            this.socket = socket;
            try {
                this.dis = new DataInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (isLive) {
                if (!Server.serverLive) {
                    System.exit(1);
                }
                MyProtocol.Result result = MyProtocol.getResult(this, dis);
                if (result != null) {
                    handleType(result.getType(), result.getData());
                }
            }
        }

        //处理类型type的消息
        private void handleType(MyProtocol type, byte[] data) {
            System.out.println(type);
            try {
                switch (type) {
                    case IMAGE:
                        if (!Server.curKey.equals(key)) {
                            break;
                        }
                        ByteArrayInputStream bai = new ByteArrayInputStream(data);
                        BufferedImage buff = ImageIO.read(bai);
                        //为屏幕监控视图设置BufferedImage
                        Server.view.centerPanel.setBufferedImage(buff);
                        Server.view.centerPanel.repaint();
                        bai.close();
                        break;
                    case LOGIN:
                        String msg = new String(data);
                        if ("client".equals(msg)) {
                            key = socket.getInetAddress().getHostAddress();
                            Server.client.put(key, socket);
                            Server.view.setTreeNode(Server.view.addValue(key));
                            if (Server.curKey == null) {
                                Server.curKey = key;
                            }
                        }
                        break;
                    case LOGOUT:
                        close();
                        break;
                    default:
                        break;
                }
            } catch (IOException e) {
                try {
                    if (key != null && key.contains("client")) {
                        Server.client.remove(key);
                    }
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                e.printStackTrace();
            }
        }

        public void close() {
            Server.view.setTreeNode(Server.view.removeValue(key));
            Server.client.remove(key);
            Server.view.centerPanel.setBufferedImage(null);
            Server.view.centerPanel.repaint();
            Server.curKey = null;
            isLive = false;
            try {
                socket.close();
                dis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 封装服务器端的视图层
     */
    static class View {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        private int width;
        private int height;

        private DefaultTreeModel model;
        DefaultMutableTreeNode root;
        DrawPanel centerPanel;
        List<String> list = new ArrayList<>();

        public View() {
            width = (int) (screenSize.getWidth() * 0.7);
            height = (int) (screenSize.getHeight() * 0.8);
        }

        public static class DrawPanel extends JPanel {
            private Graphics g;

            private BufferedImage bufferedImage = null;

            public void setBufferedImage(BufferedImage bufferedImage) {
                this.bufferedImage = bufferedImage;
                if (bufferedImage != null) {
                    setPreferredSize(new Dimension(this.bufferedImage.getWidth(), this.bufferedImage.getHeight()));
                }
            }

            @Override
            public void paint(Graphics g) {
                super.paint(g);
                if (bufferedImage == null) {
                    g.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    g.drawImage(bufferedImage, 0, 0, Color.white, null);
                }
            }
        }

        //创建视图
        public void create() {
            //得到内容窗格
            JFrame frame = new JFrame("远程屏幕监视系统");
            Container container = frame.getContentPane();

            //左侧
            JPanel leftPanel = new JPanel();
            leftPanel.setBackground(Color.darkGray);
            container.add(leftPanel, BorderLayout.WEST);
            //树
            root = new DefaultMutableTreeNode("所有连接的被控端");
            model = new DefaultTreeModel(root);
            JTree tree = new JTree(model);
            tree.setBackground(Color.darkGray);

            tree.addTreeSelectionListener(new TreeSelectionListener() {
                @Override
                public void valueChanged(TreeSelectionEvent e) {
                    JTree tree = (JTree) e.getSource();
                    DefaultMutableTreeNode selectionNode = (DefaultMutableTreeNode) tree
                            .getLastSelectedPathComponent();
                    String nodeName = selectionNode.toString();
                    curKey = nodeName;
                }
            });

            //设置树节点的样式
            DefaultTreeCellRenderer cr = new DefaultTreeCellRenderer();
            cr.setBackgroundNonSelectionColor(Color.darkGray);
            cr.setTextNonSelectionColor(Color.white);
            tree.setCellRenderer(cr);
            JScrollPane jsp = new JScrollPane(tree);
            JScrollBar bar = jsp.getHorizontalScrollBar();
            bar.setBackground(Color.darkGray);
            jsp.setBorder(null);
            leftPanel.add(jsp);

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    System.out.println("server closed");
                    serverLive = false;
                }
                @Override
                public void windowClosing(WindowEvent e) {
                    System.out.println("server closed");
                    serverLive = false;
                }
            });

            centerPanel = new DrawPanel();
            container.add(new JScrollPane(centerPanel));
            frame.setSize(width, height);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }

        /**
         * 添加树节点
         */
        public void setTreeNode(List<String> l) {
            list = l;
            root.removeAllChildren();
            for (String s : list) {
                DefaultMutableTreeNode node1 = new DefaultMutableTreeNode(s);
                root.add(node1);
            }
            model.reload();
        }

        public List<String> addValue(String key) {
            list.add(key);
            return list;
        }

        public List<String> removeValue(String key) {
            list.remove(key);
            return list;
        }

        public void clear() {
            list.clear();
        }

    }

    public static void main(String[] args) {
        try {
            System.out.println("server running, IP:" + InetAddress.getLocalHost());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        try {
            ServerSocket serverSocket = new ServerSocket(33000);
            view.create();
            while (serverLive) {
                Socket clientSocket = serverSocket.accept();
                System.out.println(clientSocket.getRemoteSocketAddress() + " connect!");
                tp.execute(new HandleClient(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
