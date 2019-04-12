package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.Properties;

class RequestThread extends Thread{
	DatagramSocket socket=null;
    public RequestThread(int port) throws SocketException{
    	super();//inherited 
    	socket = new DatagramSocket(port);//port of this computer for receive data by UDP 
    }
    
    @Override
    public void run(){
    	byte[] receiveBuf = new byte[1024];		
        DatagramPacket dpReceive = new DatagramPacket(receiveBuf, receiveBuf.length);

		while(true)
		try{	 
	        socket.receive(dpReceive);//blocks(waiting) until a message come in
	        InetAddress destAddr = dpReceive.getAddress();
	        int destPort = dpReceive.getPort();		
	       		       
	        String requestInfo=new String(dpReceive.getData(), 0, dpReceive.getLength());
	        String countsInfo="";
	        if(requestInfo.indexOf("getRecordCounts")!=-1){
	        	
	        	countsInfo=String.format("%d",ManagerOperationsImpl.getMyCounts());	
	        }
	        else
	        	countsInfo="unknow request command!";
	        
	        byte[] data = countsInfo.getBytes();
	        
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


public class CenterServer { 
	public static void main(String[] args) {
		if(args.length <= 0){
		      System.out.println("usage: java server.CenterServer serverLocation");
		      System.out.println("serverLocation: mtl,lvl,ddo");
		      return;
		}
		
		String serverLocation = args[0].toLowerCase();		
		
		try{
    		Properties p=new Properties();
            InputStream in=new FileInputStream(new File(System.getProperty("user.dir")+"\\config.properties"));
            p.load(in);
			String mtlIP = p.getProperty("mtl"); 
			String lvlIP = p.getProperty("lvl");
			String ddoIP = p.getProperty("ddo"); 
			int udpServerPort = Integer.parseInt(p.getProperty("udpServerPort"));
			int udpClientPort = Integer.parseInt(p.getProperty("udpClientPort"));
			int rmiPort = Integer.parseInt(p.getProperty("rmi"));            
            in.close();
			
            LogTool log = new LogTool(serverLocation+".log");
			ManagerOperations op=new ManagerOperationsImpl(mtlIP,lvlIP,ddoIP,udpServerPort,udpClientPort,serverLocation,log);
            LocateRegistry.createRegistry(rmiPort);
            String URL=String.format("rmi://localhost:%d/ManagerOperations", rmiPort);
            Naming.rebind(URL, op);
            
            Thread t=new RequestThread(udpServerPort);
            t.start();
            log.logMsg(String.format("sever %s start!", serverLocation),true,"server."+serverLocation,"main");
            
		}catch(Exception e){
			e.printStackTrace();
		}

	}

}
