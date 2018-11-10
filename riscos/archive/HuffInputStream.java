package riscos.archive;

import java.io.FilterInputStream;
import java.io.*;

public class HuffInputStream extends FilterInputStream
{
	private class HuffTree
	{
		private short leftix;
		private short rightix;

		public void setLeftIx(short i)
		{
			leftix = i;
		}

		public void setRightIx(short i)
		{
			rightix = i;
		}

		public short getChild(int direction)
		{
			if ((direction & 1) == 0)
			{
				return leftix;
			}

			return rightix;
		}

	}
		
	private InputStream is;
	private int curin;
	private int bpos;
	private HuffTree t[];
	private static final int SPEOF = 256;
	private boolean gotHuffTree;
	private boolean atEof;

	public HuffInputStream(InputStream in)
	{
		super(in);

		is = in;
		bpos = 99;
		gotHuffTree = false;
		atEof = false;
	}

	public short
	read16() throws IOException
	{
		char c = (char)is.read();
		char c1 = (char)is.read();

		return (short)(c | (c1 << 8));
	}

	public void
	get_hdr() throws IOException
	{
		if (!gotHuffTree)
		{
			short magic = read16();
			short crc = read16();
			String s = new String();

			byte b;
			do
			{
				b = (byte)is.read();
				if (b != 0)
				{
					s = s + (char)b;
				}
			} while (b != 0);

			System.out.println("magic = " + Integer.toHexString(magic));
			System.out.println("crc = " + Integer.toHexString(crc));
			System.out.println("Orig name = " + s);

			read_huff_tree();
		}
	}

	public void
	read_huff_tree() throws IOException
	{
		if (!gotHuffTree)
		{
			short numnodes = read16();

			t = new HuffTree[numnodes];
			for (int i = 0; i < numnodes; i++)
			{
				t[i] = new HuffTree();
				short r = read16();
				t[i].setLeftIx(r);
				r = read16();
				t[i].setRightIx(r);
			}
			gotHuffTree = true;
		}
	}

	public int
	gethuff() throws IOException
	{
		int i = 0;

		if (atEof)
		{
			return -1;
		}
		do
		{
			if (++bpos > 7)
			{
				curin = is.read();
				if (curin == -1)
				{
					return -1;
				}
				bpos = 0;
				i = t[i].getChild(curin);
			}
			else
			{
				curin >>>= 1;
				i = t[i].getChild(curin);
			}
		} while (i >= 0);

		i = -(i+1);

		if (i == SPEOF)
		{
			atEof = true;
			i = -1;
		}

		return i;
	}

	public int read() throws IOException
	{
		read_huff_tree();

		return gethuff();
	}

	public int read(byte buf[], int off, int len) throws IOException
	{
		int i = 0;

		read_huff_tree();

		while (i < off)
		{
			int b = gethuff();
			if (b == -1)
				break;
		}

		i = 0;
		while (i < len)
		{
			int b = gethuff();
			if (b == -1)
			{
				if (i == 0)
				{
					i = -1;
				}
				break;
			}
			buf[i++] = (byte)b;
		}

		return i;
	}
/*
public static void
main(String args[])
{
	rd r = new rd();
	
	try
	{
		r.get_hdr();
		r.read_huff();
		int i = r.gethuff();
		System.out.println("Got char " + (char)i);
	}
	catch (IOException e)
	{
		System.out.println("Failed to read");
	}
}
*/

}
