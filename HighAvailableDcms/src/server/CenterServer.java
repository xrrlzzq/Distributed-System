package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Properties;
import common.LogTool;

class RequestThread extends Thread{
	DatagramSocket socket=null;
	HighAvailableDcmsOperationsImpl op;
    public RequestThread(int port,HighAvailableDcmsOperationsImpl op) throws SocketException{
    	super();//inherited 
    	socket = new DatagramSocket(port);//port of this computer for receive data by UDP 
    	this.op=op;
    }
    
    @Override
    public void run(){
    	byte[] receiveBuf = new byte[1024];		
        DatagramPacket dpReceive = new DatagramPacket(receiveBuf, receiveBuf.length);
        int destPort;
        
		while(true)
		try{	 
	        socket.receive(dpReceive);//blocks(waiting) until a message come in
	        InetAddress destAddr = dpReceive.getAddress();
	        destPort = dpReceive.getPort();		
	       		       
	        String requestInfo=new String(dpReceive.getData(), 0, dpReceive.getLength());
	        
	        Thread t=new ResponseThread(destAddr,destPort,requestInfo,op);
            t.start();
            dpReceive.setLength(receiveBuf.length);//for receive next packet
		}catch(Exception e)
		 {
			e.printStackTrace();
			continue;
		 }	            	        
    }
}    

class ResponseThread extends Thread{	
	public InetAddress destAddr;
	public int destPort;
	public String requestInfo;
	HighAvailableDcmsOperationsImpl op;
    public ResponseThread(InetAddress destAddr,int destPort,String requestInfo,HighAvailableDcmsOperationsImpl op) throws SocketException{
    	super();//inherited 
    	this.op=op;
    	this.destAddr = destAddr;
    	this.destPort = destPort;
    	this.requestInfo = requestInfo;
    }
    
    @Override
    public void run(){
		try{	 
	        String requestData="";
	        String operate=requestInfo.split("\\,")[0];
	        boolean needAddToFIFOBuffer=true;
	        String info="unknow request command!";
           if(requestInfo.indexOf("getRecordCounts")!=-1){
	        	
	        	info=String.format("%d",op.getMyCounts());	
	        }
	       
	        else
	        {
	        	//command format: operator,senderID;request data
	        	requestData = requestInfo.split("\\;")[1]; //this is database data

		        switch(operate){
		        case "creatTR":
		        	info=doCreatTR(requestData);
		        	//make sure the recordId of backup server is the same as leader when create take place simultaneously
		        	needAddToFIFOBuffer = false;//already put addRequesttoBuffer to createTRecord() synchronized{} block
		        	break;
		        case "creatSR":
		        	info=doCreatSR(requestData);
		        	//make sure the recordId of backup server is the same as leader when create take place simultaneously
		        	needAddToFIFOBuffer = false;//already put addRequesttoBuffer to createSRecord() synchronized{} block
		        	break;
		        case "editRecord":
		        	info=doEditRecord(requestData);
		        	//make sure edit the same recordId of backup server as leader when edit take place simultaneously
		        	needAddToFIFOBuffer = false;//already put addRequesttoBuffer to editRecord() synchronized{} block
		        	break;
		        case "getCounts":
		        	info=doGetCounts(requestData);break;
		        case "transferSend":
		        	if(op.myServer.equals(op.tolerant.curLeader)){
		        		info=doTransferSend(requestData);
		        		//only add to FIFO buffer when transfer to leader successfully
		        		needAddToFIFOBuffer=(info.indexOf("successfully")!=-1);
		        	}
		        	else
		        		info=doRemoveOnly(requestData);
		        	break;
		        case "transferReceive":
		        	info=op.doTransferedReceive(requestData);
		        	break;
		        case "notifyLeaderID"://front end send notifyLeaderID to three leader, leader broadcast notifyLeaderID to backup server(by call addRequesttoBuffer), so all server updated.
		        	info=doNotifyLeaderID(requestData);
		        	break;		        	
		        case "failureDetection": 
		        	info=dofailureDetection(requestData);break;//return alive message(for failure server, request can not be responsed ,so this code can not be executed)
		        
		        }	        	
	        }

           if(!op.myServer.equals(op.tolerant.curLeader))
        	   info = String.format("(ACK)[FIFO boradcast to %s] %s", op.myServer,info) ;
            //return to front end
            byte[] data = info.getBytes();	        
			DatagramPacket dpSend = new DatagramPacket(data,data.length, destAddr, destPort);//port of other computer to send data by UDP
	    	
			DatagramSocket socket = new DatagramSocket(); 			
			socket.send(dpSend); //send response to front end, then to client
			socket.close();
			//add request from front end to FIFO buffer and then broadcast to backup server for replication.
        	//the server which the front end connected is leader
        	if(needAddToFIFOBuffer&&requestInfo.indexOf(",frontend;")!=-1){//only request come from front end need FIFO broadcast(skip the request from leader broadcast, this avoid dead loop)	        	
        		if(operate.equals("notifyLeaderID")) //broadcast to backup servers prior to any requests in FIFO buffer
        		    op.tolerant.addRequesttoBuffer(false,operate,op.myServer,requestData);//insert request to the first of FIFO buffer
        		else
        			op.tolerant.addRequesttoBuffer(true,operate,op.myServer,requestData);//add request to FIFO buffer
        	}  
        				
		}catch(Exception e)
		 {
			e.printStackTrace();
		 }					        	
    }
    /**
     * update the three leaderID and make server begin failure detection(serverGroupReady = true)
     * 
     * @param request
     * @return
     */
    public String doNotifyLeaderID(String request){
    	String[] fieldName=request.split("\\|");
    	
        op.mtlLeaderID = fieldName[0];
        op.lvlLeaderID = fieldName[1];
        op.ddoLeaderID = fieldName[2];
        
        String prefix = op.myServer.substring(0, 3);
        switch(prefix){
    	case "mtl":
    		op.tolerant.curLeader = op.mtlLeaderID;break;
    	case "lvl":
    		op.tolerant.curLeader = op.lvlLeaderID;break;
    	case "ddo":
    		op.tolerant.curLeader = op.ddoLeaderID;break;
    	}        
    	
        op.tolerant.serverGroupReady = true;//start failure detection
        
        String msg= String.format("server %s leader updated: remote center leader(mtl,ddo,lvl) = (%s,%s,%s)", op.myServer,op.mtlLeaderID,op.lvlLeaderID,op.ddoLeaderID); 
        op.tolerant.log.logMsg(msg,true,op.tolerant.curLeader+".broadcast",op.myServer);   
        
        return msg;
    }
    
    public String doCreatTR(String request){
    	
    	String[] fieldName=request.split("\\|");
    	
    	return op.createTRecord(fieldName[0], fieldName[1], fieldName[2], fieldName[3], fieldName[4], fieldName[5], fieldName[6]) ;
    }
    public String doCreatSR(String request){
    	String[] fieldName=request.split("\\|");   	
    	return op.createSRecord(fieldName[0], fieldName[1], fieldName[2], fieldName[3], fieldName[4], fieldName[5]) ;
    }
    public String doGetCounts(String request){
    	return op.getRecordCounts(request);    
    }
    public String doEditRecord(String request){
    	String[] fieldName=request.split("\\|");    	
    	return op.editRecord(fieldName[0], fieldName[1], fieldName[2], fieldName[3]);
    }
    public String doTransferSend(String request){
    	String[] fieldName=request.split("\\|");    	
    	return op.transferRecord(fieldName[0], fieldName[1], fieldName[2]);
    }

    public String dofailureDetection(String request){
    	return "i am alive!";
    }
    public String doRemoveOnly(String request){
    	
    	String[] fieldName=request.split("\\|");
    	op.removeData(fieldName[1]);
    	return op.tolerant.log.logMsg(String.format("(transfer by "+fieldName[0]+")transfer record[%s] in replica successfully!", fieldName[1]),true, "server."+op.myServer,"transferRecord");
    }
   
}    

public class CenterServer { 
	public static void main(String[] args) {
		if(args.length <= 0){
		      System.out.println("usage: java server.CenterServer serverID");
		      System.out.println("serverID: mtl1,mtl2,mtl3,lvl1,lvl2,lvl3,ddo1,ddo2,ddo3");
		      return;
		}
		
		String identifier = args[0].toLowerCase();		
		
		try{
    		Properties p=new Properties();
            InputStream in=new FileInputStream(new File(System.getProperty("user.dir")+File.separator+"config.properties"));
            p.load(in);
			int listenPort=Integer.parseInt(p.getProperty(identifier).split("\\:")[1]);
			
            LogTool log = new LogTool(identifier+".log");
            HighAvailableDcmsOperationsImpl op=new HighAvailableDcmsOperationsImpl(identifier,log);
	        
	        Thread t=new RequestThread(listenPort,op);
            t.start();
            
            log.logMsg(String.format("sever %s start!", identifier),true,"server."+identifier,"main");

		}catch(Exception e){
			e.printStackTrace();
		}

	}

}
