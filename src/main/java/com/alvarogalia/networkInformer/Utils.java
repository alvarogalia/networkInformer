package com.alvarogalia.networkInformer;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
import java.net.*;
import java.util.concurrent.TimeUnit;

public class Utils {
    public static String getIp() throws Exception {
        URL whatismyip = new URL("http://checkip.amazonaws.com");
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));
            String ip = in.readLine();
            return ip;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void reboot() throws RuntimeException, IOException {
        String shutdownCommand;
        String operatingSystem = System.getProperty("os.name");

        if ("Linux".equals(operatingSystem) || "Mac OS X".equals(operatingSystem)) {
            shutdownCommand = "reboot";
        } else if ("Windows".equals(operatingSystem)) {
            shutdownCommand = "reboot /r";
        } else {
            throw new RuntimeException("Unsupported operating system.");
        }

        Runtime.getRuntime().exec(shutdownCommand);
        System.exit(0);
    }

    public static String getMacAdress() throws UnknownHostException, SocketException {
            InetAddress ip;
            ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            byte[] mac = network.getHardwareAddress();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }
            return sb.toString();
    }

    public static String enviaFTP(String ip, int port, String username, String password, String source, String end){

        InetAddress thisIp = null;

        FTPClient client = new FTPClient();
        FileInputStream fis = null;
        String error = "";
        try {
            thisIp = InetAddress.getLocalHost();

            client.connect(ip, port);
            if (client.login(username, password)) {
                error += client.getReplyCode();
                client.setFileTransferMode(FTP.STREAM_TRANSFER_MODE);
                client.setFileType(FTP.BINARY_FILE_TYPE);
                client.enterRemotePassiveMode();
                String filename = source;
                fis = new FileInputStream(filename);
                if (client.storeFile(end, fis)) {
                    error += client.getReplyCode();
                } else {
                    error += client.getReplyCode();
                }
                client.logout();
            } else {
                error += client.getReplyCode();
            }


        }catch(ConnectException e){
            error = e.getMessage();
        } catch (SocketException e) {
            error = e.getMessage();
        } catch (FileNotFoundException e) {
            error = e.getMessage();
        } catch (IOException e) {
            error = e.getMessage();
        } finally {
            try {
                TimeUnit.SECONDS.sleep(10);
                if (fis != null) {
                    fis.close();
                }
                try{
                    //print("Borrando " + source );
                    File file = new File(source);
                    if(file.exists()){
                        file.delete();
                    }
                }catch(Exception e){
                    error += e.getMessage();
                }
                client.disconnect();

            } catch (IOException e) {
                error += e.getMessage();
            } catch (InterruptedException e) {
                error += e.getMessage();
            }

        }
        return error;
    }

}
