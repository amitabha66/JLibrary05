package org.rzo.netty.ahessian.io;

import static org.jboss.netty.buffer.ChannelBuffers.dynamicBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.netty.buffer.ChannelBuffer;


/**
 * InputStreamBuffer pipes bytes read from the channel to an input stream
 */
class InputStreamBuffer extends InputStream
{
	
	/** Buffer for storing incoming bytes */
	//private ChannelBuffer _buf = dynamicBuffer();
	LinkedList<ChannelBuffer> _bufs = new LinkedList<ChannelBuffer>();
	
	/** Indicates if the stream has been closed */
	private volatile boolean _closed = false;
	private Lock			_lock				= new ReentrantLock();
	/** Sync condition indicating that buffer is not empty. */
	private volatile Condition		_notEmpty			= _lock.newCondition();
	private volatile int _available = 0;
	
	/* (non-Javadoc)
	 * @see java.io.InputStream#read()
	 */
	@Override
	public int read() throws IOException
	{
		int result = -1;
		if (_closed)
			return -1;
		_lock.lock();
		try
		{
		while  (!_closed && available() == 0)
			_notEmpty.await();
		if (!_closed)
		{
			result = (int)_bufs.getFirst().readByte() & 0xFF;
			_available--;
			checkBufs();
		}
		}
		catch (Exception ex)
		{
			throw new IOException(ex.getMessage());
		}
		_lock.unlock();
		return result;
	}
	
	private void checkBufs()
	{
		while (!_bufs.isEmpty()  && _bufs.getFirst().readableBytes() == 0)
			_bufs.removeFirst();
	}
	
	/* (non-Javadoc)
	 * @see java.io.InputStream#close()
	 */
	@Override
	public void close() throws IOException
	{
		_lock.lock();
		_closed = true;
		_notEmpty.signal();
		_lock.unlock();
		super.close();
	}
	
	/**
	 * Insert bytes to the input stream
	 * 
	 * @param buf bytes received from previous upstream handler
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void write(ChannelBuffer buf) throws IOException
	{
		if (_closed)
			throw new IOException("stream closed");
		_lock.lock();
		if (_bufs.isEmpty() || buf != _bufs.getLast())
			_bufs.addLast(buf);
		_available += buf.readableBytes();
		_notEmpty.signal();
		_lock.unlock();		
	}
	
	/* (non-Javadoc)
	 * @see java.io.InputStream#available()
	 */
	public int available() throws IOException
	{
		if (_closed)
			throw new IOException("stream closed");
		return _available;
	}
	
    /* (non-Javadoc)
     * @see java.io.InputStream#read(byte[], int, int)
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException 
    {
    	int result = -1;
    	if (_closed)
    		return -1;
    	_lock.lock();
    	try
    	{
    	while (!_closed && available() == 0)
    	{
    		_notEmpty.await();
    	}
    	if (!_closed)
    	{
	    	int length = Math.min(len, _bufs.getFirst().readableBytes());
	    	_bufs.getFirst().readBytes(b, off, length);
	    	result = length;
	    	_available -= length;
			checkBufs();
    	}
    	}
    	catch (Exception ex)
    	{
    		throw new IOException(ex.getMessage());
    	}
    	_lock.unlock();
    	return result;
    }
    
    /**
     * Checks if the stream is closed.
     * 
     * @return true, if is closed
     */
    public boolean isClosed()
    {
    	return _closed;
    }
    
    

}
