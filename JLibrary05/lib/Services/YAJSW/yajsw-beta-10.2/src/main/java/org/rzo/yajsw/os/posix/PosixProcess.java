package org.rzo.yajsw.os.posix;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rzo.yajsw.io.CyclicBufferFileInputStream;
import org.rzo.yajsw.io.CyclicBufferFilePrintStream;
import org.rzo.yajsw.os.AbstractProcess;
import org.rzo.yajsw.os.OperatingSystem;
import org.rzo.yajsw.os.Process;
import org.rzo.yajsw.util.DaemonThreadFactory;

import test.TestUbuntu.CLibrary;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;

public class PosixProcess extends AbstractProcess
{
	protected int[]							_inPipe			= new int[2];
	protected int[]							_outPipe		= new int[2];
	protected int[]							_errPipe		= new int[2];
	public IntByReference					status			= new IntByReference();
	int								_exitCodeKill	= -1;

	protected static final Executor	executor		= Executors.newCachedThreadPool(new DaemonThreadFactory("posix.process.terminate"));
	protected boolean							lock			= true;
	volatile protected boolean							_terminated		= false;
	protected Utils _utils = new Utils();
	boolean _stopWaiter = false;

	public interface CLibrary extends Library
	{

		CLibrary	INSTANCE	= (CLibrary) Native.loadLibrary(Platform.isLinux() ? "libc.so.6" : "c",CLibrary.class);

		int fork();

		void exit(int status);
		String strerror (int errnum);
		
		/*
		 * int readlink (const char *filename, char *buffer, size_t size)
		 */
		short readlink (String filename, Memory buffer, short size);


		/*
		 * int execv (const charfilename, charconst argv[])
		 */
		int execvp(String filename, String[] argv);
		int  execve(String path, String[] argv, String[] envp);


		/*
		 * int pipe (int filedes[2])
		 */
		int pipe(int filedes[]);

		/*
		 * int dup2(int oldfd, int newfd)
		 */
		int dup2(int oldfd, int newfd);

		/*
		 * int close(int fd)
		 */
		int close(Pointer fd);

		int close(int fd);

		/*
		 * mode_t umask (mode_t mask)
		 */
		void umask(int mask);

		int setsid();

		/*
		 * FILE freopen ( const char filename, const char mode, FILE stream );
		 */
		Pointer freopen(String filename, String mode, int stream);

		/*
		 * int kill (pid_t pid, int signum)
		 */
		int kill(int pid, int signum);

		static final int	SIGTERM	= 15;
		static final int	SIGKILL	= 9;

		/*
		 * pid_t waitpid(pid_t pid, intstat_loc, int options);
		 */
		int waitpid(int pid, IntByReference stat_loc, int options);

		static final int	ESRCH	= 3;

		/*
		 * int chdir(const charpath);
		 */
		int chdir(String path);

		static final int	WNOHANG		= 1;	/* don't hang in wait */
		static final int	WUNTRACED	= 2;	/*
												 * tell about stopped, untraced
												 * children
												 */

		/*
		 * int fputc (int c, FILEstream)
		 */
		int fputc(int c, Pointer stream);

		/*
		 * FILEfdopen(int fildes, const chartype);
		 */
		Pointer fdopen(Pointer fildes, String type);

		/*
		 * int fileno(FILEstream);
		 */
		int fileno(Pointer stream);

		/*
		 * struct dirent64 { __u64 d_ino; __s64 d_off; unsigned short d_reclen;
		 * unsigned char d_type; char d_name[256]; };
		 */
		class dirent64 extends Structure
		{
			public long		d_ino;
			public long		d_off;
			public short	d_reclen;
			public char		d_type;
			public char[]	d_name	= new char[256];

			public String getName()
			{
				return getPointer().getString(8 + 8 + 2 + 1, false);
			}
		};

		/*
		 * struct dirent { long d_ino; off_t d_off; unsigned short d_reclen;
		 * char d_name[NAME_MAX+1]; };
		 */
		class dirent extends Structure
		{
			public int		d_ino;
			public int		d_off;
			public short	d_reclen;
			public String	d_name;
		};

		/*
		 * DIR opendir (const chardirname)
		 */
		Pointer opendir(String dirname);

		/*
		 * struct dirent64 readdir64 (DIRdirstream)
		 */
		dirent64 readdir64(Pointer dirstream);

		/*
		 * int closedir (DIRdirstream)
		 */
		int closedir(Pointer dirstream);

		/*
		 * int nice (int increment)
		 */
		int nice(int increment);

		/*
		 * int sched_setaffinity (pid_t pid, size_t cpusetsize, const cpu_set_t
		 * cpuset)
		 */
		int sched_setaffinity(int pid, int cpusetsize, IntByReference cpuset);

		/*
		 * pid_t getpid(void);
		 */
		int getpid();

		/*
		 * int symlink (const charoldname, const charnewname)
		 */
		int symlink(String oldname, String newname);

		/*
		 * struct passwd
		 * 
		 * The passwd data structure is used to hold information about entries
		 * in the system user data base. It has at least the following members:
		 * 
		 * charpw_name The user's login name. charpw_passwd. The encrypted
		 * password string. uid_t pw_uid The user ID number. gid_t pw_gid The
		 * user's default group ID number. charpw_gecos A string typically
		 * containing the user's real name, and possibly other information such
		 * as a phone number. charpw_dir The user's home directory, or initial
		 * working directory. This might be a null pointer, in which case the
		 * interpretation is system-dependent. charpw_shell The user's default
		 * shell, or the initial program run when the user logs in. This might
		 * be a null pointer, indicating that the system default should be used.
		 */

		public static class passwd extends Structure
		{
			public passwd(Pointer p)
			{
				super();
				if (p != null)
				{
					this.useMemory(p);
					this.read();
				}
			}

			public String	pw_name;
			public String	pw_passwd;
			public int		pw_uid;
			public int		pw_gid;
			public String	pw_gecos;
			public String	pw_dir;
			public String	pw_shell;

			public String getName()
			{
				return pw_name;
			}

			public int getUid()
			{
				return pw_uid;
			}
		}

		/*
		 * struct passwd getpwnam (const charname) This function returns a
		 * pointer to a statically-allocated structure containing information
		 * about the user whose user name is name. This structure may be
		 * overwritten on subsequent calls to getpwnam.
		 * 
		 * A null pointer return indicates there is no user named name.
		 */
		Pointer getpwnam(String name);

		/*
		 * uid_t geteuid (void)
		 * 
		 * The geteuid function returns the effective user ID of the process.
		 */
		int geteuid();

		/*
		 * struct passwd getpwuid (uid_t uid)
		 * 
		 * This function returns a pointer to a statically-allocated structure
		 * containing information about the user whose user ID is uid. This
		 * structure may be overwritten on subsequent calls to getpwuid.
		 * 
		 * A null pointer value indicates there is no user in the data base with
		 * user ID uid.
		 */

		Pointer getpwuid(int uid);

		/*
		 * int setreuid (uid_t ruid, uid_t euid)
		 */
		int setreuid(int ruid, int euid);

		/*
		 * int chmod (const charfilename, mode_t mode)
		 */
		int chmod(String filename, int mode);

	}

	public void destroy()
	{
		if (_outputStream != null)
			try
			{
				_outputStream.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		if (_inputStream != null)
			try
			{
				_inputStream.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		if (_errorStream != null)
			try
			{
				_errorStream.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			CLibrary.INSTANCE.close(_inPipe[1]);
			CLibrary.INSTANCE.close(_outPipe[0]);
			CLibrary.INSTANCE.close(_errPipe[0]);
			CLibrary.INSTANCE.close(_inPipe[0]);
			CLibrary.INSTANCE.close(_outPipe[1]);
			CLibrary.INSTANCE.close(_errPipe[1]);
	}

	public Collection getChildren()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public int getCurrentPageFaults()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public int getCurrentPhysicalMemory()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public int getCurrentVirtualMemory()
	{
		int result = -1;
		if (!isRunning())
			return result;

		String stat = _utils.readFile("/proc/" + _pid + "/stat");
		// System.out.println("status "+status);
		if (status != null)
			try
			{
				// vsize (23th)
				String sp = "(?:[^\\s]+[\\s]+){22}(\\d+).+";
				Pattern p = Pattern.compile(sp, Pattern.DOTALL);
				Matcher m = p.matcher(stat);
				m.find();
				// get threads
				result = Integer.parseInt(m.group(1).trim());
				}
			catch (Exception ex)
			{
				if (_logger != null)
				_logger.info("Error in getCurrentVirtualMemory() " + ex.getMessage());
			}

		return result;
	}

	public boolean isRunning()
	{
		if (_pid < 1)
			return false;
		return _exitCode < 0;
	}

	public int getExitCode()
	{
		if (_exitCodeKill >= 0)
			return _exitCodeKill;
		return _exitCode;

	}

	public boolean kill(int code)
	{
		if (_logger != null)
			_logger.info("killing " + _pid);
		int count = 0;
		while (_exitCode < 0 && count < 3)
		{
			count++;
			if (_logger != null)
				_logger.info("send kill sig");
			int r = CLibrary.INSTANCE.kill(_pid, CLibrary.SIGKILL);
			if (r == 0)
			{
				_exitCodeKill = code;
				return true;
			}
			else
			{
				if (_logger != null)
					_logger.fine("error calling kill: " + r);
			}
			if (_exitCode < 0)
				try
				{
					Thread.sleep(100);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
					Thread.currentThread().interrupt();
				}
		}
		return false;
	}

	public boolean killTree(int code)
	{
		// TODO Auto-generated method stub
		return false;
	}

	public boolean start()
	{
		if (_arrCmd == null && _cmd == null)
			return false;
		if (_arrCmd == null)
		{
			_arrCmd = _cmd.split(" ");
			log("exec: "+ _cmd);
		}
		else
		{
			String cmd = "";
			for (String c : _arrCmd)
				cmd += c+" ";
			log("exec:"+cmd);
		}


		int pid = 0;
		_exitCode = -2;
		String title = _title == null ? "yajsw" : _title;
		_terminated = false;
		if (_visible)
			setCommand(String.format("xterm -hold -sb -T %1$s -e %2$s", title, getCommand()));

		// System.out.println("exec \n"+getCommand());
		// System.out.println("working dir\n"+getWorkingDir());

		
		if (_visible)
			_pipeStreams = false;

		// if (_pipeStreams)
		{
			CLibrary.INSTANCE.pipe(_inPipe);
			CLibrary.INSTANCE.pipe(_outPipe);
			CLibrary.INSTANCE.pipe(_errPipe);
			// System.out.println(_outPipe[0]+" "+_outPipe[1]);
		}

		// fork a child process
		if ((pid = CLibrary.INSTANCE.fork()) == 0)
		{
			
			int stdout = getStdOutNo();
			int stderr = getStdErrNo();//CLibrary.INSTANCE.fileno(NativeLibrary.getInstance("c").getFunction(getStdErrName()).getPointer(0));
			int stdin = getStdInNo();//CLibrary.INSTANCE.fileno(NativeLibrary.getInstance("c").getFunction(getStdInName()).getPointer(0));

			// pipe streams to OS pipes
			// if (_pipeStreams)
			{
				CLibrary.INSTANCE.close(_inPipe[1]);
				moveDescriptor(_inPipe[0], stdin);
				CLibrary.INSTANCE.close(_outPipe[0]);
				moveDescriptor(_outPipe[1], stdout);
				CLibrary.INSTANCE.close(_errPipe[0]);
				moveDescriptor(_errPipe[1], stderr);
			}
			// closeDescriptors();

			// disconect from parent
			CLibrary.INSTANCE.umask(0);
			if (CLibrary.INSTANCE.setsid() < 0)
				CLibrary.INSTANCE.exit(-1);

			// set working dir
			if (getWorkingDir() != null)
				if (CLibrary.INSTANCE.chdir(getWorkingDir()) != 0)
					if (_logger != null) 
						_logger.info("could not set working dir");

			// set priority
			if (_priority == PRIORITY_BELOW_NORMAL)
			{
				if (CLibrary.INSTANCE.nice(1) == -1)
					if (_logger != null) 
						_logger.info("could not set priority ");
			}
			else if (_priority == PRIORITY_LOW)
			{
				if (CLibrary.INSTANCE.nice(2) == -1)
					if (_logger != null) 
						_logger.info("could not set priority ");
			}
			else if (_priority == PRIORITY_ABOVE_NORMAL)
			{
				if (CLibrary.INSTANCE.nice(-1) == -1)
					if (_logger != null) 
						_logger.info("could not set priority ");
			}
			else if (_priority == PRIORITY_HIGH)
			{
				if (CLibrary.INSTANCE.nice(-2) == -1)
					if (_logger != null) 
						_logger.info("could not set priority ");
			}
			if (getUser() != null)
				switchUser(getUser(), getPassword());

			try
			{
				int res = CLibrary.INSTANCE.execvp(_arrCmd[0], _arrCmd);
				int err = Native.getLastError();
				if (_logger != null) 
					_logger.info("errno " + err + " "+CLibrary.INSTANCE.strerror(err));
				if (_logger != null) 
					_logger.info("exec res "+res);
			}
			catch (Exception ex)
			{
				if (_logger != null)
				_logger.throwing(PosixProcess.class.getName(), "start", ex);
			}
			lock = false;
			// CLibrary.INSTANCE.exit(-1);
		} // child code
		else if (pid > 0)
		{
			_pid = pid;
			try
			{
				Thread.sleep(500);
			}
			catch (InterruptedException e1)
			{
			}
			// or pipe streams to cyclic buffer files
			if (_teeName != null && _tmpPath != null)
			{
				// System.out.println("opening tee streams");
				File f = new File(_tmpPath);
				try
				{
					if (!f.exists())
						f.mkdir();
				}
				catch (Exception ex)
				{
					if (_logger != null)
						_logger.throwing(PosixProcess.class.getName(), "start", ex);
					Thread.currentThread().interrupt();
				}
				try
				{
					// System.out.println("opening tee streams out");
					_inputStream = new CyclicBufferFileInputStream(createRWfile(_tmpPath, "out_" + _teeName));
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				try
				{
					// System.out.println("opening tee streams err");
					_errorStream = new CyclicBufferFileInputStream(createRWfile(_tmpPath, "err_" + _teeName));
				}
				catch (Exception e)
				{
					if (_logger != null)
						_logger.throwing(PosixProcess.class.getName(), "start", e);
				}
				try
				{
					// System.out.println("opening tee streams in");
					_outputStream = new CyclicBufferFilePrintStream(createRWfile(_tmpPath, "in_" + _teeName));
				}
				catch (Exception e)
				{
					if (_logger != null)
						_logger.throwing(PosixProcess.class.getName(), "start", e);
				}
				// System.out.println("- opening tee streams");
			}
			/*
			if (!_pipeStreams)
			{
				 System.out.println("setting out streams to /dev/null/");
				 CLibrary.INSTANCE.freopen("/dev/null", "w", _outPipe[0]);
				 System.out.println("setting err streams to /dev/null/");
				 CLibrary.INSTANCE.freopen("/dev/null", "w", _errPipe[0]);
				 //System.out.println("setting streams to /dev/null/");
				 //CLibrary.INSTANCE.freopen("/dev/null", "r", _inPipe[1]);
				 System.out.println("- setting streams to /dev/null/");
			}
			*/

			// System.out.println("parent");
			if (_pipeStreams && _teeName == null && _tmpPath == null)
			{
				writefd(in_fd, _inPipe[1]);
				writefd(out_fd, _outPipe[0]);
				writefd(err_fd, _errPipe[0]);

				_outputStream = new BufferedOutputStream(new FileOutputStream(in_fd));
				_inputStream = new BufferedInputStream(new FileInputStream(out_fd));
				_errorStream = new BufferedInputStream(new FileInputStream(err_fd));

				CLibrary.INSTANCE.close(_inPipe[0]);
				CLibrary.INSTANCE.close(_outPipe[1]);
				CLibrary.INSTANCE.close(_errPipe[1]);

			}
			if (_cpuAffinity != AFFINITY_UNDEFINED)
			{
				IntByReference affinity = new IntByReference();
				affinity.setValue(_cpuAffinity);
				if (CLibrary.INSTANCE.sched_setaffinity(_pid, 4, affinity) == -1)
					System.out.println("error setting affinity");
			}
			_stopWaiter = true;
			executor.execute(new Runnable()
			{

				public void run()
				{
					int r = CLibrary.INSTANCE.waitpid(_pid, status, 0);
					// System.out.println("wait for "+r);
					if (r == _pid)
						_exitCode = status.getValue();
					if (_logger != null)
						_logger.info("exit code linux process " + _exitCode);
					_terminated = true;
				}

			});

			if (_logger != null)
				_logger.info("started process " + _pid);
			return true;
		} // parent process
		else if (pid < 0)
		{
			if (_logger != null)
				_logger.info("failed to fork: " + pid);
			return false;
		}
		return false;

	}

	protected File createRWfile(String path, String fname) throws IOException
	{
		File result = new File(path, fname);
		if (!result.exists())
		{
			// result.createNewFile();
		}
		String name = result.getCanonicalPath();
		// System.out.println("chmod 777 " + name);
		// Runtime.getRuntime().exec("chmod 777 " + name);
		// int res = CLibrary.INSTANCE.chmod(name, 777);
		// if (res != 0)
		// System.out.println("chmod failed "+res);

		return result;
	}

	public boolean stop(int timeout, int code)
	{
		if (_logger != null)
			_logger.info("killing " + _pid);
		if (!isRunning())
			return true;
		int r = CLibrary.INSTANCE.kill(_pid, CLibrary.SIGTERM);
		waitFor(timeout);
		int count = 0;
		while (isRunning() && count++ < 4)
		{
			CLibrary.INSTANCE.kill(_pid, CLibrary.SIGKILL);
			if (isRunning())
				try
				{
					Thread.sleep(100);
				}
				catch (InterruptedException e)
				{
					if (_logger != null)
						_logger.throwing(PosixProcess.class.getName(), "stop", e);
					Thread.currentThread().interrupt();
				}
		}
		return !isRunning();
	}

	protected void moveDescriptor(int fd_from, int fd_to)
	{
		// System.out.println("move desc "+fd_from+" "+fd_to);
		if (fd_from != fd_to)
		{
			CLibrary.INSTANCE.dup2(fd_from, fd_to);
			CLibrary.INSTANCE.close(fd_from);
		}
	}

	// int
	// closeDescriptors()
	// {
	// Pointer dir;
	// dirent64 dirp;
	// //int from_fd = FAIL_FILENO + 1;
	//
	// /* We're trying to close all file descriptors, but opendir() might
	// * itself be implemented using a file descriptor, and we certainly
	// * don't want to close that while it's in use. We assume that if
	// * opendir() is implemented using a file descriptor, then it uses
	// * the lowest numbered file descriptor, just like open(). So we
	// * close a couple explicitly. */
	//
	// //close(from_fd); /* for possible use by opendir() */
	// //close(from_fd + 1); /* another one for good luck */
	//
	// if ((dir = CLibrary.INSTANCE.opendir("/proc/self/fd")) == null)
	// return 0;
	//	    
	// //System.out.println("dir opened "+dir);
	// //System.out.flush();
	//
	// /* We use readdir64 instead of readdir to work around Solaris bug
	// * 6395699: /proc/self/fd fails to report file descriptors >= 1024 on
	// Solaris 9
	// */
	// while ((dirp = CLibrary.INSTANCE.readdir64(dir)) != null)
	// try
	// {
	// //System.out.println("dir read");
	// //System.out.flush();
	// dirp.read();
	// //System.out.println(dirp.getName());
	// //System.out.flush();
	// String name = dirp.getName();
	// //System.out.println("closing "+name);
	// int fd = Integer.parseInt(name);
	// int r = CLibrary.INSTANCE.close(fd);
	// System.out.println("closing "+name+" "+r);
	// }
	// catch (Exception ex){//ex.printStackTrace();
	// }
	//
	// CLibrary.INSTANCE.closedir(dir);
	//
	// return 1;
	// }

	public void waitFor()
	{
		waitFor(Integer.MAX_VALUE);
	}

	public void waitFor(long timeout)
	{
		long start = System.currentTimeMillis();
		File f = new File("/proc/" + _pid);

		while (System.currentTimeMillis() - start < timeout)
		{
			if (!isRunning() || !f.exists())
				return;
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
				if (_logger != null)
					_logger.throwing(PosixProcess.class.getName(), "waitFor", e);
				Thread.currentThread().interrupt();
			}
		}

	}

	// test
	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException
	{
		PosixProcess[] p = new PosixProcess[1];
		boolean pipe = true;
		for (int i = 0; i < p.length; i++)
		{
			p[i] = new PosixProcess();

			// p[i].setPipeStreams(true, false);
			// p[i].setCommand("xeyes");// "java -cp yajsw.jar
			// org.rzo.yajsw.HelloWorld >
			// t.log");
			// p[i].setCommand("/usr/java/jre1.5.0_10/bin/java -classpath ./bin test.HelloWorld");
			p[i]
					.setCommand("/usr/java/jre1.5.0_10/bin/java -classpath /home/test/rzodyndns/test/wrapper.jar -Dwrapper.config=/home/test/rzodyndns/test/bat/../conf/wrapper.conf -Dwrapper.port=15003 -Dwrapper.key=6566092584194115879 -Dwrapper.teeName=6566092584194115879$1225016378236 -Dwrapper.tmpPath=/tmp org.rzo.yajsw.app.WrapperJVMMain");
			// p[i].setWorkingDir("/home/test/rzodyndns/test/bat/.");
			p[i].setVisible(false);
			// p[i].setPriority(PRIORITY_BELOW_NORMAL);
			// p[i].setCpuAffinity(1);

			p[i].setPipeStreams(pipe, false);
		}
		boolean doit = true;
		while (doit)
		{
			doit = false;
			System.out.println("START");
			// doit = false;
			for (int i = 0; i < p.length; i++)
			{

				p[i].start();
				// p[i].getPid();
				// Runtime.getRuntime().exec(p[i].getCommand());
				System.out.println("started");
				// for (int j=0; i<10000; j++)
				// {
				// System.out.println("b"+j);
				// try
				// {
				// Thread.sleep(00);
				// }
				// catch (InterruptedException e)
				// {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// }
				// }
				// return;
				try
				{
					Thread.yield();
					Thread.sleep(1000);
				}
				catch (InterruptedException e1)
				{
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				if (pipe)
				{
					InputStreamReader isr = new InputStreamReader(p[i].getInputStream());
					System.out.println("in stream " + p[i].getInputStream() + " " + p[i].getInputStream().available());

					BufferedReader br = new BufferedReader(isr);
					String line = "?";
					int k = 0;
					try
					{

						while (k < 10 && (line = br.readLine()) != null)
						{
							System.out.println(line);
							k++;
						}

					}
					catch (Exception e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			p[0].waitFor(1000);
			// System.out.println("exit code "+p[0].getExitCode());
			System.out.println("KILL");

			for (int i = 0; i < p.length; i++)
			{
				// System.out.println(p[i].isRunning());
				p[i].kill(999);
				System.out.println("exit code " + p[i].getExitCode());
				// System.out.println(p[i].isRunning());
				// p[i].destory();
			}
			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// p.setCommand("java -classpath z:\dev\yajsw\wrapper.jar org.rzo." )
	}

	/**
	 * Writefd.
	 * 
	 * @param fd
	 *            the fd
	 * @param pointer
	 *            the pointer
	 */
	protected void writefd(FileDescriptor fd, int pointer)
	{
		try
		{
			// Field[] fields = FileDescriptor.class.getDeclaredFields();
			// System.out.println("fields");
			// for (Field field : fields){
			// System.out.println(field.getName());
			// }
			// System.out.println("writefd");
			Field handleField = FileDescriptor.class.getDeclaredField("fd");
			handleField.setAccessible(true);
			Field peerField = Pointer.class.getDeclaredField("peer");
			peerField.setAccessible(true);
			long value = pointer;// peerField.getLong(pointer);
			// System.out.println(value);
			// System.out.flush();
			handleField.setInt(fd, (int) value);
			// System.out.println(fd.valid());
			// Method sync = FileDescriptor.class.getDeclaredMethod("sync", new
			// Class[0]);
			// sync.setAccessible(true);
			// sync.invoke(fd, new Object[0]);

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}

	public boolean reconnectStreams()
	{
		// TODO Auto-generated method stub
		return false;
	}

	private String getCommandInternal()
	{
		String result = _utils.readFile("/proc/" + getPid() + "/cmdline");
		if (result == null)
			result = "?";
		// System.out.println("cmd line: "+result);
		return result;
	}

	private Map getEnvironmentInternal()
	{
		String result = _utils.readFile("/proc/" + getPid() + "/environ");
		return parseEnvironment(result);
	}

	private Map parseEnvironment(String env)
	{
		Map result = new HashMap();
		if (env == null || "".equals(env))
			return result;
		String sp = "(\\S+)=([^=.]+)( |$)";
		Pattern p = Pattern.compile(sp, Pattern.DOTALL);
		Matcher m = p.matcher(env);
		while (m.find())
		{
			String[] str = m.group().trim().split("=",2);
			if (str.length == 2)
			{
				result.put(str[0],str[1]);
			}
		}
		return result;

	}

	protected String getWorkingDirInternal()
	{
		String result = null;
		File f = new File("/proc/" + getPid() + "/cwd");
		try
		{
			result = f.getCanonicalPath();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return result;

	}

	public String getWorkingDir()
	{
		return _workingDir;
	}

	public static Process getProcess(int pid)
	{
		PosixProcess result = null;
		File f = new File("/proc/" + pid);
		if (f.exists())
		{
			//TODO this may not always work
			result = (PosixProcess) OperatingSystem.instance().processManagerInstance().createProcess();
			result._pid = pid;
			result._user = result.getUserInternal();
			result._cmd = result.getCommandInternal();
			result._workingDir = result.getWorkingDirInternal();
			result._environment = result.getEnvironmentInternal();

		}
		return result;
	}

	public static int currentProcessId()
	{
		return CLibrary.INSTANCE.getpid();
	}

	public  String currentUser()
	{
		int euid = CLibrary.INSTANCE.geteuid();
		Pointer p = CLibrary.INSTANCE.getpwuid(euid);
		if (p == null)
			if (_logger != null)
				_logger.info("could not get current user");
		return new CLibrary.passwd(p).getName();

	}

	public  void switchUser(String name, String password)
	{
		if (_logger != null)
			_logger.info("setting to user " + name);
		if (name == null || "".equals(name))
			return;
		String current = currentUser();
		if (_logger != null)
			_logger.info("current user" + current);
		if (current != null && !current.equals(name))
		{
			Pointer p = CLibrary.INSTANCE.getpwnam(name);
			int newUid = new CLibrary.passwd(p).getUid();
			if (newUid == 0)
				if (_logger != null)
					_logger.info("could not get user " + name);
			int res = CLibrary.INSTANCE.setreuid(newUid, newUid);
			if (res != 0)
				if (_logger != null)
					_logger.info("could not change to user " + name);
			current = currentUser();
			if (!name.equals(current))
				if (_logger != null)
					_logger.info("could not set user");

		}
	}

	public String getUserInternal()
	{
		String status = _utils.readFile("/proc/" + _pid + "/status");
		// System.out.println("status "+status);
		if (status != null)
			try
			{
				// ruid, euid, suid fuid
				String sp = ".*[U|u]id:\\s*(\\d+)\\s*(\\d+)\\s*(\\d+)\\s*(\\d+).*";
				Pattern p = Pattern.compile(sp, Pattern.DOTALL);
				Matcher m = p.matcher(status);
				m.find();
				// get ruid
				int ruid = Integer.parseInt(m.group(1));
				System.out.println("rudi " + ruid);
				Pointer po = CLibrary.INSTANCE.getpwuid(ruid);
				if (po == null)
					if (_logger != null)
						_logger.info("could not get user");
				return new CLibrary.passwd(po).getName().trim();
			}
			catch (Exception ex)
			{
				if (_logger != null)
					_logger.info("Error in getUser() " + ex.getMessage());
			}

		return "";

	}

	public String getUser()
	{
		return _user;
	}

	public String getStdInName()
	{
		return "stdin";
	}

	public String getStdOutName()
	{
		return "stdout";
	}

	public String getStdErrName()
	{
		return "stderr";
	}
	
	public int getStdOutNo()
	{
		return CLibrary.INSTANCE.fileno(NativeLibrary.getInstance("c").getFunction(getStdOutName()).getPointer(0));
	}

	public int getStdErrNo()
	{
		return CLibrary.INSTANCE.fileno(NativeLibrary.getInstance("c").getFunction(getStdErrName()).getPointer(0));
	}

	public int getStdInNo()
	{
		return CLibrary.INSTANCE.fileno(NativeLibrary.getInstance("c").getFunction(getStdInName()).getPointer(0));
	}

	public int getCurrentHandles()
	{
		if (!isRunning())
			return -1;
		File f = new File("/proc/" + _pid + "/fd");
		if (!f.exists() || ! f.isDirectory())
			return -1;
		return f.list().length;
	}

	public int getCurrentThreads()
	{
		int result = -1;
		if (!isRunning())
			return result;
		String status = _utils.readFile("/proc/" + _pid + "/status");
		// System.out.println("status "+status);
		if (status != null)
			try
			{
				// thread count
				String sp = ".*[T|t]hreads:\\s*(\\d+).*";
				Pattern p = Pattern.compile(sp, Pattern.DOTALL);
				Matcher m = p.matcher(status);
				m.find();
				// get threads
				result = Integer.parseInt(m.group(1));
			}
			catch (Exception ex)
			{
				if (_logger != null)
					_logger.info("Error in getCurrentThreads() " + ex.getMessage());
			}

		return result;
	}

	long _currentTotalCPU = -1;
	long _oldTotalCPU = -1;
	long _lastCPUReadTime = Long.MAX_VALUE;
	public int getCurrentCpu()
	{
		int result = -1;
		if (!isRunning())
			return result;

		String stat = _utils.readFile("/proc/" + _pid + "/stat");
		// System.out.println("status "+status);
		if (status != null)
			try
			{
				// ucpu scpu (13th)
				String sp = "(?:[^\\s]+[\\s]+){13}(\\d+)\\s+(\\d+).+";
				Pattern p = Pattern.compile(sp, Pattern.DOTALL);
				Matcher m = p.matcher(stat);
				m.find();
				// get threads
				int ucpu = Integer.parseInt(m.group(1).trim());
				int scpu = Integer.parseInt(m.group(2).trim());
				//System.out.println(ucpu + "<<" + scpu);
				_oldTotalCPU = _currentTotalCPU;
				_currentTotalCPU = ucpu + scpu;
				double elapsed = ((double)(System.currentTimeMillis() - _lastCPUReadTime))/1000;
				double used = _currentTotalCPU - _oldTotalCPU;
				//System.out.println(elapsed + "<<" + used);
				if (elapsed > 0)
					result = (int)(used/elapsed);
				_lastCPUReadTime = System.currentTimeMillis();
				
				}
			catch (Exception ex)
			{
				if (_logger != null)
					_logger.info("Error in getCurrentCPU() " + ex.getMessage());
			}

		return result;
	}

	public boolean isTerminated()
	{
		return _terminated;
	}
	
	public  boolean setWorkingDirectory(String name)
	{
		File f = new File(name);
		String dir;
		if (!f.exists() || !f.isDirectory())
		{
			System.out.println("setWorkingDirectory failed. file not found " +name);
			return false;
		}
		else
			try
			{
				dir = f.getCanonicalPath();
			}
			catch (IOException e)
			{
				if (_logger != null)
					_logger.throwing(PosixProcess.class.getName(), "setWorkingDirectory", e);
				return false;
			}
		boolean result = CLibrary.INSTANCE.chdir(name) == 0;
			if (result)
				System.setProperty("user.dir", dir);
		return result;
	}
	
	public void setTerminated(boolean terminated)
	{
		_terminated = terminated;
	}
	
	@Override
	public void setLogger(Logger logger)
	{
		super.setLogger(logger);
		_utils.setLog(logger);
	}


}
