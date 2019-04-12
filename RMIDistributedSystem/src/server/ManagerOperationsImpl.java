package server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;

import java.rmi.server.UnicastRemoteObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class ManagerOperationsImpl extends UnicastRemoteObject implements ManagerOperations {
	private static final long serialVersionUID = 9206245996460676359L;
	private HashMap<Character,HashMap<String,HashMap<String,String>>> records=new HashMap<Character,HashMap<String,HashMap<String,String>>>();
	private static HashMap<String,Character> indexSR= new HashMap<String,Character>();
	private static HashMap<String,Character> indexTR= new HashMap<String,Character>();
	private String locVaildSet=";mtl;lvl;ddo;";
    private String courseVaildSet=";french;maths;science;";
    private String trVaildSet=";firstname;lastname;address;phone;specialization;location;";
    private String srVaildSet=";firstname;lastname;courseregistered;status;statusdate;";
    private String reVaildSet=";TR;SR;";

    //configure file information
	//serverLocation is which server of this server running(MTL, LVL,DDO)
	private String mtlIP;
	private String lvlIP;
	private String ddoIP;
	private int udpServerPort;//port UDPService listening 
	//private int udpClientPort;//port getUDPRecordCount() to receive RecordCount
	private String myServer;//mtl,lvl,ddo
	private LogTool log;
    private DatagramSocket socket;
	protected ManagerOperationsImpl(String mtlIP,String lvlIP,String ddoIP,int udpServerPort,int udpClientPort,String serverLocation,LogTool log) throws RemoteException, SocketException {
		super();
		this.mtlIP = mtlIP; 
		this.lvlIP = lvlIP; 
		this.ddoIP = ddoIP; 
		this.udpServerPort = udpServerPort;
		//this.udpClientPort = udpClientPort;
		this.myServer = serverLocation.toLowerCase();
		this.log = log;
		socket=new DatagramSocket(udpClientPort);
	}

    public static boolean isValidDate(String dateStr) {
        boolean result=true;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            //set lenient to false, otherwise SimpleDateFormat to check date loosely.for example 02/29/2018 can be accept and convert to 03/01/2018
            format.setLenient(false);
            format.parse(dateStr);
        } catch (ParseException e) {
            result=false;
        } 
        return result;
    }
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
   /* public String createUniqueID(String id){
    	String result="";
    	switch(myServer){
    	case "mtl":result=String.format("%s%05d", id,getUDPCounts(lvlIP,id)+getUDPCounts(ddoIP,id)+getMyCounts(id)+1);break;
    	case "lvl":result=String.format("%s%05d", id,getUDPCounts(mtlIP,id)+getUDPCounts(ddoIP,id)+getMyCounts(id)+1);break;
    	case "ddo":result=String.format("%s%05d", id,getUDPCounts(lvlIP,id)+getUDPCounts(mtlIP,id)+getMyCounts(id)+1);break;
    	}
    	return result;
    }*/
	@Override
	public String createTRecord(String firstName, String lastName, String address, String phone, String specialization,
			String location) throws RemoteException {
		String opreation=String.format("client of %s want to create teacher record(%s, %s, %s, %s, %s, %s)", myServer,
				firstName, lastName, address,  phone, specialization,
				location);
		log.logMsg(opreation, true, "server."+myServer,"createTRecord");
		String locToken=";"+location.toLowerCase()+";";
		String msg="";
	    if(locVaildSet.indexOf(locToken)==-1)
	    	return log.logMsg("invaild location,please enter again(mtl,lvl,ddo)!", false,"server."+myServer,"createTRecord");
	    else if(lastName.equals(""))
			return log.logMsg("The lastname can not be empty!", false,"server."+myServer,"createTRecord");
	    else if(firstName.equals(""))
			return log.logMsg("The firstname can not be empty!", false,"server."+myServer,"createTRecord");
	    else if(!specialization.equals("french")&&
	    		!specialization.equals("maths")&&!specialization.equals("science")) //for teach may be not so (e.g. french, maths, etc)
	    	return log.logMsg("invaild specialization,please again(french,maths,science)!", false,"server."+myServer,"createTRecord");
	    else{
			Object lock = new Object();
			synchronized(lock){ //make thread safe when update HashMap data
				char initial=lastName.toUpperCase().charAt(0);
			    //int id=indexTR.size()+1;
			    String recordId=String.format("TR%05d", indexTR.size()+1);
			    HashMap<String,String> record=new HashMap<String,String>();
			    record.put("firstname", firstName);
			    record.put("lastname", lastName);
			    record.put("address", address);
			    record.put("phone", phone);
			    record.put("specialization", specialization);
			    record.put("location", location);
			    //record.put("recordid", recordId);//not need
			    if(records.containsKey(initial))
			    records.get(initial).put(recordId, record);
			    else{
			    	HashMap<String,HashMap<String,String>> rec=new HashMap<String,HashMap<String,String>>();
			    	rec.put(recordId,record);
			    	records.put(initial, rec);
			    }
			    indexTR.put(recordId, initial);
			    msg=String.format("create teacher record in %s:(%s,%s,%s,%s,%s,%s,%s) successfully",myServer,myServer, recordId,
						firstName,lastName,address,phone,specialization,location);
			}
			
	    	return log.logMsg(msg, true,"server."+myServer,"createTRecord");
	    }
	}

	@Override
	public String createSRecord(String firstName, String lastName, String courseRegistered, String status,
			String statusDate) throws RemoteException {
		String opreation=String.format("client of %s want to create student record(%s, %s, %s, %s, %s)", myServer,
				firstName, lastName, courseRegistered, status, statusDate);
		log.logMsg(opreation, true, "server."+myServer,"createSRecord");
		    String msg="";
		    if(lastName.equals(""))
		       return log.logMsg("The lastname can not be empty!", false,"server."+myServer,"createSRecord");
		    if(firstName.equals(""))
				return log.logMsg("The firstname can not be empty!", false,"server."+myServer,"createSRecord");
		    String[] courseCheck=courseRegistered.toLowerCase().split("/");
		    for(int i=0;i<courseCheck.length;i++){
		    	if(courseVaildSet.indexOf(";"+courseCheck[i]+";")==-1)
		    	    return log.logMsg("invaild course enter,please enter again(french/maths/science)", false,"server."+myServer,"createSRecord");
		    }
		    
			if (!isValidDate(statusDate)){
				return log.logMsg(String.format("value [%s] is invalid for status date(format: yyyy-MM-dd, e.g. 2018-05-27)!", statusDate), false,"server."+myServer,"createSRecord");
			}
		    
		    if(!status.equals("active")&&!status.equals("inactive"))
		        return log.logMsg("invaild status enter,please enter again(active/inactive)", false,"server."+myServer,"createSRecord");
		    else{
				Object lock = new Object();
				synchronized(lock){ //make thread safe when update HashMap data
				    char initial=lastName.toUpperCase().charAt(0);
				    //int id=indexSR.size()+1;
				    String recordId=String.format("SR%05d", indexSR.size()+1);
				    HashMap<String,String> record=new HashMap<String,String>();
				    record.put("firstname", firstName);
				    record.put("lastname", lastName);
				    record.put("courseregistered",courseRegistered);
				    record.put("status", status);
				    record.put("statusdate", statusDate);
				    //record.put("recordid", recordId);//not needed
				    if(records.containsKey(initial))
					    records.get(initial).put(recordId, record);
					    else{
					    	HashMap<String,HashMap<String,String>> rec=new HashMap<String,HashMap<String,String>>();
					    	rec.put(recordId,record);
					    	records.put(initial, rec);
					    }
				    indexSR.put(recordId, initial);	
				    msg=String.format("create student record in %s:(%s,%s,%s,%s,%s,%s) successfully",myServer, recordId,firstName,lastName,courseRegistered
				    		,status,statusDate);
				}
		    return log.logMsg(msg, true,"server."+myServer,"createSRecord");
		    }
		
	}

	@Override
	public String getRecordCounts() throws RemoteException {
		log.logMsg("client of "+myServer+" want to get the count of records", true,"server."+myServer,"getRecordCounts");
		String result="";
		switch(myServer){
		       case "mtl":
		    	   result=String.format("MTL %d,LVL %d,DDO %d", getMyCounts(),getUDPCounts(lvlIP),getUDPCounts(ddoIP));
		    	   break;
		       case "lvl":
		    	   result=String.format("MTL %d,LVL %d,DDO %d", getUDPCounts(mtlIP),getMyCounts(),getUDPCounts(ddoIP));
		    	   break;
		       case "ddo":
		    	   result=String.format("MTL %d,LVL %d,DDO %d", getUDPCounts(mtlIP),getUDPCounts(lvlIP),getMyCounts());
		    	   break;		       
		}
			
		return log.logMsg(result, true,"server."+myServer,"getRecordCounts");
	}
    public static int getMyCounts(){
    	
    	return indexTR.size()+indexSR.size();	    	    	
    }
     
    public int getUDPCounts(String ip){
    	int result=0;
    	byte[] receiveBuf=new byte[1024];
    	DatagramPacket dpReceive=new DatagramPacket(receiveBuf,receiveBuf.length);
    	try{
            
            //socket.setSoTimeout(10);
            String info="getRecordCounts";
            byte[] data=info.getBytes();
            InetAddress addr = getInetAddress(ip);
            DatagramPacket dpSend=new DatagramPacket(data,data.length,addr,udpServerPort);
            socket.send(dpSend);
            socket.receive(dpReceive);
            result=Integer.parseInt(new String(dpReceive.getData(),0,dpReceive.getLength()));
            
          }catch(Exception e){
        	 e.printStackTrace();
          }
    	return result;
    }
	@Override
	public String editRecord(String recordID, String fieldName, String newValue) throws RemoteException {
		 char index;
		 String operation=String.format("client of %s want to change record[%s]'s field[%s] into new value[%s]",
				 myServer,recordID,fieldName,newValue);
		 log.logMsg(operation, true,"server."+myServer,"editRecord");
		 String msg="";
		 String action="";
		 if((recordID.startsWith("T")&&!indexTR.containsKey(recordID))||
		    (recordID.startsWith("S")&&!indexSR.containsKey(recordID)))
			//guarantee index=indexTR.get(recordID) does not report null error at editRecord()
		    return log.logMsg(String.format("RecordID [%s] does not exists in [%s] server!", recordID,myServer), false,"server."+myServer,"editRecord");
		 
		 fieldName=fieldName.replaceAll(" ","").toLowerCase();//field name stored in hashmap("First Name" acceptable, convert to "firstname")
		 
		 if (fieldName.equals("recordid")) //check recordID key
		     return log.logMsg("Field RecordID is key, can not be changed!", false,"server."+myServer,"editRecord");
		 if(fieldName.equals("lastname"))
			 return log.logMsg("Field lastname can not be changed!", false,"server."+myServer,"editRecord");
		 if(fieldName.equals("firstname"))
			 return log.logMsg("Field firstname can not be changed!", false,"server."+myServer,"editRecord"); 
		/* if(fieldName.equals("lastname")&&newValue.equals(""))
		     return log.logMsg("The first letter of the last name is key, can not changed to empty!", false,"server."+myServer,"editRecord");*/
		 if (fieldName.equals("statusdate")&&!isValidDate(newValue)){
			 return log.logMsg(String.format("value [%s] is invalid for status date(format: yyyy-MM-dd, e.g. 2018-05-27)!", newValue), false,"server."+myServer,"editRecord");
		 }
		 if(fieldName.equals("specialization"))
			 return log.logMsg("Field specialization can not be changed!", false, "server."+myServer,"editRecord");
		 
		 recordID=recordID.toUpperCase();
		 if(!checkRecordID(recordID))
		     return log.logMsg("invaild recordID, please try again(TR or SR+5 digit)", false,"server."+myServer,"editRecord");
		 if(!checkFieldName(recordID,fieldName,newValue))
		     return log.logMsg("invaild fieldName:"+fieldName+", please try again", false,"server."+myServer,"editRecord");		 
		 if(!checkValue(fieldName,newValue))
		     return log.logMsg("invaild value:"+newValue+",please try again", false,"server."+myServer,"editRecord");
		 
		 if(recordID.startsWith("T"))
			 index=indexTR.get(recordID);
		 else
			 index=indexSR.get(recordID);
		 HashMap<String,HashMap<String,String>> eRecord=records.get(index);
		
		 if(eRecord.containsKey(recordID)){
			/* if (fieldName.equals("lastname")){ //lastname's first character is key, do special check 
				  String oldValue = eRecord.get(recordID).get(fieldName);
				  Character oldCh = oldValue.charAt(0);
				  Character newCh = newValue.charAt(0);//newValue has make sure not empty in beforeEditCheck()
				  if(newCh!=oldCh)					
				     return log.logMsg("The first letter of the last name is key, must keep the same!", false,"server."+myServer,"editRecord");
			 }*/
			 
			 Object lock = new Object();
			 synchronized(lock){ //make thread safe when update HashMap data
				 if(fieldName.equals("status")){
					 String s=eRecord.get(recordID).get("status");
					 if(!newValue.equalsIgnoreCase(s)){//status changed, update status date to current date
						 SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
						 String statusDate=df.format(new Date());
						 eRecord.get(recordID).put("statusdate", statusDate);
					 }
					 
				 }
				 eRecord.get(recordID).put(fieldName,newValue);	
				 action=String.format("Edit record(%s) successfully, the field name(%s) changed into new value(%s)", recordID,fieldName,newValue);
			 }			 
			 msg = log.logMsg(action, true,"server."+myServer,"editRecord");
		 }
		 else
			 msg= log.logMsg(String.format("RecordID [%s] does not exists!", recordID), false,"server."+myServer,"editRecord");
   		     //guarantee index=indexTR.get(recordID) does not report null error at editRecord()
		
		 return msg;
	}
	
	public boolean checkRecordID(String recordID){
		boolean result=true;
		if(recordID.length()!=7)
			return false;
		else{
			String leftCheck=";"+recordID.substring(0, 2)+";";
			String rightCheck=recordID.substring(2,7);
			if(reVaildSet.indexOf(leftCheck)==-1)
				return false;
			try{
				int num=Integer.parseInt(rightCheck);
			}catch(Exception e){
				result=false;
			}
		}
		return result;
	}
	public boolean checkFieldName(String recordID, String fieldName, String newValue){
		boolean result=true;
		String checkToken=";"+fieldName.toLowerCase()+";";
		if(recordID.startsWith("T")){
			if(trVaildSet.indexOf(checkToken)==-1)
				result=false;
		}
		 else{
			 if(srVaildSet.indexOf(checkToken)==-1)
				 result=false;
		 }
		return result; 
	}
	public boolean checkValue(String fieldName, String newValue){
		boolean result=true;
		fieldName=fieldName.toLowerCase();
		if(fieldName.equals("location")){
			String checkToken=";"+newValue+";";
			if(locVaildSet.indexOf(checkToken)==-1)
				return false;
			else
				return true;
		}
		else if(fieldName.equals("courseregistered")){
			String[] courseCheck=newValue.toLowerCase().split("/");
		    for(int i=0;i<courseCheck.length;i++){
		    	if(courseVaildSet.indexOf(";"+courseCheck[i]+";")==-1)
		    	    return false;
		    }
		    return true;
		}
		else if(fieldName.equals("specialization")){
			if(!newValue.equals("french")&&
		    		!newValue.equals("maths")&&!newValue.equals("science"))
				return false;
			else
				return true;
		}
		else if(fieldName.equals("status")){
			if(!newValue.equals("active")&&!newValue.equals("inactive"))
				return false;
			else
				return true;
		}
		else
		return result;
	}	
}
