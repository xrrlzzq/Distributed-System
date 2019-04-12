package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Properties;

import javax.xml.ws.Endpoint;




import common.LogTool;
import server.service.DCMS;

/**
 * 
 * This class is to receive a UDP request and deal with the request
 *
 */


class RequestThread extends Thread{
	DatagramSocket socket=null;
    public RequestThread(int port) throws SocketException{
    	super();//inherited 
    	socket = new DatagramSocket(port);//port of this computer for receive data by UDP 
    }
    
    @Override
    public void run(){
    	byte[] receiveBuf = new byte[1024];		
        DatagramPacket dpReceive = new DatagramPacket(receiveBuf, receiveBuf.length); // instantiate the datagram packet 

		while(true)
		try{	 
	        socket.receive(dpReceive);//blocks(waiting) until a message come in
	        InetAddress destAddr = dpReceive.getAddress();
	        int destPort = dpReceive.getPort();		
	       		       
	        String requestInfo=new String(dpReceive.getData(), 0, dpReceive.getLength()); // create the buffer to receive the request
	        String info="";
	        if(requestInfo.indexOf("getRecordCounts")!=-1){ // if the request information contain "getRecordCounts",then call the getMyCounts() from DCMS
	        	
	        	info=String.format("%d",DCMS.getMyCounts()); // get the count of local records	
	        }
	        else if(requestInfo.startsWith("t")){ // if the request information startwith "T", then transfer the record
	        	info=DCMS.dealTransferedRecord(requestInfo);
	        }
	        else
	        	info="unknow request command!";
	        
	        byte[] data = info.getBytes();
	        
			DatagramPacket dpSend = new DatagramPacket(data,data.length, destAddr, destPort);//port of other computer to send data by UDP
			socket.send(dpSend);
			dpReceive.setLength(receiveBuf.length);//for receive next packet
		}catch(Exception e)
		 {
			e.printStackTrace();
			continue;
		 }					        	
    }
}
/**
 * This is the class of Remote server which deal with the logic of server side  
 * 
 *
 */

public class CenterServer { 
	public static void main(String[] args) {
		if(args.length <= 0){ // To check the validity of server name
		      System.out.println("usage: java server.CenterServer serverLocation");
		      System.out.println("serverLocation: mtl,lvl,ddo");
		      return;
		}
		
		String serverLocation = args[0].toLowerCase();		// To get the server location(mtl,lvl,ddo) 
		
		try{
    		Properties p=new Properties();
            InputStream in=new FileInputStream(new File(System.getProperty("user.dir")+File.separator+"config.properties"));
            p.load(in);
			String mtlIP = p.getProperty("mtl");  // To get the ip of mtl server from config file
			String lvlIP = p.getProperty("lvl");  // To get the ip of lvl server from config file
			String ddoIP = p.getProperty("ddo");  // To get the ip of ddp server from config file
			int udpServerPort = Integer.parseInt(p.getProperty("udpServerPort")); // To get the UDP server port from config file
			int udpClientPort = Integer.parseInt(p.getProperty("udpClientPort")); // To get the UDP client port from config file
			String ip=p.getProperty(serverLocation);
			String port=p.getProperty("webservice");
            in.close();
            String url=String.format("http://%s:%s/",ip,port); // Generate the URL
            LogTool log = new LogTool(serverLocation+".log");  // write the corresponding information to log file 
			DCMS op=new DCMS(); // instantiate the DCMS object(remote web service object)
			op.init(mtlIP,lvlIP,ddoIP,udpServerPort,udpClientPort,serverLocation,log); // initialize DCMS object
		    Thread t=new RequestThread(udpServerPort); 
            t.start(); // start a new thread for a udp request
            Endpoint.publish(url, op); // publish the DCMS object(remote web service object) to the URL
            log.logMsg(String.format("sever %s start!", serverLocation),true,"server."+serverLocation,"main");
            
   
		}catch(Exception e){
			e.printStackTrace();
		}

	}

}
