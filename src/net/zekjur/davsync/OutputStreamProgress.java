package net.zekjur.davsync;

import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamProgress extends OutputStream {

	private final OutputStream outstream;
	private volatile long bytesWritten=0;
	
	public OutputStreamProgress(OutputStream outstream) {
		this.outstream = outstream;
	}
	
	@Override
	public void write(int b) throws IOException {
		outstream.write(b);
		bytesWritten++;
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		outstream.write(b);
		bytesWritten += b.length;
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		outstream.write(b, off, len);
		bytesWritten += len;
	}
	
	@Override
	public void flush() throws IOException {
		outstream.flush();
	}
	
	@Override
	public void close() throws IOException {
		outstream.close();
	}
	
	public long getWrittenLength() {
		return bytesWritten;
	}
}
