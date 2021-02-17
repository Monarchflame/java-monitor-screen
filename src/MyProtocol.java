import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 封装一个协议
 *
 * @author Administrator
 */
public enum MyProtocol {
    // 图片
    IMAGE(1),
    // 登录
    LOGIN(2),
    // 退出
    LOGOUT(3);
    private final int index;

    MyProtocol(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static MyProtocol getMyProtocolByIndex(int index) {
        for (MyProtocol protocol : MyProtocol.values()) {
            if (index == protocol.getIndex()) {
                return protocol;
            }
        }
        return null;
    }

    public static class Result {
        private MyProtocol type;
        private byte[] data;

        public Result(MyProtocol type, int totalLen, byte[] data) {
            this.type = type;
            this.data = data;
        }

        public MyProtocol getType() {
            return type;
        }

        public void setType(MyProtocol type) {
            this.type = type;
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }
    }

    public static void send(MyProtocol protocol, byte[] bytes, DataOutputStream dos) {
        int totalLen = 1 + 4 + bytes.length;
        try {
            dos.writeByte(protocol.getIndex());
            dos.writeInt(totalLen);
            dos.write(bytes);
            dos.flush();
        } catch (IOException e) {
            System.exit(0);
        }
    }

    public static Result getResult(Server.HandleClient socket, DataInputStream dis) {
        try {
            int index = dis.readByte() & 0xFF;
            int totalLen = dis.readInt();
            byte[] bytes = new byte[totalLen - 4 - 1];
            dis.readFully(bytes);
            return new Result(getMyProtocolByIndex(index), totalLen, bytes);
        } catch (IOException e) {
            e.printStackTrace();
            // 关闭连接
            System.out.println("关闭连接");
            socket.close();
        }
        return null;
    }
}