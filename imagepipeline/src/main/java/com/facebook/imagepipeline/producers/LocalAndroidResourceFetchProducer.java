/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.util.Log;

import com.facebook.common.internal.VisibleForTesting;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.request.ImageRequest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Executes a local fetch from a resource.
 */
public class LocalAndroidResourceFetchProducer extends LocalFetchProducer {
  @VisibleForTesting static final String PRODUCER_NAME = "LocalAndroidResourceFetchProducer";

  private final PackageManager mPackageManager;

  public LocalAndroidResourceFetchProducer(
      Executor executor,
      PooledByteBufferFactory pooledByteBufferFactory,
      PackageManager packageManager,
      boolean decodeFileDescriptorEnabled) {
    super(executor, pooledByteBufferFactory, decodeFileDescriptorEnabled);
    mPackageManager = packageManager;
  }

  @Override
  protected EncodedImage getEncodedImage(ImageRequest imageRequest) throws IOException {
    Resources resources = getResources(imageRequest);
    return getEncodedImage(
            resources.openRawResource(getResourceId(resources, imageRequest)),
        getLength(imageRequest));
  }

  private int getLength(ImageRequest imageRequest) {
    AssetFileDescriptor fd = null;
    try {
      Resources resources = getResources(imageRequest);
      fd = resources.openRawResourceFd(getResourceId(resources, imageRequest));
      return (int) fd.getLength();
    } catch (Resources.NotFoundException e) {
      return -1;
    } catch (FileNotFoundException e) {
      return -1;
    } finally {
      try {
        if (fd != null) {
          fd.close();
        }
      } catch (IOException ignored) {
        // There's nothing we can do with the exception when closing descriptor.
      }
    }
  }

  @Override
  protected String getProducerName() {
    return PRODUCER_NAME;
  }

  static int getResourceId(Resources resources, ImageRequest data) throws FileNotFoundException {

    String pkg = data.getSourceUri().getAuthority();
    if (pkg == null) throw new FileNotFoundException("No package provided: " + data.getSourceUri());

    int id;
    List<String> segments = data.getSourceUri().getPathSegments();
    if (segments == null || segments.isEmpty()) {
      throw new FileNotFoundException("No path segments: " + data.getSourceUri());
    } else if (segments.size() == 1) {
      try {
        id = Integer.parseInt(segments.get(0));
      } catch (NumberFormatException e) {
        throw new FileNotFoundException("Last path segment is not a resource ID: " + data.getSourceUri());
      }
    } else if (segments.size() == 2) {
      String type = segments.get(0);
      String name = segments.get(1);

      id = resources.getIdentifier(name, type, pkg);
    } else {
      throw new FileNotFoundException("More than two path segments: " + data.getSourceUri());
    }
    return id;
  }

  Resources getResources(ImageRequest data) throws FileNotFoundException {

    String pkg = data.getSourceUri().getAuthority();
    if (pkg == null) throw new FileNotFoundException("No package provided: " + data.getSourceUri());
    try {
      return mPackageManager.getResourcesForApplication(pkg);
    } catch (PackageManager.NameNotFoundException e) {
      throw new FileNotFoundException("Unable to obtain resources for package: " + data.getSourceUri());
    }
  }

}
