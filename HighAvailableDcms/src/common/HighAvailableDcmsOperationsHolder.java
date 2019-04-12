package common;

/**
* common/HighAvailableDcmsOperationsHolder.java .
* 由IDL-to-Java 编译器 (可移植), 版本 "3.2"生成
* 从HighAvailableDcmsOperations.idl
* 2018年7月15日 星期日 下午01时24分16秒 EDT
*/

public final class HighAvailableDcmsOperationsHolder implements org.omg.CORBA.portable.Streamable
{
  public common.HighAvailableDcmsOperations value = null;

  public HighAvailableDcmsOperationsHolder ()
  {
  }

  public HighAvailableDcmsOperationsHolder (common.HighAvailableDcmsOperations initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = common.HighAvailableDcmsOperationsHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    common.HighAvailableDcmsOperationsHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return common.HighAvailableDcmsOperationsHelper.type ();
  }

}
