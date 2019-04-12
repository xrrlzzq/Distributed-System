package common;


/**
* common/HighAvailableDcmsOperationsHelper.java .
* 由IDL-to-Java 编译器 (可移植), 版本 "3.2"生成
* 从HighAvailableDcmsOperations.idl
* 2018年7月15日 星期日 下午01时24分16秒 EDT
*/

abstract public class HighAvailableDcmsOperationsHelper
{
  private static String  _id = "IDL:common/HighAvailableDcmsOperations:1.0";

  public static void insert (org.omg.CORBA.Any a, common.HighAvailableDcmsOperations that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static common.HighAvailableDcmsOperations extract (org.omg.CORBA.Any a)
  {
    return read (a.create_input_stream ());
  }

  private static org.omg.CORBA.TypeCode __typeCode = null;
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
    if (__typeCode == null)
    {
      __typeCode = org.omg.CORBA.ORB.init ().create_interface_tc (common.HighAvailableDcmsOperationsHelper.id (), "HighAvailableDcmsOperations");
    }
    return __typeCode;
  }

  public static String id ()
  {
    return _id;
  }

  public static common.HighAvailableDcmsOperations read (org.omg.CORBA.portable.InputStream istream)
  {
    return narrow (istream.read_Object (_HighAvailableDcmsOperationsStub.class));
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, common.HighAvailableDcmsOperations value)
  {
    ostream.write_Object ((org.omg.CORBA.Object) value);
  }

  public static common.HighAvailableDcmsOperations narrow (org.omg.CORBA.Object obj)
  {
    if (obj == null)
      return null;
    else if (obj instanceof common.HighAvailableDcmsOperations)
      return (common.HighAvailableDcmsOperations)obj;
    else if (!obj._is_a (id ()))
      throw new org.omg.CORBA.BAD_PARAM ();
    else
    {
      org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate ();
      common._HighAvailableDcmsOperationsStub stub = new common._HighAvailableDcmsOperationsStub ();
      stub._set_delegate(delegate);
      return stub;
    }
  }

  public static common.HighAvailableDcmsOperations unchecked_narrow (org.omg.CORBA.Object obj)
  {
    if (obj == null)
      return null;
    else if (obj instanceof common.HighAvailableDcmsOperations)
      return (common.HighAvailableDcmsOperations)obj;
    else
    {
      org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate ();
      common._HighAvailableDcmsOperationsStub stub = new common._HighAvailableDcmsOperationsStub ();
      stub._set_delegate(delegate);
      return stub;
    }
  }

}
