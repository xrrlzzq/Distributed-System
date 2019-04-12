package client; 

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner; 

import server.LogTool;  
import server.ManagerOperations;

public class ManagerClient {

    private static String managerCheck=";mtl;lvl;ddo;";
	private static String ip="";
	private static String port="";
	private static String id="";
	static LogTool log = null;
    public static void main(String[] args) throws IOException {
		String key="";		
		if(args.length<1){
			System.out.println("please enter your manager id(enter mtl,lvl or ddo and 4-digit,eg mtl0001)");
			return;
		}
		else{
			
			id=args[0].toLowerCase();			
			if(id.length()!=7){
				System.out.println("invaild id,please enter again(enter mtl,lvl or ddo and 4-digit,eg mtl0001)");
				return;
			}
			try{
				int check=Integer.parseInt(id.substring(3, 7));
			}catch(Exception e){
				System.out.println("invaild id,please enter again(enter mtl,lvl or ddo and 4-digit,eg mtl0001)");
				return;
			}
			key=id.substring(0,3);
			if(managerCheck.indexOf(";"+key+";")==-1){
				System.out.println("invaild id,please try again(enter mtl,lvl or ddo and 4-digit,eg mtl0001)");
				return;
			}
			
			getURL(key);
		}

		log = new LogTool(id+".log");		
		String url=String.format("rmi://%s:%s/ManagerOperations", ip,port);		
    	try{
    		ManagerOperations op=(ManagerOperations)Naming.lookup(url);
        	log.logMsg(String.format("Connected to %s server!", key),true,"client."+id,"main");
        	doOption(op);    		
    	}catch (MalformedURLException e) {  
            log.logMsg("bad URL"+e.getMessage(),false,"client."+id,"main");
            
        } catch (RemoteException e) {  
            log.logMsg("Create remote object error:"+e.getMessage(),false,"client."+id,"main");
           
        } catch (NotBoundException e) {  
            log.logMsg("Not bind error:"+e.getLocalizedMessage(),false,"client."+id,"main");
        } catch(Exception e){  
            e.printStackTrace();  
        }    	
	}
    public static void doCreateTR( ManagerOperations op,Scanner sc) throws RemoteException{
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
    	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");

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
    	String opreation=String.format("client[%s] is creating teacher record(%s %s %s %s %s %s)", id,
				firstname, lastname, address,  phone, specialization,
				location);
		log.logMsg(opreation, true, "client."+id,"doCreateTR");
    	String msg=op.createTRecord(firstname, lastname, address, phone, specialization, location);
    	log.logMsg(msg,true,"client."+id,"doCreateTR");
    }
    public static void doCreateSR( ManagerOperations op,Scanner sc) throws RemoteException{
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
    	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
    	
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
    	String opreation=String.format("client[%s] creating student record(%s %s %s %s %s)", id,
				firstname, lastname, courseRegistered, status, statusDate);
		log.logMsg(opreation, true, "client."+id,"doCreateSR");
    	String msg=op.createSRecord(firstname, lastname, courseRegistered, status, statusDate);
    	log.logMsg(msg,true,"client."+id,"doCreateSR");
    }
    
    public static void doEdit( ManagerOperations op,Scanner sc) throws RemoteException{
    	String recordID="",fieldName="",newValue="";
    	while(true){
    		
    		System.out.println("please enter recordID(prefix(TR/SR) with 5-digit number):");
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
	        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
			 
	        
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
    	String msg=op.editRecord(recordID, fieldName, newValue);
    	log.logMsg(msg,true,"client."+id,"doEdit");
    }
    public static void doOption(ManagerOperations op) throws RemoteException{
    	 boolean quit = false;
    	 Scanner sc=new Scanner(System.in);    	
    	 while(!quit){         
    		System.out.println("\n");
         	System.out.println("1> create teacher record");
         	System.out.println("2> create student record");
         	System.out.println("3> get the count of records");
         	System.out.println("4> edit record");
         	System.out.println("5> exit");
         	System.out.println("please select your operation,enter 1,2,3,4,5");
         	String num=sc.nextLine();
         	
         	if(";1;2;3;4;5;".indexOf(";"+num+";")==-1){
         		System.out.println("please enter numer(1-5)");
         		continue;
         	}
         	
         	switch(num){
        	case "1":doCreateTR(op,sc);break;
        	case "2":doCreateSR(op,sc);break;
        	case "3":log.logMsg("the records counts is: "+op.getRecordCounts(),true,"client."+id,"getCounts");break;
        	case "4":doEdit(op,sc);break;
        	case "5": 
            	 quit = true;
            	 System.out.println("\ngood bye!");
            	 break;
        	}
         }
    	 sc.close();    	
    }
	public static void getURL(String key) throws IOException{
    	Properties p=new Properties();
        InputStream in=new FileInputStream(new File(System.getProperty("user.dir")+"\\config.properties"));
        p.load(in);
        ip=p.getProperty(key);
        port=p.getProperty("rmi");
        
    }
}
