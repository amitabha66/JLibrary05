package org.rzo.netty.ahessian.rpc.io;

import org.rzo.netty.ahessian.rpc.message.HessianRPCCallMessage;
import org.rzo.netty.ahessian.rpc.message.HessianRPCReplyMessage;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

/**
 * The Class HessianOutput.
 */
public class HessianOutput extends com.caucho.hessian.io.HessianOutput
{

	  /**
  	 * Instantiates a new hessian output.
  	 * 
  	 * @param out the out
  	 */
  	public HessianOutput(OutputStream out)
	{
		super(out);
	}

	/**
	 * Write reply.
	 * 
	 * @param message the message
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void writeReply(HessianRPCReplyMessage message)
	    throws IOException
	  {
		if (message.getFault() == null)
		{
	    startReply();
	    writeHeaders(message.getHeaders());
	    writeObject(message.getValue());
	    completeReply();
		}
		else
		{
			Throwable fault = message.getFault();
			this.writeFault(fault.getClass().getSimpleName(), fault.getMessage(), null);
		}
	  }
	  
	  /**
  	 * Call.
  	 * 
  	 * @param message the message
  	 * 
  	 * @throws IOException Signals that an I/O exception has occurred.
  	 */
  	public void call(HessianRPCCallMessage message)
	    throws IOException
	  {
		  String method = message.getMethod();
		  Object []args = message.getArgs();
		  Map headers = message.getHeaders();
	    int length = args != null ? args.length : 0;
	    
	    os.write('c');
	    os.write(1);
	    os.write(0);

	    writeHeaders(headers);

	    
	    os.write('m');
	    int len = method.length();
	    os.write(len >> 8);
	    os.write(len);
	    printString(method, 0, len);
	    
	    for (int i = 0; i < length; i++)
	      writeObject(args[i]);
	    
	    completeCall();
	  }

	  
	  private void writeHeaders(Map headers)
	  {
		    try
		    {
		    	for (Iterator it=headers.keySet().iterator(); it.hasNext(); )
		    	{
		    		String key = (String) it.next();
		    		Object value = headers.get(key);
		    		//System.out.println("write header "+key+" "+value);
			    writeHeader(key);
			    writeObject(value);
		    	}
		    }
		    catch (Exception ex)
		    {
		    	ex.printStackTrace();
		    }
	  }
	  

}
