package server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import common.LogTool;

public class HighAvailableDcmsOperationsImpl  {
	private static HashMap<Character,HashMap<String,HashMap<String,String>>> records=new HashMap<Character,HashMap<String,HashMap<String,String>>>(); // this is the database hashmap
	private static HashMap<String,Character> indexSR= new HashMap<String,Character>(); // this is the student record index(for locating the record in database) 
	private static HashMap<String,Character> indexTR= new HashMap<String,Character>(); // this is the teacher record index(for locating the record in database) 
	private String locVaildSet=";mtl;lvl;ddo;"; // this is use for check the validity of location 
    private String courseVaildSet=";french;maths;science;";// this is use for check the validity of course
    private String trVaildSet=";firstname;lastname;address;phone;specialization;location;"; // this is use for check the validity of the argument of teacher record
    private String srVaildSet=";firstname;lastname;courseregistered;status;statusdate;";// // this is use for check the validity of the argument of student record
    private String reVaildSet=";MTR;LTR;DTR;MSR;LSR;DSR;"; // this is use for check the validity of record id
    private int maxCountTR=0; 
    private int maxCountSR=0;
	
    public String mtlLeaderID;
	public String lvlLeaderID;
	public String ddoLeaderID;
    public String myServer;//who am I, mtl1,mtl2,lvl1 ...
	
    private LogTool log;
    private DatagramSocket socket;	    
    public Tolerant tolerant;
    
	/**
	 * this method is used for initialing the information from center server
	 * @param serverLocation
	 * @param log
	 */
    protected HighAvailableDcmsOperationsImpl(String serverLocation,LogTool log) throws RemoteException, SocketException {
		super();
		mtlLeaderID="mtl1"; //default leader id at start
    	lvlLeaderID="lvl1";
    	ddoLeaderID="ddo1";
		myServer = serverLocation;
		this.log = log;
		socket=new DatagramSocket();
				
        tolerant = new Tolerant(this,serverLocation,log);        
	}
	/**
	 * this method is used for checking if the format of date is correct
	 * @param dateStr
	 * 
	 */
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
	 * this method is the remote web service method which offer the service for creating a teacher record
	 */
	 
	public String createTRecord(String managerID, String firstName, String lastName, String address, String phone,
			String specialization, String location) {
		String opreation=String.format("manager [%s] want to create teacher record(%s, %s, %s, %s, %s, %s)",managerID,managerID,
				firstName, lastName, address,  phone, specialization,
				location); 
		log.logMsg(opreation, true, "server."+myServer,"createTRecord");// write the operation information to the log
		String locToken=";"+location.toLowerCase()+";"; // create the location token which use for checking the validity of location 
		String msg="";
	    if(locVaildSet.indexOf(locToken)==-1)// if the location is not mtl,lvl or ddo
	    	return log.logMsg("invaild location,please enter again(mtl,lvl,ddo)!", false,"server."+myServer,"createTRecord");
	    else if(lastName.equals("")) // lastname is not allowed to be empty
			return log.logMsg("The lastname can not be empty!", false,"server."+myServer,"createTRecord");
	    else if(firstName.equals("")) // firstname is not allowed to be empty
			return log.logMsg("The firstname can not be empty!", false,"server."+myServer,"createTRecord");
	    else if(!specialization.equals("french")&&
	    		!specialization.equals("maths")&&!specialization.equals("science")) //for specialization of teacher may be not validity (e.g. french, maths, etc)
	    	return log.logMsg("invaild specialization,please again(french,maths,science)!", false,"server."+myServer,"createTRecord");
	    else{
			
			synchronized(records){ //use database hashmap as lock to make thread safe(synchronized) when update HashMap data
				char initial=lastName.toUpperCase().charAt(0); // use the initial of lastname as database index
			    String recordId=String.format("%sTR%05d", myServer.substring(0, 1).toUpperCase(),maxCountTR+1);// create the record id according to the number of existing records(make sure there aren't duplicated records in the server )
			    HashMap<String,String> record=new HashMap<String,String>();
			    record.put("firstname", firstName);
			    record.put("lastname", lastName);
			    record.put("address", address);
			    record.put("phone", phone);
			    record.put("specialization", specialization);
			    record.put("location", location);
			    record.put("managerID", managerID);
			   
			    if(records.containsKey(initial)) // if the initial index is exist in the database
			    records.get(initial).put(recordId, record);
			    else{ // if the initial index isn't exist in the database
			    	HashMap<String,HashMap<String,String>> rec=new HashMap<String,HashMap<String,String>>();
			    	rec.put(recordId,record);
			    	records.put(initial, rec);
			    }
			    indexTR.put(recordId, initial); // put the index into index database hashmap
			    maxCountTR++; // the number of teacher record add 1
			    msg=String.format("manager[%s] create teacher record :(%s,%s,%s,%s,%s,%s,%s,%s) successfully",managerID, recordId,
						firstName,lastName,address,phone,specialization,location,managerID);

			    //make sure the recordId of backup server is the same as leader when create take place simultaneously
			    if(myServer.equals(tolerant.curLeader))//only leader's request come from front end
			      tolerant.addRequesttoBuffer(true,"creatTR",myServer,String.format("%s|%s|%s|%s|%s|%s|%s", managerID, firstName, lastName, address,  phone,specialization, location));
			}
			
	    	return log.logMsg(msg, true,"server."+myServer,"createTRecord");
	    }
		
	}
    /**
     * this is the remote web service method which offer the service for creating a student record
     */
	
	public String createSRecord(String managerID, String firstName, String lastName, String courseRegistered,
			String status, String statusDate) {
		
		String opreation=String.format("manager[%s] want to create student record(%s, %s, %s, %s, %s, %s)", managerID,managerID,
				firstName, lastName, courseRegistered, status, statusDate);
		log.logMsg(opreation, true, "server."+myServer,"createSRecord");
		    String msg="";
		    if(lastName.equals("")) // the lastname is not allowed  to be empty
		       return log.logMsg("The lastname can not be empty!", false,"server."+myServer,"createSRecord");
		    if(firstName.equals("")) // the firstname is not allowed to be empty
				return log.logMsg("The firstname can not be empty!", false,"server."+myServer,"createSRecord");
		    String[] courseCheck=courseRegistered.toLowerCase().split("/"); // use coursecheck array to check the validity of coureRegistered
		    for(int i=0;i<courseCheck.length;i++){
		    	if(courseVaildSet.indexOf(";"+courseCheck[i]+";")==-1)
		    	    return log.logMsg("invaild course enter,please enter again(french/maths/science)", false,"server."+myServer,"createSRecord");
		    }
		    
			if (!isValidDate(statusDate)){ // the statusDate must be validity
				return log.logMsg(String.format("value [%s] is invalid for status date(format: yyyy-MM-dd, e.g. 2018-05-27)!", statusDate), false,"server."+myServer,"createSRecord");
			}
		    
		    if(!status.equals("active")&&!status.equals("inactive")) // the status must be active or inactive
		        return log.logMsg("invaild status enter,please enter again(active/inactive)", false,"server."+myServer,"createSRecord");
		    else{
				
				synchronized(records){ //use database hashmap as lock to make thread safe(synchronized) when update HashMap data
				    char initial=lastName.toUpperCase().charAt(0);
				    
				    String recordId=String.format("%sSR%05d", myServer.substring(0, 1).toUpperCase(),maxCountSR+1);
				    
				    HashMap<String,String> record=new HashMap<String,String>();
				    record.put("firstname", firstName);
				    record.put("lastname", lastName);
				    record.put("courseregistered",courseRegistered);
				    record.put("status", status);
				    record.put("statusdate", statusDate);
				    record.put("managerID", managerID);
				   
				    if(records.containsKey(initial))
					    records.get(initial).put(recordId, record);
					    else{
					    	HashMap<String,HashMap<String,String>> rec=new HashMap<String,HashMap<String,String>>();
					    	rec.put(recordId,record);
					    	records.put(initial, rec);
					    }
				    indexSR.put(recordId, initial);	
				    maxCountSR++;
				    msg=String.format("manager[%s] create student record :(%s,%s,%s,%s,%s,%s,%s) successfully",managerID, recordId,firstName,lastName,courseRegistered
				    		,status,statusDate,managerID);
				    
				    //make sure the recordId of backup server is the same as leader when create take place simultaneously
				    if(myServer.equals(tolerant.curLeader))//only leader's request come from front end
				      tolerant.addRequesttoBuffer(true,"creatSR",myServer,String.format("%s|%s|%s|%s|%s|%s", managerID, firstName, lastName, courseRegistered, status, statusDate));				    
				}
		    return log.logMsg(msg, true,"server."+myServer,"createSRecord");
		    }
	}
    /**
     * this is the remote web service method which is used for get the number of records from 3 servers 
     */
	
	public String getRecordCounts(String managerID) {
		log.logMsg("manager "+managerID+" want to get the count of records", true,"server."+myServer,"getRecordCounts");
		String result="";
		
		String mtlURL = tolerant.config.get(mtlLeaderID);
	    String lvlURL = tolerant.config.get(lvlLeaderID);
	    String ddoURL = tolerant.config.get(ddoLeaderID);
				
		synchronized(records){ //use database hashmap as lock to make thread safe(synchronized) when update HashMap data
		switch(myServer.substring(0, 3)){ //get the count of records from local server and send the udp request to other two server to get its records count respective
		       case "mtl":
		    	   result=String.format("MTL %d,LVL %d,DDO %d", getMyCounts(),getUDPCounts(lvlURL.split("\\:")[0],lvlURL.split("\\:")[1]),getUDPCounts(ddoURL.split("\\:")[0],ddoURL.split("\\:")[1]));
		    	   break;
		       case "lvl":
		    	   result=String.format("MTL %d,LVL %d,DDO %d", getUDPCounts(mtlURL.split("\\:")[0],mtlURL.split("\\:")[1]),getMyCounts(),getUDPCounts(ddoURL.split("\\:")[0],ddoURL.split("\\:")[1]));
		    	   break;
		       case "ddo":
		    	   result=String.format("MTL %d,LVL %d,DDO %d", getUDPCounts(mtlURL.split("\\:")[0],mtlURL.split("\\:")[1]),getUDPCounts(lvlURL.split("\\:")[0],lvlURL.split("\\:")[1]),getMyCounts());
		    	   break;		       
		}
		}	
		return log.logMsg(result, true,"server."+myServer,"getRecordCounts");
	}
	/**
	 * this method is to get the count of records of local server
	 * @return the count of records of local server
	 */
    public int getMyCounts(){
    	
    	return indexTR.size()+indexSR.size();// the count of records equal to the count of student records plus that of teacher records count	    	    	
       }
    /**
     * this method is send the udp to other remote server for getting the number of records 
     * @param ip
     * @return the number of records from remote server
     */
    
    public int getUDPCounts(String ip,String portString){
    	int result=0;
    	byte[] receiveBuf=new byte[1024];
    	DatagramPacket dpReceive=new DatagramPacket(receiveBuf,receiveBuf.length);
    	try{
            int port=Integer.parseInt(portString);           
            //socket.setSoTimeout(10);
            String info="getRecordCounts"; // the request information is "getRecordCounts"
            byte[] data=info.getBytes();
            InetAddress addr = getInetAddress(ip);
            DatagramPacket dpSend=new DatagramPacket(data,data.length,addr,port);
            socket.send(dpSend);
            socket.receive(dpReceive);
            result=Integer.parseInt(new String(dpReceive.getData(),0,dpReceive.getLength())); // get the respond and change it into number
            
          }catch(Exception e){
        	 e.printStackTrace();
          }
    	return result;
    }
    /**
     * this is the remote web service method which offer the service for editing the record
     */
    
	public String editRecord(String managerID, String recordID, String fieldName, String newValue) {
		
		char index;
		 String operation=String.format("manager[%s] want to change record[%s]'s field[%s] into new value[%s]",
				 managerID,recordID,fieldName,newValue);
		 log.logMsg(operation, true,"server."+myServer,"editRecord");
		 String msg="";
		 String action="";
		 
		 
		 fieldName=fieldName.replaceAll(" ","").toLowerCase();//field name stored in hashmap("First Name" acceptable, convert to "firstname")
		 
		 if (fieldName.equals("recordid")) //check recordID key
		     return log.logMsg("Field RecordID is key, can not be changed!", false,"server."+myServer,"editRecord");
		 if(fieldName.equals("lastname")) // lastname is not allowed to be edit
			 return log.logMsg("Field lastname can not be changed!", false,"server."+myServer,"editRecord");
		 if(fieldName.equals("firstname")) // firstname is not allowed to be edit
			 return log.logMsg("Field firstname can not be changed!", false,"server."+myServer,"editRecord"); 
		 if (fieldName.equals("statusdate")&&!isValidDate(newValue)){ // check if the new value of status date is validity
			 return log.logMsg(String.format("value [%s] is invalid for status date(format: yyyy-MM-dd, e.g. 2018-05-27)!", newValue), false,"server."+myServer,"editRecord");
		 }
		 if(fieldName.equals("specialization")) // specialization is not allowed to be changed
			 return log.logMsg("Field specialization can not be changed!", false, "server."+myServer,"editRecord");
		 
		 recordID=recordID.toUpperCase();
		 if(!checkRecordID(recordID)) // check if the record id is validity
		     return log.logMsg("invaild recordID, please try again(TR or SR+5 digit)", false,"server."+myServer,"editRecord");
		 if(!checkFieldName(recordID,fieldName,newValue)) // check if the field name is corresponding to the type of record
		     return log.logMsg("invaild fieldName:"+fieldName+", please try again", false,"server."+myServer,"editRecord");		 
		 if(!checkValue(fieldName,newValue)) // check if the newvalue is corresponding to the field name
		     return log.logMsg("invaild value:"+newValue+",please try again", false,"server."+myServer,"editRecord");
		synchronized(records){ //use database hashmap as lock to make thread safe(synchronized) when update HashMap data
		 if((recordID.substring(1, 2).equals("T")&&!indexTR.containsKey(recordID))||
				 recordID.substring(1, 2).equals("S")&&!indexSR.containsKey(recordID))
					//guarantee index=indexTR.get(recordID) does not report null error at editRecord()
				    return log.logMsg(String.format("(Edit by "+managerID+") RecordID [%s] does not exists in [%s] server!", recordID,myServer), false,"server."+myServer,"editRecord");
		 if(recordID.substring(1, 2).equals("T")) // get the corresponding index
			 index=indexTR.get(recordID);
		 else
			 index=indexSR.get(recordID); // get the corresponding index
		 HashMap<String,HashMap<String,String>> eRecord=records.get(index); // get the corresponding record by index
		
		 if(eRecord.containsKey(recordID)){
			if(fieldName.equals("status")){ // if the manager want to change status, we should update statusdate together
					 String s=eRecord.get(recordID).get("status");
					 if(!newValue.equalsIgnoreCase(s)){//status changed, update status date to current date
						 SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
						 String statusDate=df.format(new Date());
						 eRecord.get(recordID).put("statusdate", statusDate);
					 }
					 
				 }
				 eRecord.get(recordID).put(fieldName,newValue);	
				 action=String.format("(Edit by "+managerID+") Edit record(%s) successfully, the field name(%s) changed into new value(%s)", recordID,fieldName,newValue);
						 
			 msg = log.logMsg(action, true,"server."+myServer,"editRecord");
		 }
		 else
			 msg= log.logMsg(String.format("(Edit by "+managerID+") RecordID [%s] does not exists in [%s] server!", recordID,myServer), false,"server."+myServer,"editRecord");
  		     //guarantee index=indexTR.get(recordID) does not report null error at editRecord()
		
		//make sure edit the same recordId of backup server as leader when edit take place simultaneously
		 if(myServer.equals(tolerant.curLeader))//only leader's request come from front end
		    tolerant.addRequesttoBuffer(true,"editRecord",myServer,String.format("%s|%s|%s|%s",managerID, recordID, fieldName, newValue));
		 
		 return msg;
		}
	}
    /**
     * this method is used for checking if the record id is validity
     * @param recordID
     * @return true or false
     */
	
	public boolean checkRecordID(String recordID){
		boolean result=true;
		if(recordID.length()!=8) // if the length of record id not equal to 8, then it is invalid(eg MTR00001 is valid)
			return false;
		else{
			String leftCheck=";"+recordID.substring(0, 3)+";"; 
			String rightCheck=recordID.substring(3,8);
			if(reVaildSet.indexOf(leftCheck)==-1)// check if the first 3 prefix of record id is MTR,LTR,DTR,MSR,LSR,DSR
				return false;
			try{
				Integer.parseInt(rightCheck);//check if the rest of record id is number
			}catch(Exception e){
				result=false;
			}
		}
		return result;
	}
	/**
	 * this method is used for checking if the field name is corresponding to the type of record(eg the teacher record can be its corresponding filename)
	 * @param recordID
	 * @param fieldName
	 * @param newValue
	 * @return true or false
	 */
	
	public boolean checkFieldName(String recordID, String fieldName, String newValue){
		boolean result=true;
		String checkToken=";"+fieldName.toLowerCase()+";"; 
		if(recordID.substring(1, 2).equals("T")){
			if(trVaildSet.indexOf(checkToken)==-1) // use trVaildSet to check the validity of teacher record 
				result=false;
		}
		 else{
			 if(srVaildSet.indexOf(checkToken)==-1)// use srVaildSet to check the validity of student record
				 result=false;
		 }
		return result; 
	}
	/**
	 * this method is used for checking  if the newvalue is corresponding to the field name
	 * @param fieldName
	 * @param newValue
	 * @return
	 */
	
	public boolean checkValue(String fieldName, String newValue){
		boolean result=true;
		fieldName=fieldName.toLowerCase();
		if(fieldName.equals("location")){ //check if location is mtl,lvl,ddo 
			String checkToken=";"+newValue+";";
			if(locVaildSet.indexOf(checkToken)==-1)
				return false;
			else
				return true;
		}
		else if(fieldName.equals("courseregistered")){ // check if the courseregistered is consist of french,science,maths
			String[] courseCheck=newValue.toLowerCase().split("/");
		    for(int i=0;i<courseCheck.length;i++){
		    	if(courseVaildSet.indexOf(";"+courseCheck[i]+";")==-1)
		    	    return false;
		    }
		    return true;
		}
		else if(fieldName.equals("specialization")){ // check if the specialization is french,maths,science
			if(!newValue.equals("french")&&
		    		!newValue.equals("maths")&&!newValue.equals("science"))
				return false;
			else
				return true;
		}
		else if(fieldName.equals("status")){ //check if the status is active or inactive
			if(!newValue.equals("active")&&!newValue.equals("inactive"))
				return false;
			else
				return true;
		}
		else
		return result;
	}	
    /**
     * this is the remote web service method which offer the service for transfering the record to remote server from local server 
     */
	
	public String transferRecord(String managerID, String recordID, String remoteCenterServerName) {
		String result="";
		if(locVaildSet.indexOf(remoteCenterServerName)==-1)// check if the remote server name is valid
			return log.logMsg("Server "+remoteCenterServerName+" not exist", false, "server."+myServer,"transferRecord");
		else if(remoteCenterServerName.toLowerCase().equals(myServer))// check if the remote server is equal to the local server name
			return log.logMsg("it is local server", false, "server."+myServer,"transferRecord");
		
		String mtlURL = tolerant.config.get(mtlLeaderID);
	    String lvlURL = tolerant.config.get(lvlLeaderID);
	    String ddoURL = tolerant.config.get(ddoLeaderID);
		
		synchronized(records){ //use database hashmap as lock to make thread safe(synchronized) when update HashMap data  
			String newRecord=getRecord(managerID,recordID);
			if(newRecord.equals(""))
				return log.logMsg("(transfer by "+managerID+") record "+recordID+" not exist in ["+myServer+"] server!", false, "server."+myServer,"transferRecord");

			switch(remoteCenterServerName){ //insert into hash map database at other server, so does not need synchronized in transferUDPRecord
			case "mtl":result=log.logMsg(transferUDPRecord(mtlURL.split("\\:")[0],mtlURL.split("\\:")[1] ,newRecord), true, "server."+myServer,"transferRecord");break;
			
			case "lvl":result=log.logMsg(transferUDPRecord(lvlURL.split("\\:")[0],lvlURL.split("\\:")[1] ,newRecord), true, "server."+myServer,"transferRecord");break;
			
			case "ddo":result= log.logMsg(transferUDPRecord(ddoURL.split("\\:")[0],ddoURL.split("\\:")[1] ,newRecord), true, "server."+myServer,"transferRecord");break;
			}
			removeData(recordID); //after success of transferUDPRecord, remove recordID(synchronized)			
		}
		return result;
		
	}
	/**
	 * this method is used for get the record according to the record id, and pack the record into a string
	 * @param managerID
	 * @param recordID
	 * @return record string
	 */
	
	public String getRecord(String managerID,String recordID){
		String result="";
		char index;
		if(recordID.substring(1, 2).equals("T")&&indexTR.containsKey(recordID)){// if it is the teacher record and if it is exist 
			String key=recordID;
			index=indexTR.get(key);
			HashMap<String,String> record=records.get(index).get(key);
			result=String.format("%s|%s|%s|%s|%s|%s|%s|%s",key,record.get("firstname"), 
					record.get("lastname"),record.get("address"),record.get("phone"),
					record.get("specialization"),record.get("location"),managerID);	// pack the record into a string					
		}
		else if(recordID.substring(1, 2).equals("S")&&indexSR.containsKey(recordID)){// if it is the student record and if it is exist
			String key=recordID;
			index=indexSR.get(key);
			HashMap<String,String> record=records.get(index).get(key);
			result=String.format("%s|%s|%s|%s|%s|%s|%s",key,record.get("firstname"),record.get("lastname"),record.get("courseregistered"),record.get("status"),
					record.get("statusdate"),managerID);
		}
		return result; //empty: record does not exist
	}
	/**
	 * this method is used to send a udp request to remote server for transferring 
	 * @param ip
	 * @param recordinfo
	 * @return respond information
	 */
	
	public  String transferUDPRecord(String ip,String portString,String recordinfo){
    	String result="";
    	byte[] receiveBuf=new byte[1024];
    	DatagramPacket dpReceive=new DatagramPacket(receiveBuf,receiveBuf.length);
    	try{
            int port=Integer.parseInt(portString);
            //socket.setSoTimeout(10);
            String info="transferReceive,frontend;"+recordinfo; // if the request start with "t", then it is the request for transferring
            byte[] data=info.getBytes();
            InetAddress addr = getInetAddress(ip);
            DatagramPacket dpSend=new DatagramPacket(data,data.length,addr,port);
            socket.send(dpSend);
            socket.receive(dpReceive);
            result=new String(dpReceive.getData(),0,dpReceive.getLength());
            
          }catch(Exception e){
        	 e.printStackTrace();
          }
    	return result;
    }
	/**
	 * this method is used to decode the requestinfo(the record string from remote server) into record information and insert it into local database hashmap
	 * @param requestInfo
	 * @return the respond information
	 */
	public String doTransferedReceive(String requestInfo){ //insert into hash map database at other server, so does not need synchronized
		String result="";
		String[] newRecord=requestInfo.split("\\|");
		HashMap<String,String> record=new HashMap<String,String>();
    	if(newRecord.length==8){ // do the same action as createTR 
    		 char initial=newRecord[2].charAt(0);
			    record.put("firstname", newRecord[1]);
			    record.put("lastname", newRecord[2]);
			    record.put("address", newRecord[3]);
			    record.put("phone", newRecord[4]);
			    record.put("specialization", newRecord[5]);
			    record.put("location", newRecord[6]);
			    record.put("managerID", newRecord[7]);
			    
			    if(records.containsKey(initial))
			    	records.get(initial).put(newRecord[0], record);
			    else{
			    	HashMap<String,HashMap<String,String>> rec=new HashMap<String,HashMap<String,String>>();
			    	rec.put(newRecord[0],record);
			    	records.put(initial, rec);
			    }
			   indexTR.put(newRecord[0], initial);
			   result=String.format("transfer teacher record :(%s,%s,%s,%s,%s,%s,%s) into server[%s] by manager[%s] successfully",newRecord[0],
					  newRecord[1],newRecord[2],newRecord[3],newRecord[4],newRecord[5],newRecord[6],myServer,newRecord[7]);
    	}
    	else{ // do the same action as createSR 
    		char initial=newRecord[2].charAt(0);
		    record.put("firstname", newRecord[1]);
		    record.put("lastname", newRecord[2]);
		    record.put("courseregistered",newRecord[3]);
		    record.put("status", newRecord[4]);
		    record.put("statusdate", newRecord[5]);
		    record.put("managerID", newRecord[6]);
		    //record.put("recordid", newRecord[0]);//not needed
		    if(records.containsKey(initial))
		    	records.get(initial).put(newRecord[0], record);
			    else{
			    	HashMap<String,HashMap<String,String>> rec=new HashMap<String,HashMap<String,String>>();
			    	rec.put(newRecord[0],record);
			    	records.put(initial, rec);
			    }
		    indexSR.put(newRecord[0], initial);	
		    result=String.format("transfer student record :(%s,%s,%s,%s,%s,%s) into server[%s] by manager[%s] successfully",newRecord[0],
 					  newRecord[1],newRecord[2],newRecord[3],newRecord[4],newRecord[5],myServer,newRecord[6]);
    	}
    	log.logMsg(result, true, "server."+myServer, "receiveRecord");
		return result;
	}
	/**
	 * this method is used to remove the record which have been already transfered
	 * @param recordID
	 */
	
	public void removeData(String recordID){
		char index;
		HashMap<String,HashMap<String,String>> dealRecord;
		synchronized(records){
		if(recordID.charAt(1)=='T'){ // delete the teacher record from database record 
			index=indexTR.get(recordID);
			indexTR.remove(recordID); 
		}
			
		else{
			index=indexSR.get(recordID); // delete the student record from database record
			indexSR.remove(recordID);
		}
		dealRecord=records.get(index);
		dealRecord.get(recordID).clear();
		dealRecord.remove(recordID);
		if(dealRecord.size()==0)
			records.remove(index);							
	}
	}

}
