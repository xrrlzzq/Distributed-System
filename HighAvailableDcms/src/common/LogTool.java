package common;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

class LogToolFormatter extends Formatter{
	private  final DateFormat dFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private String token1;
	private String token2;
	@Override
	public String format(LogRecord record) {
		 StringBuilder sbuilder = new StringBuilder(1000);
	        sbuilder.append(dFormat.format(new Date(record.getMillis()))).append(" - ");
	        sbuilder.append("(").append(token1).append(".");
	        sbuilder.append(token2).append("):  \r\n");
	        sbuilder.append("  (").append(record.getLevel()).append(") ");
	        sbuilder.append(formatMessage(record));
	        sbuilder.append("\r\n\r\n");
	        return sbuilder.toString();		
	}
	public void setupToken(String token1,String token2){
		this.token1 = token1;
		this.token2 = token2;
	}
} 

public class LogTool{		
	private static LogToolFormatter myFormatter=new LogToolFormatter();
	private static Logger logger = Logger.getLogger("MyLogTool");
	private String filename="";
	
	public LogTool(String filename){
		this.filename=filename;
	    setupLogger(Level.ALL);
	}
	
    public Logger setupLogger(Level level)  {
        try{            
            FileHandler fileHandler = new FileHandler(System.getProperty("user.dir")+"\\"+filename, true);
            fileHandler.setFormatter(myFormatter);

            logger.addHandler(fileHandler);
            logger.setLevel(level);
        } catch (Exception e) {
                e.printStackTrace();
        }
        return logger;
    }
    
    public synchronized String logMsg(String msg, Boolean isInfo,String token1,String token2){    
    	myFormatter.setupToken(token1,token2);
    	if(isInfo)
    	    logger.info(msg);
    	else
    		logger.warning(msg);
    
    	return msg;
    }
}

