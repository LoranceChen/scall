import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.*;

public class ConnectSSH {

    public void connect(String dnsName, String privKey) throws IOException {
        JSch jSch = new JSch();


        try {

            //Authenticate through Private Key File
            jSch.addIdentity(privKey);
            //Give the user and dnsName
            Session session = jSch.getSession("root", dnsName, 22);
            //Required if not a trusted host
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            System.out.println("Connecting SSH to " + dnsName + " - Please wait for few minutes... ");
            session.connect();
            //Open a shell 
            Channel channel=session.openChannel("shell");
            channel.setOutputStream(System.out);
            //Create a Shell Script
            File shellScript = createShellScript();
            //Convert the shell script to byte stream
            FileInputStream fin = new FileInputStream(shellScript);
            byte fileContent[] = new byte[(int)shellScript.length()];
            fin.read(fileContent);
            InputStream in = new ByteArrayInputStream(fileContent);
            //Set the shell script to the channel as input stream
            channel.setInputStream(in);
            //Connect and have fun!
            channel.connect();

        } catch (JSchException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public File createShellScript() {
        String filename = "shellscript.sh";
        File fstream = new File(filename);

        try{
            // Create file
            PrintStream out = new PrintStream(new FileOutputStream(fstream));
            out.println("#!/bin/bash");
            out.println("echo \"hi\" > /tmp/test.info");
            out.println("echo \"n\" > /tmp/fdisk.in");
            out.println("echo \"p\" >> /tmp/fdisk.in");
            out.println("echo \"1\" >> /tmp/fdisk.in");
            out.println("echo >> /tmp/fdisk.in");
            out.println("echo >> /tmp/fdisk.in");
            out.println("echo \"w\" >> /tmp/fdisk.in");

            out.println("/sbin/fdisk /dev/sdf < /tmp/fdisk.in");
            out.println("mkfs.ext3 /dev/sdf1");
            out.println("mkdir /usr/myebs");
            out.println("mount /dev/sdf1 /usr/myebs");
            out.println("partprobe /dev/sdf1");

            out.println("echo \"Success\"");

            //Close the output stream
            out.close();
        }catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
        return fstream;

    }

    public static void main(String[] args) {
        ConnectSSH ssh = new ConnectSSH();
        String privKey = "/Users/neo/Desktop/mykey.pem";
        try {
            ssh.connect("yourexampleserver.com", privKey);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}