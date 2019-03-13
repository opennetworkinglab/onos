package org.ctpd.closfwd;

import java.io.Serializable;

public class CustomiceException extends RuntimeException implements Serializable
{
    private static final long serialVersionUID = 1L;
 
    public CustomiceException() {
        super();
    }
	 
    public CustomiceException(Exception e){
        super(e);
    }
    public CustomiceException(String msg)   {
        super(msg);
    }
    public CustomiceException(String msg, Exception e)  {
        super(msg, e);
    }
}
