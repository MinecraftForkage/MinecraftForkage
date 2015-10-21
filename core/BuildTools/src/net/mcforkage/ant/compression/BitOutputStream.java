package net.mcforkage.ant.compression;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

// LSB first
public class BitOutputStream extends FilterOutputStream {
	
	public BitOutputStream(OutputStream s) {
		super(s);
	}
	
	private int nbits;
	private int accumbits;
	
	public void write(boolean b) throws IOException {
		if(b) accumbits |= (1 << nbits);
		nbits++;
		if(nbits == 8) {
			super.write(accumbits);
			nbits = 0;
			accumbits = 0;
		}
	}
	
	@Override
	public void write(byte[] ba) throws IOException {
		for(byte b : ba)
			write(b);
	}
	
	@Override
	public void write(byte[] ba, int off, int len) throws IOException {
		for(int k = off; k < off + len; k++)
			write(ba[k]);
	}
	
	@Override
	public void write(int b) throws IOException {
		for(int k = 0; k < 8; k++)
			write((b & (1 << k)) != 0);
	}
	
	public void write(boolean[] ba) throws IOException {
		for(boolean b : ba)
			write(b);
	}
	
	public void close() throws IOException {
		padToByte();
		super.close();
	}

	public void padToByte() throws IOException {
		while(nbits != 0)
			write(false);
	}
}
