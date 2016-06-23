package org.rzo.yajsw.tray;

public class ByteFormat
{
	// taken from
	// http://groups.google.com/group/comp.lang.java.help/browse_thread/thread/0db818517ca9de79/b0a55aa19f911204
	// thanks to Piotr Kobzda
	public enum StorageUnit
	{
		BYTE("B", 1L), KILOBYTE("KB", 1L << 10), MEGABYTE("MB", 1L << 20), GIGABYTE("GB", 1L << 30), TERABYTE("TB", 1L << 40), PETABYTE("PB",
				1L << 50), EXABYTE("EB", 1L << 60);

		public static final StorageUnit	BASE	= BYTE;

		private final String			symbol;
		private final long				divider;		// divider of BASE unit

		StorageUnit(String name, long divider)
		{
			this.symbol = name;
			this.divider = divider;
		}

		public static StorageUnit of(final long number)
		{
			final long n = number > 0 ? -number : number;
			if (n > -(1L << 10))
			{
				return BYTE;
			}
			else if (n > -(1L << 20))
			{
				return KILOBYTE;
			}
			else if (n > -(1L << 30))
			{
				return MEGABYTE;
			}
			else if (n > -(1L << 40))
			{
				return GIGABYTE;
			}
			else if (n > -(1L << 50))
			{
				return TERABYTE;
			}
			else if (n > -(1L << 60))
			{
				return PETABYTE;
			}
			else
			{ // n >= Long.MIN_VALUE
				return EXABYTE;
			}
		}
	}

	public String format(long number)
	{
		StorageUnit st = StorageUnit.of(number);
		return nf.format((double) number / st.divider) + " " + st.symbol;
	}

	private static java.text.NumberFormat	nf	= java.text.NumberFormat.getInstance();
	static
	{
		nf.setGroupingUsed(false);
		nf.setMinimumFractionDigits(0);
		nf.setMaximumFractionDigits(1);
	}

}
