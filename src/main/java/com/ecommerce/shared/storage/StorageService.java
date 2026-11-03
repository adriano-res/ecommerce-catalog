package com.ecommerce.shared.storage;

import java.io.InputStream;

/** Core interface abstracting object storage operations across the application. */
public interface StorageService {

  /**
   * Uploads raw binary content to object storage.
   *
   * @param key unique storage object key
   * @param content input stream containing file content
   * @param contentLength total byte size of content
   * @param contentType MIME type of the file
   * @return full public URL referencing the uploaded object
   */
  String upload(String key, InputStream content, long contentLength, String contentType);

  /**
   * Deletes an object from storage by its key.
   *
   * @param key storage object key
   */
  void delete(String key);

  /**
   * Computes the public URL for a given storage object key.
   *
   * @param key storage object key
   * @return canonical public URL
   */
  String getPublicUrl(String key);
}
