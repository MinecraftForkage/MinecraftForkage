/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.java.decompiler.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class DataInputFullStream extends DataInputStream {

  public DataInputFullStream(byte[] bytes) {
    super(new ByteArrayInputStream(bytes));
  }

  public int readFull(byte[] b) throws IOException {
    int length = b.length;
    byte[] temp = new byte[length];
    int pos = 0;

    int bytes_read;
    while (true) {
      bytes_read = read(temp, 0, length - pos);
      if (bytes_read == -1) {
        return -1;
      }

      System.arraycopy(temp, 0, b, pos, bytes_read);
      pos += bytes_read;
      if (pos == length) {
        break;
      }
    }

    return length;
  }

  public void discard(int n) throws IOException {
    if (super.skip(n) != n) {
      throw new IOException("Skip failed");
    }
  }
}
