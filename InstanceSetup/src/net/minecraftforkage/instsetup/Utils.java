package net.minecraftforkage.instsetup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

class Utils {
	public static void copyStream(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[32768];
		while(true) {
			int read = in.read(buffer);
			if(read <= 0)
				break;
			out.write(buffer, 0, read);
		}
	}
	
	public static byte[] readStream(InputStream in) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		copyStream(in, baos);
		return baos.toByteArray();
	}
	
	
	/**
	 * Runs mainTask. If it has not completed after timeoutMS milliseconds (approximately),
	 * run timeoutTask on a new thread.
	 */
	public static void runWithTimeout(Runnable mainTask, long timeoutMS, final Runnable timeoutTask) {
		
		final Object monitor = new Object();
		final AtomicBoolean hasFinished = new AtomicBoolean();
		final long timeoutTime = System.nanoTime() + timeoutMS * 1000000L;
		
		hasFinished.set(false);
		
		new Thread() {
			@Override
			public void run() {
				synchronized(monitor) {
					while(!hasFinished.get()) {
						int remaining = (int)((timeoutTime - System.nanoTime()) / 1000000);
						if(remaining <= 0)
							break;
						try {
							monitor.wait(remaining);
						} catch(InterruptedException e) {
							e.printStackTrace();
							return;
						}
					}
				}
				
				if(!hasFinished.get())
					timeoutTask.run();
			}
		}.start();
		
		try {
			mainTask.run();
		} finally {
			hasFinished.set(true);
			synchronized(monitor) {
				monitor.notifyAll();
			}
		}
	}
}
