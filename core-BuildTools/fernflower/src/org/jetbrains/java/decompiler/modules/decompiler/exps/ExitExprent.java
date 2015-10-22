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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.ClassesProcessor.ClassNode;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.TextBuffer;
import org.jetbrains.java.decompiler.main.collectors.BytecodeMappingTracer;
import org.jetbrains.java.decompiler.main.rels.MethodWrapper;
import org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor;
import org.jetbrains.java.decompiler.modules.decompiler.vars.CheckTypesResult;
import org.jetbrains.java.decompiler.struct.attr.StructExceptionsAttribute;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.util.InterpreterUtil;


public class ExitExprent extends Exprent {

  public static final int EXIT_RETURN = 0;
  public static final int EXIT_THROW = 1;

  // return or throw statement
  private int exittype;

  private Exprent value;

  private VarType rettype;

  {
    this.type = EXPRENT_EXIT;
  }

  public ExitExprent(int exittype, Exprent value, VarType rettype, Set<Integer> bytecode_offsets) {
    this.exittype = exittype;
    this.value = value;
    this.rettype = rettype;

    addBytecodeOffsets(bytecode_offsets);
  }

  @Override
  public Exprent copy() {
    return new ExitExprent(exittype, value == null ? null : value.copy(), rettype, bytecode);
  }

  public CheckTypesResult checkExprTypeBounds() {
    CheckTypesResult result = new CheckTypesResult();

    if (exittype == EXIT_RETURN && rettype.type != CodeConstants.TYPE_VOID) {
      result.addMinTypeExprent(value, VarType.getMinTypeInFamily(rettype.type_family));
      result.addMaxTypeExprent(value, rettype);
    }

    return result;
  }

  public List<Exprent> getAllExprents() {
    List<Exprent> lst = new ArrayList<Exprent>();
    if (value != null) {
      lst.add(value);
    }
    return lst;
  }

  @Override
  public TextBuffer toJava(int indent, BytecodeMappingTracer tracer) {

    tracer.addMapping(bytecode);

    if (exittype == EXIT_RETURN) {
      TextBuffer buffer = new TextBuffer();

      if (rettype.type != CodeConstants.TYPE_VOID) {
        buffer.append(" ");
        ExprProcessor.getCastedExprent(value, rettype, buffer, indent, false, tracer);
      }

      return buffer.prepend("return");
    }
    else {

      MethodWrapper meth = (MethodWrapper)DecompilerContext.getProperty(DecompilerContext.CURRENT_METHOD_WRAPPER);
      ClassNode node = ((ClassNode)DecompilerContext.getProperty(DecompilerContext.CURRENT_CLASS_NODE));

      if (meth != null && node != null) {
        StructExceptionsAttribute attr = (StructExceptionsAttribute)meth.methodStruct.getAttributes().getWithKey("Exceptions");

        if (attr != null) {
          String classname = null;

          for (int i = 0; i < attr.getThrowsExceptions().size(); i++) {
            String excclassname = attr.getExcClassname(i, node.classStruct.getPool());
            if ("java/lang/Throwable".equals(excclassname)) {
              classname = excclassname;
              break;
            }
            else if ("java/lang/Exception".equals(excclassname)) {
              classname = excclassname;
            }
          }

          if (classname != null) {
            VarType exctype = new VarType(classname, true);

            TextBuffer buffer = new TextBuffer();
            ExprProcessor.getCastedExprent(value, exctype, buffer, indent, false, tracer);

            return buffer.prepend("throw ");
          }
        }
      }

      return value.toJava(indent, tracer).prepend("throw ");
    }
  }

  public boolean equals(Object o) {
    if (o == this) return true;
    if (o == null || !(o instanceof ExitExprent)) return false;

    ExitExprent et = (ExitExprent)o;
    return exittype == et.getExittype() &&
           InterpreterUtil.equalObjects(value, et.getValue());
  }

  public void replaceExprent(Exprent oldexpr, Exprent newexpr) {
    if (oldexpr == value) {
      value = newexpr;
    }
  }

  public int getExittype() {
    return exittype;
  }

  public Exprent getValue() {
    return value;
  }

  public VarType getRettype() {
    return rettype;
  }
}
