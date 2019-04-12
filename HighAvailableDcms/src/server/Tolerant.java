package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;

import common.LogTool;

import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * this class all procedure related to Software Failure Tolerant,
 * such as FIFO broadcast replication, server failure detection,  
 * leader election etc.
 * 
 */
public class Tolerant {	
	private List<String> requestBuffer = new CopyOnWriteArrayList<String>(); //Format:"operatorID,senderID;request"leaderID=mtl1,mtl2 ...(same as config.peoperties)
	public boolean serverGroupReady = false; //after all server in group started(addRequesttoBuffer(): when first request come in), FailureDetectionThread begin works

	public LogTool log;
	public HashMap<String,String> config = new HashMap<String,String>(); //contains serverID = URL in config.properties (mtl1=localhost:8181 ...)
	public List<String> serverGroup = new CopyOnWriteArrayList<String>(); //contains serverID already started and alive (mtl1,mtl2,...),using thread safe CopyOnWriteArrayList (avoid remove while iterator error)
	
	public String curLeader=""; //current leaderID
	public HighAvailableDcmsOperationsImpl op;//for get mtlLeaderID,lvlLeaderID,ddoLeaderID,myServer etc.
	
	public Tolerant(HighAvailableDcmsOperationsImpl op,String serverLocation,LogTool log){
		this.log = log;
		this.op = op; 

		try{
    		Properties p=new Properties();
            InputStream in=new FileInputStream(new File(System.getProperty("user.dir")+File.separator+"config.properties"));
            p.load(in);
            config.put("FE", p.getProperty("FE")); 
            config.put("mtl1", p.getProperty("mtl1")); 
            config.put("mtl2", p.getProperty("mtl2"));
            config.put("mtl3", p.getProperty("mtl3"));
            config.put("lvl1", p.getProperty("lvl1")); 
            config.put("lvl2", p.getProperty("lvl2"));
            config.put("lvl3", p.getProperty("lvl3"));
            config.put("ddo1", p.getProperty("ddo1")); 
            config.put("ddo2", p.getProperty("ddo2"));
            config.put("ddo3", p.getProperty("ddo3"));
            
            switch (serverLocation.toLowerCase().substring(0,3)){
            case "mtl":
              serverGroup.add("mtl1"); 
              serverGroup.add("mtl2");
              serverGroup.add("mtl3");
			  break;			
            case "lvl":
                serverGroup.add("lvl1"); 
                serverGroup.add("lvl2");
                serverGroup.add("lvl3");
  			  break;			
            case "ddo":
                serverGroup.add("ddo1"); 
                serverGroup.add("ddo2");
                serverGroup.add("ddo3");
  			  break;		
            }
		}catch(Exception e){
			e.printStackTrace();
		}
		
		//begin monitor request buffer, if there a request added in buffer, then broadcast it to server in group(except leader)
		Thread t1=new FIFOBroadcastThread(this);
        t1.start();//ready to do FIFO Broadcast		
        
        //begin to detect the failure of the server in group(after all server in group started: call checkServerGroupReady())
        Thread t2=new FailureDetectionThread(this);
        t2.start();//ready to do FIFO Broadcast
	}
	
	 /**
	  * this method is used for translate string(addr) into ip address
	  * @param addr
	  * @return
	  * @throws UnknownHostException
	  */
	
	 public InetAddress getInetAddress(String addr) throws UnknownHostException{ //addr: hostname or IP address
	    	InetAddress result;
	    	int idx1 = addr.indexOf(".");
	    	int idx2 = addr.lastIndexOf(".");
	    	if (idx1>0&&idx2>idx1){ //ip
	    	        String[] ipStr = addr.split("\\."); // use "." to split addr string 
	    	        byte[] ipBuf = new byte[4];  
	    	        for(int i = 0; i < 4; i++){  
	    	            ipBuf[i] = (byte)(Integer.parseInt(ipStr[i])&0xff);  
	    	        }  
	    	    result = InetAddress.getByAddress(ipBuf); // change into ip address
	    	}
	    	else //host name
	    	    result = InetAddress.getByName(addr);
	    	return result;
	}
	 /**
	  * broadcast request to backup servers in group for replication.
	  * this is reliable communication, because we send one request in FIFO buffer
	  * each time and wait for ACK reply.
	  * 
	  * @param senderID
	  * @param serverID
	  * @param request
	  * @return
	 */
	public String sendBroadcastUDP(String senderID, String serverID, String request){
	    	String serverURL = config.get(serverID);
	    	String ip=serverURL.split("\\:")[0];
	    	int port=Integer.parseInt(serverURL.split("\\:")[1]);

	    	String result="";
			byte[] receiveBuf=new byte[1024];
	    	DatagramPacket dpReceive=new DatagramPacket(receiveBuf,receiveBuf.length);
	    	
	    	try{
	    		DatagramSocket socket=new DatagramSocket();
	            socket.setSoTimeout(500);
	            byte[] data=request.getBytes();
	            InetAddress addr = getInetAddress(ip);
	            DatagramPacket dpSend=new DatagramPacket(data,data.length,addr,port);
	            socket.send(dpSend);
	            socket.receive(dpReceive);//wait reply for ACK
	            result=new String(dpReceive.getData(),0,dpReceive.getLength());
	            socket.close();
	          }catch(Exception e){
	        	  return log.logMsg("(NO ACK) "+senderID+" report: "+serverID+" is dead!",true,senderID+".broadcast",serverID);
	        }
			return log.logMsg(result,true,senderID+".broadcast",serverID);//ACK information
	}	
	    	
	/**
	 * send detection UDP request to other servers in group and wait for response  
	 * to see whether they are alive
	 */
	public boolean sendDetectionUDP(String senderID, String serverID){
		boolean result = false;
		
		//command format: operator,senderID;request data		
		String request =String.format("failureDetection,%s;alive?", senderID);
    	String serverURL = config.get(serverID);
    	String ip=serverURL.split("\\:")[0];
    	int port=Integer.parseInt(serverURL.split("\\:")[1]);

		byte[] receiveBuf=new byte[1024];
    	DatagramPacket dpReceive=new DatagramPacket(receiveBuf,receiveBuf.length);
    	
    	try{
    		DatagramSocket socket=new DatagramSocket();
            socket.setSoTimeout(500);
            byte[] data=request.getBytes();
            InetAddress addr = getInetAddress(ip);
            DatagramPacket dpSend=new DatagramPacket(data,data.length,addr,port);
            socket.send(dpSend);
            socket.receive(dpReceive);
            String response=new String(dpReceive.getData(),0,dpReceive.getLength());
            result = response.indexOf("i am alive") >= 0;
            socket.close();
          }catch(Exception e){
        	  log.logMsg("[Failure Detection] "+senderID+" report: "+serverID+" is dead!",true,senderID+".detection",serverID);        	  
        }
		return result;
}	
//-----------------------------------------------
	/**
	 * add come in request from front end to FIFO buffer, so that broadcast this request to backup servers
	 * by FIFO order
	 *  
	 * @param fifo
	 * @param operate
	 * @param leadID
	 * @param request
	 */
	public void addRequesttoBuffer(boolean fifo, String operate,String leadID,String request){
		//command format: operator,senderID;request data
		if (fifo) //append to the end of buffer
			requestBuffer.add(operate+","+leadID+";"+request);
		else //insert to the first of buffer(broadcast first)
			requestBuffer.add(0, operate+","+leadID+";"+request);
	}

	public String getFirstRequestInBuffer(){
		String result = "";
		if (!requestBuffer.isEmpty())
		  result = requestBuffer.get(0);
		return result;
	}
	
	public void removeFirstRequestInBuffer(){
		if (!isBufferEmpty())
		  requestBuffer.remove(0);
	}
		
	public boolean isBufferEmpty(){
		return requestBuffer.isEmpty();
	}

	//---------------------------------------------
	/**
	 * this procedure to select a new leader server after the current leader is failed.
	 * election is based on the bully algorithm.
	 */
	
	public void  doLeaderElection(){		
		boolean responsed = false;
		String myProcessID = op.myServer;		
		for (int i = 0; i< serverGroup.size();i++ ){ //serverGroup contains all server currently may be alive
			String processID = serverGroup.get(i);
			if (processID.compareTo(myProcessID) >= 0)
				continue;
			//send election message to all server which has large id than me
			responsed = sendDetectionUDP(myProcessID, processID);
			if(responsed)//processID take over the election, I stop the election process.
				break;
		}
		
		//no any other server response to election message,so I was elected as new leader
		if(!responsed){
			curLeader = myProcessID;
			notifyFrontEndNewLeader();//tell front end the leader, then front end broadcast new leader to all servers  
		}		
	}

	/**
	 * send new leader to front end
	 */
	public void  notifyFrontEndNewLeader(){
		final int FRONT_END_PORT = 6000;//port for update to new leaded  		
		  
		String prefix = op.myServer.substring(0, 3);
		
		//command format: prefix:leader		
		String request =String.format("%s:%s", prefix,curLeader);
    	String serverURL = config.get("FE");
    	String ip=serverURL.split("\\:")[0];
    	int port=FRONT_END_PORT;

    	try{
    		DatagramSocket socket=new DatagramSocket();
            byte[] data=request.getBytes();
            InetAddress addr = getInetAddress(ip);
            DatagramPacket dpSend=new DatagramPacket(data,data.length,addr,port);
            socket.send(dpSend);
            socket.close();
          }catch(Exception e){
        }		
	}	
}

/**
 * automatically do FIFO Broadcast to backup servers 
 * when requestBuffer is not empty to complete replication.
 *
 */
class FIFOBroadcastThread extends Thread{
	Tolerant tolerant;
	public FIFOBroadcastThread(Tolerant tolerant){
		this.tolerant = tolerant;		
	}
	
	@Override
    public void run(){		
		 Timer timer = new Timer();         
         timer.schedule(new TimerTask(){ //Timer: avoid while (true) make CPU work overloaded
             @Override
             public void run() {
     			while (true){
    				if (tolerant.isBufferEmpty())
    					break;
    				String fifoRequest = tolerant.getFirstRequestInBuffer();
    				for(String serverID:tolerant.serverGroup){
    					  String senderID = fifoRequest.substring(fifoRequest.indexOf(",")+1,fifoRequest.indexOf(";"));
    					  if(serverID.equals(senderID)) //do not broadcast request to leader again
    						  continue;
    					  //broadcast request to value (ip:port)
    					  tolerant.sendBroadcastUDP(senderID,serverID, fifoRequest);
    				}
    				tolerant.removeFirstRequestInBuffer(); //fifoRequest finished, remove it from buffer 						
    			}
             }
         }, 0,2000);//time interval(if replication time is more than time interval, then real interval is replication time)		
	}
}


/**
 * automatically detected the failure of server in group
 *
 */
class FailureDetectionThread extends Thread{
	Tolerant tolerant;
	public FailureDetectionThread(Tolerant tolerant){
		this.tolerant = tolerant;		
	}
	
	@Override
    public void run(){
		Timer timer = new Timer();         
        timer.schedule(new TimerTask(){ //Timer: avoid while (true) make CPU work overloaded
            @Override
            public void run() {
    			if (!tolerant.serverGroupReady) //some server in group seem not started, wait
    				return;
    			//begin Failure Detection   
    			String senderID = tolerant.op.myServer;
				for(String serverID:tolerant.serverGroup){
					  if(serverID.equals(senderID)) //do not send UDP to myself
						  continue;
					  //senderID send a UDP request to serverID to test whether serverID is alive
					  if(!tolerant.sendDetectionUDP(senderID,serverID)){
						  //tolerant.broadcastFailureServer(serverID);//delete dead server is from group of all server(avoid broadcast to it)
						  tolerant.serverGroup.remove(serverID);//delete dead server is from group(all server can detect who is dead, so broadcast does not needed)
			        	  if(serverID.equals(tolerant.curLeader)){ //leader dead, then election a new leader
			        		  tolerant.doLeaderElection();   
			        		  break;
			        	  }
			        	  
					  }
				}
            }
        }, 0,3000);//time interval(if Failure Detection time is more than time interval, then real interval is Failure Detection time)				
	}
}	
