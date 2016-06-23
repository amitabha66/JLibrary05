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
public class Hessian2Output extends com.caucho.hessian.io.Hessian2Output
{

	  /**
  	 * Instantiates a new hessian output.
  	 * 
  	 * @param out the out
  	 */
  	public Hessian2Output(OutputStream out)
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
	    writeVersion();
	    startEnvelope("Header");
	    writeHeaders(message.getHeaders());
		if (message.getFault() == null)
		{
	    startReply();
	    writeObject(message.getValue());
	    completeReply();
		}
		else
		{
			Throwable fault = message.getFault();
			this.writeFault(fault.getClass().getSimpleName(), fault.getMessage(), null);
		}
	    completeEnvelope();

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
		  if (args == null)
			  args = new Object[0];
		  Map headers = message.getHeaders();
	    int length = args != null ? args.length : 0;
	    
	    writeVersion();
	    startEnvelope("Header");
	    writeHeaders(headers);
	    // no packet read in Hessian2Input
	    //startPacket();
	    startCall(method, args.length);
	    for (int i = 0; i < length; i++)
	      writeObject(args[i]);	    
	    completeCall();
	    //endPacket();
	    completeEnvelope();
	  }

	  
	  private void writeHeaders(Map headers)
	  {
		    try
		    {
			    writeInt(headers.size());
		    	for (Iterator it=headers.keySet().iterator(); it.hasNext(); )
		    	{
		    		String key = (String) it.next();
		    		Object value = headers.get(key);
		    		//System.out.println("write header "+key+" "+value);
			    writeString(key);
			    writeObject(value);
		    	}
		    }
		    catch (Exception ex)
		    {
		    	ex.printStackTrace();
		    }
	  }

}
