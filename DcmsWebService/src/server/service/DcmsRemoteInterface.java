package server.service;

import javax.jws.WebMethod;
import javax.jws.WebService;
/**
 * 
 * This is the remote interface
 *
 */
@WebService
public interface DcmsRemoteInterface {

	@WebMethod
	public String createTRecord(String managerID, String firstName, String lastName, String address, String phone,
			String specialization, String location);
	@WebMethod
	public String createSRecord(String managerID, String firstName, String lastName, String courseRegistered,
			String status, String statusDate);
	
	@WebMethod
	public String getRecordCounts(String managerID);
	
	@WebMethod
	public String editRecord(String managerID, String recordID, String fieldName, String newValue);
	
	@WebMethod
	public String transferRecord(String managerID, String recordID, String remoteCenterServerName);
	
}
