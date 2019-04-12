package middleware;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Properties;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import common.HighAvailableDcmsOperations;
import common.HighAvailableDcmsOperationsHelper;
import common.HighAvailableDcmsOperationsPOA;
import common.LogTool;

class UpdateLeaderThread extends Thread{
	private final int FRONT_END_PORT = 6000;//listen for update to new leaded  
	private DatagramSocket socket=null;
	private CorbaOperations op;
	private LogTool log;
	
    public UpdateLeaderThread(CorbaOperations op,LogTool log) throws SocketException{
    	super();//inherited 
    	socket = new DatagramSocket(FRONT_END_PORT);//port of this computer for receive data by UDP
    	this.op = op;
    	this.log = log;
    }
    
    @Override
    public void run(){
    	byte[] receiveBuf = new byte[1024];		
        DatagramPacket dpReceive = new DatagramPacket(receiveBuf, receiveBuf.length);
        
		while(true)
		try{	 
	        socket.receive(dpReceive);//blocks(waiting) until a message come in	       		       
	        String requestInfo=new String(dpReceive.getData(), 0, dpReceive.getLength());

	        String prefix=requestInfo.split("\\:")[0];
	        String leaderID = requestInfo.split("\\:")[1];	        
	        dpReceive.setLength(receiveBuf.length);//for receive next packet

	        String msg="";	        
	    	switch(prefix){
	    	case "mtl":
	    		msg = String.format("Old leader %s is dead, new leader %s is elected!", op.mtlLeaderID,leaderID);
	    		op.mtlLeaderID = leaderID;
	    		break;
	    	case "lvl":
	    		msg = String.format("Old leader %s is dead, new leader %s is elected!", op.lvlLeaderID,leaderID);
	    		op.lvlLeaderID = leaderID;break;
	    	case "ddo":
	    		msg = String.format("Old leader %s is dead, new leader %s is elected!", op.ddoLeaderID,leaderID);
	    		op.ddoLeaderID = leaderID;break;
	    	}	        
	    	op.notifyLeaderID();//notify all server the new leaderID
	    	
	    	log.logMsg(msg,true,"frontend.update","leader");	        	
	        
		}catch(Exception e)
		 {
			e.printStackTrace();
			continue;
		 }					        	
    }    
}

public class FrontEnd  {
   
	public static void main(String[] args) {
		try{
    		Properties p=new Properties();
            InputStream in=new FileInputStream(new File(System.getProperty("user.dir")+File.separator+"config.properties"));
            p.load(in);
			
			String[] corba = new String[2];
			corba[0]="-ORBInitialPort";
			corba[1]=p.getProperty("FE").split("\\:")[1]; 
            in.close();
            ORB orb=ORB.init(corba, null);
    		org.omg.CORBA.Object obj=orb.resolve_initial_references("RootPOA");  
            POA rootpoa = POAHelper.narrow(obj);  
            rootpoa.the_POAManager().activate();
            LogTool log = new LogTool("frontend.log");
            CorbaOperations op=new CorbaOperations(log);

            op.config.put("mtl1", p.getProperty("mtl1")); 
            op.config.put("mtl2", p.getProperty("mtl2"));
            op.config.put("mtl3", p.getProperty("mtl3"));
            op.config.put("lvl1", p.getProperty("lvl1")); 
            op.config.put("lvl2", p.getProperty("lvl2"));
            op.config.put("lvl3", p.getProperty("lvl3"));
            op.config.put("ddo1", p.getProperty("ddo1")); 
            op.config.put("ddo2", p.getProperty("ddo2"));
            op.config.put("ddo3", p.getProperty("ddo3"));
            
            org.omg.CORBA.Object ref = rootpoa.servant_to_reference(op);  
			HighAvailableDcmsOperations href = HighAvailableDcmsOperationsHelper.narrow(ref);
	        
	        org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");  
	        NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
	        
	        String name = "ManagerOperations";  
	        NameComponent path[] = ncRef.to_name(name);  
	        ncRef.rebind(path, href);             
	        
	        Thread t=new UpdateLeaderThread(op,log);
            t.start();
	        
	        log.logMsg(String.format("Front end start!"),true,"frontend","main");;

	        orb.run();  
            
		}catch(Exception e){
			e.printStackTrace();
		}
	}	
}

class CorbaOperations extends HighAvailableDcmsOperationsPOA {
	private static String ip;
	private static int port;
	private static LogTool log;

	public String mtlLeaderID;
	public String lvlLeaderID;
	public String ddoLeaderID;
	public HashMap<String,String> config = new HashMap<String,String>(); //contains serverID = URL in config.properties (mtl1=localhost:8181 ...)
	public boolean readyWork = false; //system begin work

	public CorbaOperations(LogTool log){
	   	mtlLeaderID="mtl1"; //default leader id at start
	   	lvlLeaderID="lvl1";
	   	ddoLeaderID="ddo1";
	  	CorbaOperations.log=log;	        
	}
	
	/**
	 * convert IP or host string to InetAddress type
	 * @param addr
	 * @return
	 * @throws UnknownHostException
	 */
	public InetAddress getInetAddress(String addr) throws UnknownHostException{ //addr: hostname or IP address
    	InetAddress result;
    	int idx1 = addr.indexOf(".");
    	int idx2 = addr.lastIndexOf(".");
    	if (idx1>0&&idx2>idx1){ //ip
    	        String[] ipStr = addr.split("\\.");  
    	        byte[] ipBuf = new byte[4];  
    	        for(int i = 0; i < 4; i++){  
    	            ipBuf[i] = (byte)(Integer.parseInt(ipStr[i])&0xff);  
    	        }  
    	    result = InetAddress.getByAddress(ipBuf);
    	}
    	else //host name
    	    result = InetAddress.getByName(addr);
    	return result;
    }
	/**
	 * setup UDP IP and PORT according to leaderID before communication
	 * @param prefix
	 */
    public void setAdress(String prefix){
	    String mtlLeaderURL = config.get(mtlLeaderID);
	    String lvlLeaderURL = config.get(lvlLeaderID);
	    String ddoLeaderURL = config.get(ddoLeaderID);
    	
    	switch(prefix){
    	case "mtl":
    		ip=mtlLeaderURL.split("\\:")[0];port=Integer.parseInt(mtlLeaderURL.split("\\:")[1]);break;// to get ip and port of mtl leader server
    	case "lvl":
    		ip=lvlLeaderURL.split("\\:")[0];port=Integer.parseInt(lvlLeaderURL.split("\\:")[1]);break;// to get ip and port of lvl leader server
    	case "ddo":
    		ip=ddoLeaderURL.split("\\:")[0];port=Integer.parseInt(ddoLeaderURL.split("\\:")[1]);break;// to get ip and port of ddo leader server
    	}
    }
    
    /**
     * send request to leader server according to prefix of managerID.
     * leader may failed just before send, so we use a loop to wait
     * new leader election and try again 
     * 
     * 
     * @param prefix
     * @param request
     * @return
     */
    public String sendUDP(String prefix,String request){
    	if(!readyWork){ //execute once only
    		notifyLeaderID(); //notify all servers of three leaderIDs and begin to do failure failure detection each other
    		readyWork = true;
    	}
    	//-------------------------------------------
    	String result="";
		byte[] receiveBuf=new byte[1024];
    	DatagramPacket dpReceive=new DatagramPacket(receiveBuf,receiveBuf.length);
        byte[] data=request.getBytes();

        DatagramSocket socket=null;
    	try {
    	    socket=new DatagramSocket();//each time use a new socket to support multiple client request call at the same time
	        socket.setSoTimeout(500);
		} catch (SocketException e) {
			e.printStackTrace();  
		}        	    	
        
        while(true){ //when leader failed, using new leader to try again
            setAdress(prefix);//set leader ip and port
        	try{
                InetAddress addr = getInetAddress(ip);
                DatagramPacket dpSend=new DatagramPacket(data,data.length,addr,port);
                socket.send(dpSend);
                socket.receive(dpReceive);
                result=new String(dpReceive.getData(),0,dpReceive.getLength());
                dpReceive.setLength(receiveBuf.length);//for receive next packet  
                break;
              }catch(Exception e){
            	  continue;//wait new leadID updated by server
              }        	
        }
        socket.close();
		return log.logMsg(result,true,"frontend","main");
    }
    
    /**
     * send three leaderIDs to all three leader servers.
     * leader server then broadcast to its backup servers.
     * when front end send first command, it also do notifyLeaderID()
     * and thus all servers will begin detection each other(make all serverGroupReady = true).
     */
    public void notifyLeaderID(){
        DatagramSocket socket=null;
    	try {
    	    socket=new DatagramSocket();//each time use a new socket to support multiple client request call at the same time
	        socket.setSoTimeout(500);
		} catch (SocketException e1) {
		}        	    	
        
    	String request = String.format("notifyLeaderID,frontend;%s|%s|%s", mtlLeaderID,lvlLeaderID,ddoLeaderID);
        byte[] data=request.getBytes();
        String[] groupID = {"mtl","lvl","ddo"};

        for(int i = 0; i<3;i++){
            setAdress(groupID[i]);//set leader ip and port
        	try{
                InetAddress addr = getInetAddress(ip);
                DatagramPacket dpSend=new DatagramPacket(data,data.length,addr,port);
                socket.send(dpSend);
              }catch(Exception e){
            	  e.printStackTrace();  
              }        	        	
        }    	
        socket.close();
    }
    
	@Override
	public String createTRecord(String managerID, String firstName, String lastName, String address, String phone,
			String specialization, String location) {//frontend token: indicate server the request comes from front end(nor server FIFO broadcast)

		//command format: operator,senderID;request data
		String request=String.format("creatTR,frontend;%s|%s|%s|%s|%s|%s|%s", managerID,firstName,lastName,address,phone,specialization,location);
		String prefix=managerID.toLowerCase().substring(0, 3);
		return sendUDP(prefix,request);
	}

	@Override
	public String createSRecord(String managerID, String firstName, String lastName, String courseRegistered,
			String status, String statusDate) {
		String request=String.format("creatSR,frontend;%s|%s|%s|%s|%s|%s", managerID,firstName,lastName,courseRegistered,status,statusDate);
		String prefix=managerID.toLowerCase().substring(0, 3);
		return sendUDP(prefix,request);
	}

	@Override
	public String getRecordCounts(String managerID) {
		String request=String.format("getCounts,frontend;%s", managerID);
		String prefix=managerID.toLowerCase().substring(0, 3);
		return sendUDP(prefix,request);
	}

	@Override
	public String editRecord(String managerID, String recordID, String fieldName, String newValue) {
		String request=String.format("editRecord,frontend;%s|%s|%s|%s", managerID,recordID,fieldName,newValue);
		String prefix=managerID.toLowerCase().substring(0, 3);
		return sendUDP(prefix,request);
	}

	@Override
	public String transferRecord(String managerID, String recordID, String remoteCenterServerName) {
		String request=String.format("transferSend,frontend;%s|%s|%s", managerID,recordID,remoteCenterServerName);
		String prefix=managerID.toLowerCase().substring(0, 3);
		return sendUDP(prefix,request);
	}
}
