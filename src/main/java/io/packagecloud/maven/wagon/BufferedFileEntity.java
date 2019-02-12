package io.packagecloud.maven.wagon;

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.util.Args;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.util.Args;
import java.io.*;

public class BufferedFileEntity extends AbstractHttpEntity implements Cloneable {

  protected final File file;

  /**
   * Creates a new instance.
   *
   * @param file The file to serve.
   * @param contentType  The content type for the given {@code file}.
   *
   */
  @Deprecated
  public BufferedFileEntity(final File file, final String contentType) {
    super();
    this.file = Args.notNull(file, "File");
    setContentType(contentType);
  }

  /**
   * Creates a new instance.
   *
   * @param file The file to serve.
   * @param contentType  The content type for the given {@code file}.
   *
   * @since 4.2
   */
  public BufferedFileEntity(final File file, final ContentType contentType) {
    super();
    this.file = Args.notNull(file, "File");
    if (contentType != null) {
      setContentType(contentType.toString());
    }
  }

  /**
   * Creates a new instance.
   *
   * @param file The file to serve.
   *
   * @since 4.2
   */
  public BufferedFileEntity(final File file) {
    super();
    this.file = Args.notNull(file, "File");
  }

  @Override
  public boolean isRepeatable() {
    return true;
  }

  @Override
  public long getContentLength() {
    return this.file.length();
  }

  @Override
  public InputStream getContent() throws IOException {
    return new FileInputStream(this.file);
  }

  @Override
  public void writeTo(final OutputStream outStream) throws IOException {
    final InputStream inStream = new BufferedInputStream(new FileInputStream(this.file));
    try {
      System.out.println("hi");
      IOUtils.copy(inStream, outStream, OUTPUT_BUFFER_SIZE);
      outStream.flush();
    }  finally {
      inStream.close();
    }
  }

  /**
   * Tells that this entity is not streaming.
   *
   * @return {@code false}
   */
  @Override
  public boolean isStreaming() {
    return false;
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    // File instance is considered immutable
    // No need to make a copy of it
    return super.clone();
  }

} // class FileEntity
