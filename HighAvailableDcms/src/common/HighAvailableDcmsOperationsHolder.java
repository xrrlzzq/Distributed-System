package common;

/**
* common/HighAvailableDcmsOperationsHolder.java .
* ��IDL-to-Java ������ (����ֲ), �汾 "3.2"����
* ��HighAvailableDcmsOperations.idl
* 2018��7��15�� ������ ����01ʱ24��16�� EDT
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
