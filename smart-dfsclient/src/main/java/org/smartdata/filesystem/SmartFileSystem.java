/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartdata.filesystem;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.smartdata.metrics.FileAccessEvent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;

public class SmartFileSystem extends DistributedFileSystem{

  SmartClient smartClient;
  InetSocketAddress smartServerAddress;

  public SmartFileSystem() {
  }

  @Override
  public void initialize(URI uri, Configuration conf) throws IOException {
    super.initialize(uri, conf);

    String smartServerIp = conf.get("smart.server.rpc.address", "127.0.0.1");
    int smartServerPort = conf.getInt("smart.server.rpc.port", 7042);

    try {
      smartServerAddress = new InetSocketAddress(smartServerIp, smartServerPort);
    } catch (Exception e){
      try {
        super.close();
      } catch (Throwable e1) {
        // DO nothing now
      }
      throw new IOException("Cannot parse smart server rpc address or port, address: " + smartServerIp + ", port:" +  smartServerPort);
    }
    this.smartClient = new SmartClient(conf, smartServerAddress);
  }

  @Override
  public FSDataInputStream open(Path f, final int bufferSize) throws IOException {
    FSDataInputStream inputStream = super.open(f, bufferSize);
    this.statistics.incrementReadOps(1);
    reportFileAccessEvent(f.getName());
    return inputStream;
  }

  private void reportFileAccessEvent(String src) {
    try {
      smartClient.reportFileAccessEvent(new FileAccessEvent(src));
    } catch (IOException e) {  // Here just ignores that failed to report
      e.printStackTrace();
      LOG.error("Can not report file access event to SmartServer: " + src);
    }
  }

  public void close() throws IOException {
    try {
      super.close();
    } finally {
      this.smartClient.close();
    }
  }
}
