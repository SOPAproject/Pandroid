package net.sample.pandroid;

import java.io.*;
import java.io.IOException;
import java.util.Arrays;

class sopaOpen
{
	BufferedInputStream bis;
	int length,iChunkSize,iOverlap,iSampleRate;

	public sopaOpen(BufferedInputStream bis)
	{
		this.bis = bis;
	}

	public boolean readHeader()
	{
		final int headerSize = 44;
		DataInputStream dis = new DataInputStream(bis);
		int iNum,iBytesRead;
		int iLong;
		int nTerm0[] = {82,73,70,70};				// RIFF
		int nTerm1[] = {83,79,80,65};				// SOPA
		int nTerm2[] = {102,109,116};				// fmt
		int nTmp[] = new int[4];
		int nFmt[] = new int[3];
		byte[] headerByte = new byte[44];

		try
		{
			iBytesRead = dis.read(headerByte,0,headerSize);
			dis.close();
			if(iBytesRead != headerSize)
				return false;
		}
		catch(IOException e)
		{
			return false;
		}

		for(iNum = 0;iNum < 4;iNum ++)
		{
			nTmp[iNum] = headerByte[iNum];
		}
		if(!Arrays.equals(nTmp,nTerm0))
			return false;
		for(iNum = 0;iNum < 4;iNum ++)
			nTmp[iNum] = headerByte[8 + iNum];
		if(!Arrays.equals(nTmp,nTerm1))
			return false;
		for(iNum = 0;iNum < 3;iNum ++)
			nFmt[iNum] = headerByte[12 + iNum];
		if(!Arrays.equals(nFmt,nTerm2))
			return false;
		if(headerByte[16] != 16)
			return false;
		if(headerByte[20] != 1)
			return false;
		iOverlap = (int)headerByte[22];
		if(iOverlap != 2 && iOverlap != 4)
			return false;
		iSampleRate = (int)headerByte[25] & 0x000000ff;
		iSampleRate *= 256;
		iSampleRate += headerByte[24] & 0xff;
		System.out.println("Sampling rate " + iSampleRate + " Hz");
		for(iNum = 0;iNum < 4;iNum ++)
			nTmp[iNum] = headerByte[36 + iNum];
		System.out.println("SOPA file version " + nTmp[3] + "." + nTmp[2] + "." + nTmp[1] + "." + nTmp[0]);
		iChunkSize = (int)headerByte[43] & 0x000000ff;
		iChunkSize *= 16777216;
		iLong = (int)headerByte[42] & 0x000000ff;
		iLong *= 65536;
		iChunkSize += iLong;
		iLong = (int)headerByte[41] & 0x000000ff;
		iLong *= 256;
		iChunkSize += iLong;
		iChunkSize += (int)headerByte[40] & 0x000000ff;

		System.out.println("Data chunk size " + iChunkSize);
		length = iChunkSize;
		return true;
	}
}