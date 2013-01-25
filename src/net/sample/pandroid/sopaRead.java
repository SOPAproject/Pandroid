package net.sample.pandroid;

import java.io.*;
import java.io.IOException;

public class sopaRead extends Thread{
	BufferedInputStream bis;
	byte[] dataByte;
	final int EXT_BUF_SIZE = 8192;
	int length;
	int iLength;
	boolean isFinished = false;
	boolean isTerminated = false;

	public sopaRead(BufferedInputStream bis,byte dataByte[])
	{
		this.bis = bis;
		this.dataByte = dataByte;
	}

	public void run()
	{
		DataInputStream diStream = new DataInputStream(bis);
		int iBytesRead;
		byte[] dummyByte = new byte[44];

		iLength = 0;
		try
		{
			iBytesRead = diStream.read(dummyByte,0,44);
			if(iBytesRead == 44)
			{
				System.out.println("Start");
				iBytesRead = 0;
				while(iBytesRead >= 0 && iLength < length && !isTerminated)
				{
					iBytesRead = diStream.read(dataByte,iLength,Math.min(EXT_BUF_SIZE,length - iLength));
					if(iBytesRead >= 0)
						iLength += iBytesRead;
				}
			}
			diStream.close();
		}
		catch(IOException e)
		{
			System.out.println("error");
			System.exit(1);
		}
		isFinished = true;
	}
}
