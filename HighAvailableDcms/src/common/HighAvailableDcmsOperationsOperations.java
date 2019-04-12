package common;


/**
* common/HighAvailableDcmsOperationsOperations.java .
* ��IDL-to-Java ������ (����ֲ), �汾 "3.2"����
* ��HighAvailableDcmsOperations.idl
* 2018��7��15�� ������ ����01ʱ24��16�� EDT
*/

public interface HighAvailableDcmsOperationsOperations 
{
  String createTRecord (String managerID, String firstName, String lastName, String address, String phone, String specialization, String location);
  String createSRecord (String managerID, String firstName, String lastName, String courseRegistered, String status, String statusDate);
  String getRecordCounts (String managerID);
  String editRecord (String managerID, String recordID, String fieldName, String newValue);
  String transferRecord (String managerID, String recordID, String remoteCenterServerName);
} // interface HighAvailableDcmsOperationsOperations
