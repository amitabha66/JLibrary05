package org.rzo.yajsw.util;

import java.util.Date;

public class File extends java.io.File
{

	public File(String pathname)
	{
		super(pathname);
	}

	public long created()
	{
		return FileUtils.created(this);
	}

	public long getFreeSpace()
	{
		return FileUtils.freeSpace(this);
	}

	public long getTotalSpace()
	{
		return FileUtils.totalSpace(this);
	}

	static public void main(String[] args)
	{
		File f = new File("E:");
		System.out.println(new Date(f.created()));
		System.out.println(f.getFreeSpace());
		System.out.println(f.getTotalSpace());
	}

}
