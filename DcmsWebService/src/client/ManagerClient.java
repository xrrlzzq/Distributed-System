package client; 


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;

import client.stub.DCMS;
import client.stub.DCMSService;

import common.LogTool;

/**
 * This is the class of manager client which deal with the logic of client side  
 * 
 *
 */
public class ManagerClient {

    private static String managerCheck=";mtl;lvl;ddo;";
	private static String ip="";
	private static String port="";
	private static String id="";
	static LogTool log = null;
    public static void main(String[] args) throws IOException, NotBoundException {
		String key="";		
		if(args.length<1){// check if there is login information 
			System.out.println("please enter your manager id(enter mtl,lvl or ddo and 4-digit,eg mtl0001)");
			return;
		}
		else{ //check if it is the valid manager id
			
			id=args[0].toLowerCase();			
			if(id.length()!=7){
				System.out.println("invaild id,please enter again(enter mtl,lvl or ddo and 4-digit,eg mtl0001)");
				return;
			}
			try{
				int check=Integer.parseInt(id.substring(3, 7)); // check if the last 4 prefix of manager id is number
			}catch(Exception e){
				System.out.println("invaild id,please enter again(enter mtl,lvl or ddo and 4-digit,eg mtl0001)");
				return;
			}
			key=id.substring(0,3);
			if(managerCheck.indexOf(";"+key+";")==-1){ // check if the first 3 prefix of manager id is mtl,lvl,ddo
				System.out.println("invaild id,please try again(enter mtl,lvl or ddo and 4-digit,eg mtl0001)");
				return;
			}
			
			//getCorbaParm(key);
		}
        URL url=getURL(key); // to get url 
		log = new LogTool(id+".log");		
				
    	try{
    		DCMS op=new DCMSService(url).getDCMSPort();// instantiate the DCMS object from stub
        	log.logMsg(String.format("Connected to %s server!", key),true,"client."+id,"main");
        	doOption(op); // pass the DCMS instance to doOption function   		
    	}catch (RemoteException e) {  
            log.logMsg("Create remote object error:"+e.getMessage(),false,"client."+id,"main");
           
        } catch(Exception e){  
            e.printStackTrace();  
        }    	
	}
    /**
     * this method is used to receive the user input and pass the input to the remote method CreateTRecord
     * @param op
     * @param sc
     * @throws RemoteException
     */
    public static void doCreateTR( DCMS op,Scanner sc) throws RemoteException{
    	String firstname="",lastname="",address="",phone="",specialization="",location="";
    	while(true){
    	
    	System.out.println("please enter firstname:");
    	firstname=sc.nextLine();
    	System.out.println("please enter lasttname:");
    	lastname=sc.nextLine();
    	System.out.println("please enter address:");
    	address=sc.nextLine();
    	System.out.println("please enter phone:");
    	phone=sc.nextLine();
    	System.out.println("please enter specialization(french, maths, science):");
    	specialization=sc.nextLine();
    	System.out.println("please enter location(mtl,lvl,ddo):");
    	location=sc.nextLine();
    	System.out.println("your enter is following:"
    			           +"\nfirstname="+firstname
    			           +"\nlastname="+lastname
    			           +"\naddress="+address
    			           +"\nphone="+phone
    			           +"\nspecialization="+specialization
    			           +"\nlocation="+location);
    	System.out.println("\nare you sure?(y/n)");
    	String c=sc.nextLine().toUpperCase();

		String execDate;
    	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    	if(c.equals("Y"))
    		break;
    	else if (c.indexOf("-") !=-1) {
    		while(true){
    		  execDate=df.format(new Date());
    		  if (execDate.equals(c))
    			 break;
    		}
    		break;
    	}
    	else
    		continue;
    	}
    	String opreation=String.format("client[%s] is creating teacher record(%s %s %s %s %s %s %s)", id,id,
				firstname, lastname, address,  phone, specialization,
				location);
		log.logMsg(opreation, true, "client."+id,"doCreateTR");
    	String msg=op.createTRecord(id,firstname, lastname, address, phone, specialization, location);
    	log.logMsg(msg,true,"client."+id,"doCreateTR");
    }
    /**
     * this method is used to receive the user input and pass the input to the remote method CreateSRecord
     * @param op
     * @param sc
     * @throws RemoteException
     */
    public static void doCreateSR( DCMS op,Scanner sc) throws RemoteException{
    	String firstname="",lastname="",courseRegistered="", status="", statusDate="";
    	while(true){
    	
    	System.out.println("please enter firstname:");
    	firstname=sc.nextLine();
    	System.out.println("please enter lasttname:");
    	lastname=sc.nextLine();
    	System.out.println("please enter courseRegistered(maths/french/science combination):");
    	courseRegistered=sc.nextLine();
    	System.out.println("please enter status(active/inactive):");
    	status=sc.nextLine();
    	System.out.println("please enter statusDate(dateformat: yyyy-MM-dd):");
    	statusDate=sc.nextLine();
    	System.out.println("your enter is following:"
    			           +"\nfirstname="+firstname
    			           +"\nlastname="+lastname
    			           +"\ncourseRegistered="+courseRegistered
    			           +"\nstatus="+status
    			           +"\nstatusDate="+statusDate);
    	System.out.println("\nare you sure?(y/n)");
    	String c=sc.nextLine().toUpperCase();

    	String execDate;
    	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    	
    	if(c.equals("Y"))
    		break;
    	else if (c.indexOf("-") !=-1) {
    		while(true){
    		  execDate=df.format(new Date());
    		  if (execDate.equals(c))
    			 break;
    		}
    		break;
    	}    	
    	else
    		continue;
    	}
    	String opreation=String.format("client[%s] creating student record(%s %s %s %s %s %s)", id,id,
				firstname, lastname, courseRegistered, status, statusDate);
		log.logMsg(opreation, true, "client."+id,"doCreateSR");
    	String msg=op.createSRecord(id,firstname, lastname, courseRegistered, status, statusDate);
    	log.logMsg(msg,true,"client."+id,"doCreateSR");
    }
    /**
     * this method is used to receive the user input and pass the input to the remote method editRecord
     * @param op
     * @param sc
     * @throws RemoteException
     */
    public static void doEdit( DCMS op,Scanner sc) throws RemoteException{
    	String recordID="",fieldName="",newValue="";
    	while(true){
    		
    		System.out.println("please enter recordID(Serevr initial and prefix(TR/SR) with 5-digit number,etc MTR00001):");
    		recordID=sc.nextLine();
        	System.out.println("please enter fieldName:");
        	fieldName=sc.nextLine();
        	System.out.println("please enter newValue:");
        	newValue=sc.nextLine();
        	System.out.println("your enter is following:"
			           +"\nrecodID="+recordID
			           +"\nfieldName="+fieldName
			           +"\nnewValue="+newValue);
	        System.out.println("\nare you sure?(y/n)");
	        String c=sc.nextLine().toUpperCase();
	    	
	        String execDate;
	        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
			 
	        
	        if(c.equals("Y")){
		       break;
	        }
	        else if (c.indexOf("-") !=-1) {
	    		while(true){
	    		  execDate=df.format(new Date());
	    		  if (execDate.equals(c))
	    			 break;
	    		}
	    		break;
	    	}
	        else
		       continue;
    	}
    	 String operation=String.format("client[%s] is changing record[%s]'s field[%s] into newvalue[%s]",
				 id,recordID,fieldName,newValue);
		 log.logMsg(operation, true,"client."+id,"doEdit");
    	String msg=op.editRecord(id,recordID, fieldName, newValue);
    	log.logMsg(msg,true,"client."+id,"doEdit");
    }
    /**
     * this method is used to receive the user input and pass the input to the remote method transferRecord
     * @param op
     * @param sc
     * @throws RemoteException
     */ 
    public static void doTransfer(DCMS op,Scanner sc){
    	String recordID="",remoteServer="";
    	while(true){
    		System.out.println("please enter recordID:");
    		recordID=sc.nextLine();
    		System.out.println("please enter remote server you want to transfer to:");
    		remoteServer=sc.nextLine();
    		System.out.println("your enter is following:"
    				+ "\nrecordID="+recordID
    				+"\nremoteServer="+remoteServer);
    		 System.out.println("\nare you sure?(y/n)");
 	        String c=sc.nextLine().toUpperCase();
 	    	
 	        String execDate;
 	        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
 			 
 	        
 	        if(c.equals("Y")){
 		       break;
 	        }
 	        else if (c.indexOf("-") !=-1) {
 	    		while(true){
 	    		  execDate=df.format(new Date());
 	    		  if (execDate.equals(c))
 	    			 break;
 	    		}
 	    		break;
 	    	}
 	        else
 		       continue;
    		
    	}
    	String operation=String.format("client[%s] is transferring the record[%s] to remoteServer[%s] ",id ,recordID,remoteServer );
    	log.logMsg(operation, true, "client."+id,"doTransfer");
    	String msg=op.transferRecord(id, recordID, remoteServer);
    	log.logMsg(msg, true,  "client."+id,"doTransfer");
    }
    /**
     * this method is used to get record counts from all three servers
     * @param op
     * @param sc
     */
    public static void doGetRecordCounts(DCMS op,Scanner sc){
    	while(true){
   		    System.out.println("\nThis command will get record counts from all servers, are you sure?(y/n)");
 	        String c=sc.nextLine().toUpperCase();
 	    	
 	        String execDate;
 	        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
 	        
 	        if(c.equals("Y")){
 		       break;
 	        }
 	        else if (c.indexOf("-") !=-1) {
 	    		while(true){
 	    		  execDate=df.format(new Date());
 	    		  if (execDate.equals(c))
 	    			 break;
 	    		}
 	    		break;
 	    	}
 	        else
 		       return;    		
    	}
    	log.logMsg("the records counts is: "+op.getRecordCounts(id),true,"client."+id,"getCounts");
    }
    /**
     * this method would generate the menu and receive the user's input and call the corresponding method according to the user's input
     * @param op
     * @throws RemoteException
     */
    public static void doOption(DCMS op) throws RemoteException{
    	 boolean quit = false;
    	 Scanner sc=new Scanner(System.in);    	
    	 while(!quit){         
    		System.out.println("\n");
         	System.out.println("1> create teacher record");
         	System.out.println("2> create student record");
         	System.out.println("3> get the count of records");
         	System.out.println("4> edit record");
         	System.out.println("5> transfer record");
         	System.out.println("6> exit");
         	System.out.println("please select your operation,enter 1,2,3,4,5,6");
         	String num=sc.nextLine();
         	
         	if(";1;2;3;4;5;6;".indexOf(";"+num+";")==-1){
         		System.out.println("please enter numer(1-6)");
         		continue;
         	}
         	
         	switch(num){
        	case "1":doCreateTR(op,sc);break;
        	case "2":doCreateSR(op,sc);break;
        	case "3":doGetRecordCounts(op,sc);break;
        	case "4":doEdit(op,sc);break;
        	case "5":doTransfer(op,sc);break;
        	case "6": 
            	 quit = true;
            	 System.out.println("\ngood bye!");
            	 break;
        	}
         }
    	 sc.close();    	
    }
    /**
     * this method would get the ip and udp from config file and generate the corresponding URL
     * @param key
     * @return URL
     * @throws IOException
     */
	public static URL getURL(String key) throws IOException{
    	Properties p=new Properties();
        InputStream in=new FileInputStream(new File(System.getProperty("user.dir")+File.separator+"config.properties"));
        p.load(in);
        ip=p.getProperty(key);
        port=p.getProperty("webservice");
        URL url= new URL(String.format("http://%s:%s/",ip,port));
        return url;
    }
}
