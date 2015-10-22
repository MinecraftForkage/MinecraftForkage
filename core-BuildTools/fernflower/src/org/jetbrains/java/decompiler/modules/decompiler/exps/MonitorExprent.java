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
package org.jetbrains.java.decompiler.modules.decompiler.exps;

import org.jetbrains.java.decompiler.main.TextBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.java.decompiler.main.collectors.BytecodeMappingTracer;
import org.jetbrains.java.decompiler.util.InterpreterUtil;


public class MonitorExprent extends Exprent {

  public static final int MONITOR_ENTER = 0;
  public static final int MONITOR_EXIT = 1;

  private int montype;

  private Exprent value;

  {
    this.type = EXPRENT_MONITOR;
  }

  public MonitorExprent(int montype, Exprent value, Set<Integer> bytecode_offsets) {
    this.montype = montype;
    this.value = value;

    addBytecodeOffsets(bytecode_offsets);
  }

  @Override
  public Exprent copy() {
    return new MonitorExprent(montype, value.copy(), bytecode);
  }

  public List<Exprent> getAllExprents() {
    List<Exprent> lst = new ArrayList<Exprent>();
    lst.add(value);
    return lst;
  }

  @Override
  public TextBuffer toJava(int indent, BytecodeMappingTracer tracer) {

    tracer.addMapping(bytecode);

    if (montype == MONITOR_ENTER) {
      return value.toJava(indent, tracer).enclose("synchronized(", ")");
    }
    else {
      return new TextBuffer();
    }
  }

  public boolean equals(Object o) {
    if (o == this) return true;
    if (o == null || !(o instanceof MonitorExprent)) return false;

    MonitorExprent me = (MonitorExprent)o;
    return montype == me.getMontype() &&
           InterpreterUtil.equalObjects(value, me.getValue());
  }

  public void replaceExprent(Exprent oldexpr, Exprent newexpr) {
    if (oldexpr == value) {
      value = newexpr;
    }
  }

  public int getMontype() {
    return montype;
  }

  public Exprent getValue() {
    return value;
  }
}
