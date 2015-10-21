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
package org.jetbrains.java.decompiler.struct.gen;

public class FieldDescriptor {

  public static final FieldDescriptor INTEGER_DESCRIPTOR = parseDescriptor("Ljava/lang/Integer;");
  public static final FieldDescriptor LONG_DESCRIPTOR = parseDescriptor("Ljava/lang/Long;");
  public static final FieldDescriptor FLOAT_DESCRIPTOR = parseDescriptor("Ljava/lang/Float;");
  public static final FieldDescriptor DOUBLE_DESCRIPTOR = parseDescriptor("Ljava/lang/Double;");

  public VarType type;

  public String descriptorString;

  private FieldDescriptor() {
  }

  public static FieldDescriptor parseDescriptor(String descr) {

    FieldDescriptor fd = new FieldDescriptor();

    fd.type = new VarType(descr);
    fd.descriptorString = descr;

    return fd;
  }

  public String getDescriptor() {
    return type.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (o == null || !(o instanceof FieldDescriptor)) return false;

    FieldDescriptor fd = (FieldDescriptor)o;
    return type.equals(fd.type);
  }

  @Override
  public int hashCode() {
    return type.hashCode();
  }
}
