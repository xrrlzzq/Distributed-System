package common;


/**
* common/HighAvailableDcmsOperationsPOA.java .
* 由IDL-to-Java 编译器 (可移植), 版本 "3.2"生成
* 从HighAvailableDcmsOperations.idl
* 2018年7月15日 星期日 下午01时24分16秒 EDT
*/

public abstract class HighAvailableDcmsOperationsPOA extends org.omg.PortableServer.Servant
 implements common.HighAvailableDcmsOperationsOperations, org.omg.CORBA.portable.InvokeHandler
{

  // Constructors

  private static java.util.Hashtable _methods = new java.util.Hashtable ();
  static
  {
    _methods.put ("createTRecord", new java.lang.Integer (0));
    _methods.put ("createSRecord", new java.lang.Integer (1));
    _methods.put ("getRecordCounts", new java.lang.Integer (2));
    _methods.put ("editRecord", new java.lang.Integer (3));
    _methods.put ("transferRecord", new java.lang.Integer (4));
  }

  public org.omg.CORBA.portable.OutputStream _invoke (String $method,
                                org.omg.CORBA.portable.InputStream in,
                                org.omg.CORBA.portable.ResponseHandler $rh)
  {
    org.omg.CORBA.portable.OutputStream out = null;
    java.lang.Integer __method = (java.lang.Integer)_methods.get ($method);
    if (__method == null)
      throw new org.omg.CORBA.BAD_OPERATION (0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);

    switch (__method.intValue ())
    {
       case 0:  // common/HighAvailableDcmsOperations/createTRecord
       {
         String managerID = in.read_string ();
         String firstName = in.read_string ();
         String lastName = in.read_string ();
         String address = in.read_string ();
         String phone = in.read_string ();
         String specialization = in.read_string ();
         String location = in.read_string ();
         String $result = null;
         $result = this.createTRecord (managerID, firstName, lastName, address, phone, specialization, location);
         out = $rh.createReply();
         out.write_string ($result);
         break;
       }

       case 1:  // common/HighAvailableDcmsOperations/createSRecord
       {
         String managerID = in.read_string ();
         String firstName = in.read_string ();
         String lastName = in.read_string ();
         String courseRegistered = in.read_string ();
         String status = in.read_string ();
         String statusDate = in.read_string ();
         String $result = null;
         $result = this.createSRecord (managerID, firstName, lastName, courseRegistered, status, statusDate);
         out = $rh.createReply();
         out.write_string ($result);
         break;
       }

       case 2:  // common/HighAvailableDcmsOperations/getRecordCounts
       {
         String managerID = in.read_string ();
         String $result = null;
         $result = this.getRecordCounts (managerID);
         out = $rh.createReply();
         out.write_string ($result);
         break;
       }

       case 3:  // common/HighAvailableDcmsOperations/editRecord
       {
         String managerID = in.read_string ();
         String recordID = in.read_string ();
         String fieldName = in.read_string ();
         String newValue = in.read_string ();
         String $result = null;
         $result = this.editRecord (managerID, recordID, fieldName, newValue);
         out = $rh.createReply();
         out.write_string ($result);
         break;
       }

       case 4:  // common/HighAvailableDcmsOperations/transferRecord
       {
         String managerID = in.read_string ();
         String recordID = in.read_string ();
         String remoteCenterServerName = in.read_string ();
         String $result = null;
         $result = this.transferRecord (managerID, recordID, remoteCenterServerName);
         out = $rh.createReply();
         out.write_string ($result);
         break;
       }

       default:
         throw new org.omg.CORBA.BAD_OPERATION (0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);
    }

    return out;
  } // _invoke

  // Type-specific CORBA::Object operations
  private static String[] __ids = {
    "IDL:common/HighAvailableDcmsOperations:1.0"};

  public String[] _all_interfaces (org.omg.PortableServer.POA poa, byte[] objectId)
  {
    return (String[])__ids.clone ();
  }

  public HighAvailableDcmsOperations _this() 
  {
    return HighAvailableDcmsOperationsHelper.narrow(
    super._this_object());
  }

  public HighAvailableDcmsOperations _this(org.omg.CORBA.ORB orb) 
  {
    return HighAvailableDcmsOperationsHelper.narrow(
    super._this_object(orb));
  }


} // class HighAvailableDcmsOperationsPOA
