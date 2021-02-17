import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * 封装被控端的方法
 *
 * @author Administrator
 */
public class Client {
    Socket socket;

    DataOutputStream dos = null;
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int width = (int) screenSize.getWidth();
    int height = (int) screenSize.getHeight();
    Robot robot;
    boolean isLive = true;

    public Client() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    /**
     * 连接服务器
     */
    public void connect(InetAddress address, int port) {
        try {
            socket = new Socket(address, port);
            dos = new DataOutputStream(socket.getOutputStream());
            // dos.writeUTF("client");
        } catch (IOException e) {
            System.out.println("连接" + address + ":" + port + "失败");
            e.printStackTrace();
        }
    }

    /**
     * 获取屏幕截图并保存
     *
     * @return
     */
    public BufferedImage getScreenShot() {
        BufferedImage bfImage = robot.createScreenCapture(new Rectangle(0, 0, width, height));
        return bfImage;
    }

    public void load() {
        byte[] bytes = "client".getBytes();
        MyProtocol.send(MyProtocol.LOGIN, bytes, dos);
    }

    public void sendImage(BufferedImage buff) {
        if (buff == null) {
            return;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(buff, "png", baos);
            MyProtocol.send(MyProtocol.IMAGE, baos.toByteArray(), dos);
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 关闭客户端，释放掉资源
    public void close() {
        //向服务器发送消息
        MyProtocol.send(MyProtocol.LOGOUT, new String("logout").getBytes(), dos);
        System.out.println(socket.getInetAddress() + "客户端关闭");
        // 关闭资源
        try {
            if (dos != null) {
                dos.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws UnknownHostException {
        final Client client = new Client();
        client.connect(InetAddress.getLocalHost(), 33000);
        client.load();// 登录
        while (client.isLive) {
            client.sendImage(client.getScreenShot());
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        client.close();
    }
}
