package net.zekjur.davsync;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.entity.AbstractHttpEntity;

/**
* File entity which supports a progress bar.<br/>
* Based on "org.apache.http.entity.FileEntity".
* @author Benny Neugebauer (www.bennyn.de)
*/
public class FileEntityWithProgressBar extends AbstractHttpEntity implements Cloneable
{
	
	protected final File file;
	private ProgressBarListener listener;
	private long transferredBytes;
	
	public FileEntityWithProgressBar(final File file, final String contentType)
	{
		super();
		if (file == null)
		{
			throw new IllegalArgumentException("File may not be null");
		}
		this.file = file;
		//this.listener = listener;
		this.transferredBytes = 0;
		setContentType(contentType);
	}
	
	public void setUploadListener(ProgressBarListener listener) {
		this.listener = listener;
	}
	
	public boolean isRepeatable()
	{
		return true;
	}
	
	public long getContentLength()
	{
		return this.file.length();
	}
	
	public InputStream getContent() throws IOException
	{
		return new FileInputStream(this.file);
	}
	
	public void writeTo(final OutputStream outstream) throws IOException
	{
		if (outstream == null)
		{
			throw new IllegalArgumentException("Output stream may not be null");
		}
		InputStream instream = new FileInputStream(this.file);
		try
		{
			byte[] tmp = new byte[4096];
			int l;
			while ((l = instream.read(tmp)) != -1)
			{
				outstream.write(tmp, 0, l);
				this.transferredBytes += l;
				int percent = (int) ((float)this.transferredBytes / (float)this.file.length() / 3 * 100);
				this.listener.updateTransferred(percent);
			}
			outstream.flush();
		}
		finally
		{
			instream.close();
		}
	}
	
	public boolean isStreaming()
	{
		return false;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException
	{
		return super.clone();
	}
}



