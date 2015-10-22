package net.mcforkage.ant.compression;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

// LSB first
public class BitInputStream extends FilterInputStream {
	
	public BitInputStream(InputStream s) {
		super(s);
	}
	
	private int nbits;
	private int accumbits;
	
	public boolean readBit() throws IOException {
		if(nbits == 0) {
			accumbits = super.read();
			if(accumbits == -1)
				throw new EOFException(); 
			nbits = 8;
		}
		nbits--;
		boolean result = (accumbits & 1) != 0;
		accumbits >>= 1;
		return result;
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		for(int k = off; k < off + len; k++)
			b[k] = (byte)read();
		return len;
	}
	
	@Override
	public int read() throws IOException {
		int rv = 0;
		for(int k = 0; k < 8; k++)
			if(readBit())
				rv |= 1 << k;
		return rv;
	}
	
	public void read(boolean[] ba) throws IOException {
		for(int k = 0; k < ba.length; k++)
			ba[k] = readBit();
	}

	public void padToByte() throws IOException {
		while(nbits != 0 && nbits != 8)
			readBit();
	}
}
