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
            Session session = jsch.getSession("lorancechen", "localhost", 22);
//            java.util.Properties config = new java.util.Properties();
//            config.put("StrictHostKeyChecking", "no");
            session.setPassword(" ");
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

//            String command2 = "source /etc/profile; source ~/.profile; printenv; sshpass -p ' ' ssh lorancechen@192.168.1.149;exit ; ls";
//            String command = "/bin/bash -c \"lss\"";
            String command = "ls -al";
//            String command = "ls";
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
            ((ChannelExec) channel).setErrStream(null);
//            ((ChannelExec) channel).setOutputStream(System.out);
//            ((ChannelExec) channel).setExtOutputStream(System.err);
            InputStream in = channel.getInputStream();
            InputStream in2 = ((ChannelExec) channel).getErrStream();
//
//            {
//                byte[] tmp = new byte[100];
//                new Thread(() -> {
//                    try {
//                        while (true) {
//                            System.out.println("in.available222() - " + in.available());
//                            while (in2.available() > 0) {
//                                int i = in2.read(tmp, 0, 100);
//                                System.out.println("i222 - " + i);
//
//                                if (i < 0)
//                                    break;
//                                System.out.print(new String(tmp, 0, i));
//                                System.out.print("222");
//                            }
//                            if (channel.isClosed()) {
//                                System.out.println("exit-status2222: " + channel.getExitStatus());
//                                break;
//                            }
//                            try {
//                                Thread.sleep(1000);
//                            } catch (Exception ee) {
//                            }
//                        }
//                    } catch (Exception e) {
//                        System.out.println("额222 - " + e.getMessage());
//                    }
//                }).start();
//            }
            channel.connect();

            byte[] tmp = new byte[100];
            new Thread(() -> {
                try {
                    while (true) {
                        System.out.println("in.available() - " + in.available());
                        while (in.available() > 0) {
                            int i = in.read(tmp, 0, 100);
                            System.out.println("i - " + i);

                            if (i < 0)
                                break;
                            System.out.print(new String(tmp, 0, i));
                            System.out.print("111");
                        }
//                        if (channel.isClosed()) {
//                            System.out.println("exit-status: " + channel.getExitStatus());
//                            break;
//                        }
//                        try {
//                            Thread.sleep(1000);
//                        } catch (Exception ee) {
//                        }

                        System.out.println("in2.available() - " + in2.available());

                        while (in2.available() > 0) {
                            int i = in2.read(tmp, 0, 100);
                            System.out.println("i222 - " + i);

                            if (i < 0)
                                break;
                            System.out.print(new String(tmp, 0, i));
                            System.out.print("222");
                        }
                        if (channel.isClosed()) {
                            System.out.println("exit-status2222: " + channel.getExitStatus());
                            break;
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (Exception ee) {
                        }
                    }
                } catch (Exception e) {
                    System.out.println("fail - " + e);
                }
            }).start();

            Thread.sleep(10 * 1000);
            channel.disconnect();
            session.disconnect();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
