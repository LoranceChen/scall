import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class JShellTest {
    public static void main(String[] arg){
        try{
            JSch jsch=new JSch();

            //jsch.setKnownHosts("/home/foo/.ssh/known_hosts");
            Session session=jsch.getSession("lorance", "192.168.1.230", 22);

            session.setPassword("1");
            session.setConfig("StrictHostKeyChecking", "no");

            //session.connect();
            session.connect(30000);   // making a connection with timeout.

            Channel channel=session.openChannel("shell");

            // Enable agent-forwarding.
            //((ChannelShell)channel).setAgentForwarding(true);
            channel.setInputStream(System.in);
            channel.setOutputStream(System.out);

            channel.connect(60*1000);
        }
        catch(Exception e){
            System.out.println(e);
        }

    }

}