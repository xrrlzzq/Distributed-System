package common;


/**
* common/HighAvailableDcmsOperationsOperations.java .
* 由IDL-to-Java 编译器 (可移植), 版本 "3.2"生成
* 从HighAvailableDcmsOperations.idl
* 2018年7月15日 星期日 下午01时24分16秒 EDT
*/

public interface HighAvailableDcmsOperationsOperations 
{
  String createTRecord (String managerID, String firstName, String lastName, String address, String phone, String specialization, String location);
  String createSRecord (String managerID, String firstName, String lastName, String courseRegistered, String status, String statusDate);
  String getRecordCounts (String managerID);
  String editRecord (String managerID, String recordID, String fieldName, String newValue);
  String transferRecord (String managerID, String recordID, String remoteCenterServerName);
} // interface HighAvailableDcmsOperationsOperations
