// Based on code from Ben Hardill:
// http://www.hardill.me.uk/wordpress/?p=646
package net.zekjur.davsync;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.entity.InputStreamEntity;

public class CountingInputStreamEntity extends InputStreamEntity {
	private UploadListener listener;
	private long length;

	public CountingInputStreamEntity(InputStream instream, long length) {
		super(instream, length);
		this.length = length;
	}

	public void setUploadListener(UploadListener listener) {
		this.listener = listener;
	}

	@Override
	public void writeTo(OutputStream outstream) throws IOException {
		super.writeTo(new CountingOutputStream(outstream));
	}

	class CountingOutputStream extends OutputStream {
		private long counter = 0l;
		private int lastPercent = 0;
		private OutputStream outputStream;

		public CountingOutputStream(OutputStream outputStream) {
			this.outputStream = outputStream;
		}

		@Override
		public void write(int oneByte) throws IOException {
			this.outputStream.write(oneByte);
			counter++;
			if (listener != null) {
				int percent = (int) ((counter * 100) / length);
				// NB: We need to call this only when the percentage actually
				// changed, otherwise updating the notification will churn
				// through memory far too quickly.
				if (lastPercent != percent) {
					listener.onChange(percent);
					lastPercent = percent;
				}
			}
		}
	}

	public interface UploadListener {
		public void onChange(int percent);
	}
}