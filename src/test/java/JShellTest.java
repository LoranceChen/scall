import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class JShellTest {
    public static void main(String[] arg){
        try{
            JSch jsch=new JSch();

            //jsch.setKnownHosts("/home/foo/.ssh/known_hosts");
            Session session=jsch.getSession("lorancechen", "192.168.1.149", 22);

            session.setPassword(" ");
            session.setConfig("StrictHostKeyChecking", "no");

            //session.connect();
            session.connect(30000);   // making a connection with timeout.

            Channel channel=session.openChannel("shell");

            // Enable agent-forwarding.
            ((ChannelShell)channel).setPty(false);
            channel.setInputStream(System.in);
            channel.setOutputStream(System.out);
            channel.setExtOutputStream(System.out);

            channel.connect(60*1000);
        }
        catch(Exception e){
            System.out.println(e);
        }

    }

}