import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.InputStream;

/**
 * NOTICE: exec not has some env with shell channel
 */
public class JSchExec {
    public static void main(String[] args) {
        JSch jsch = new JSch();

        try {
            Session session = jsch.getSession("lorancechen", "192.168.1.149", 22);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setPassword(" ");
            session.setConfig(config);
            session.connect();

//            String command2 = "source /etc/profile; source ~/.profile; printenv; sshpass -p ' ' ssh lorancechen@192.168.1.149;exit ; ls";
            String command = "bash -c \"lsss\"";
//            String command0 = "source ~/.zshrc";
//            String command0 = "bash -c 'which ssh'";
//            String command = "bash -c \"/usr/local/Cellar/sshpass/1.06/bin/sshpass -p ' ' ssh lorancechen@192.168.1.149\"";
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);
//            new Thread(() -> {
//                try {
//                    Thread.sleep(5000);
//                } catch (Exception e) {
//
//                }
//                System.out.println("((ChannelExec) channel).setCommand(command2);");
//
//                ((ChannelExec) channel).setCommand(command2);
//
//                try {
//                    Thread.sleep(2000);
//                } catch (Exception e) {
//
//                }
//                channel.disconnect();
//                session.disconnect();
//            }).start();
            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);
            InputStream in = channel.getInputStream();

            channel.connect();

            byte[] tmp = new byte[1024];
            new Thread(() -> {
                try {
                    while (true) {
                        while (in.available() > 0) {
                            int i = in.read(tmp, 0, 1024);
                            if (i < 0)
                                break;
                            System.out.print(new String(tmp, 0, i));
                        }
                        if (channel.isClosed()) {
                            System.out.println("exit-status: " + channel.getExitStatus());
                            break;
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (Exception ee) {
                        }
                    }
                } catch (Exception e) {

                }
            }).start();

            Thread.currentThread().join();
//            channel.disconnect();
//            session.disconnect();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
