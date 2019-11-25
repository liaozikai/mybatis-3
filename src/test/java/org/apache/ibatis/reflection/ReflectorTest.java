/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.reflection;

import static org.junit.jupiter.api.Assertions.*;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.apache.ibatis.reflection.invoker.Invoker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.googlecode.catchexception.apis.BDDCatchException.*;
import static org.assertj.core.api.BDDAssertions.then;

class ReflectorTest {

  @Test
  void testGetSetterType() {
    ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
    Reflector reflector = reflectorFactory.findForClass(Section.class);
    Assertions.assertEquals(Long.class, reflector.getSetterType("id"));
  }

  @Test
  void testGetGetterType() {
    ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
    Reflector reflector = reflectorFactory.findForClass(Section.class);
    Assertions.assertEquals(Long.class, reflector.getGetterType("id"));
  }

  @Test
  void shouldNotGetClass() {
    ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
    Reflector reflector = reflectorFactory.findForClass(Section.class);
    Assertions.assertFalse(reflector.hasGetter("class"));
  }

  interface Entity<T> {
    T getId();

    void setId(T id);
  }

  static abstract class AbstractEntity implements Entity<Long> {

    private Long id;

    @Override
    public Long getId() {
      return id;
    }

    @Override
    public void setId(Long id) {
      this.id = id;
    }
  }

  static class Section extends AbstractEntity implements Entity<Long> {
  }

  @Test
  void shouldResolveSetterParam() {
    ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
    Reflector reflector = reflectorFactory.findForClass(Child.class);
    assertEquals(String.class, reflector.getSetterType("id"));
  }

  @Test
  void shouldResolveParameterizedSetterParam() {
    ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
    Reflector reflector = reflectorFactory.findForClass(Child.class);
    assertEquals(List.class, reflector.getSetterType("list"));
  }

  @Test
  void shouldResolveArraySetterParam() {
    ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
    Reflector reflector = reflectorFactory.findForClass(Child.class);
    Class<?> clazz = reflector.getSetterType("array");
    assertTrue(clazz.isArray());
    assertEquals(String.class, clazz.getComponentType());
  }

  @Test
  void shouldResolveGetterType() {
    ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
    Reflector reflector = reflectorFactory.findForClass(Child.class);
    assertEquals(String.class, reflector.getGetterType("id"));
  }

  @Test
  void shouldResolveSetterTypeFromPrivateField() {
    ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
    Reflector reflector = reflectorFactory.findForClass(Child.class);
    assertEquals(String.class, reflector.getSetterType("fld"));
  }

  @Test
  void shouldResolveGetterTypeFromPublicField() {
    ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
    Reflector reflector = reflectorFactory.findForClass(Child.class);
    assertEquals(String.class, reflector.getGetterType("pubFld"));
  }

  @Test
  void shouldResolveParameterizedGetterType() {
    ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
    Reflector reflector = reflectorFactory.findForClass(Child.class);
    assertEquals(List.class, reflector.getGetterType("list"));
  }

  @Test
  void shouldResolveArrayGetterType() {
    ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
    Reflector reflector = reflectorFactory.findForClass(Child.class);
    Class<?> clazz = reflector.getGetterType("array");
    assertTrue(clazz.isArray());
    assertEquals(String.class, clazz.getComponentType());
  }

  static abstract class Parent<T extends Serializable> {
    protected T id;
    protected List<T> list;
    protected T[] array;
    private T fld;
    public T pubFld;

    public T getId() {
      return id;
    }

    public void setId(T id) {
      this.id = id;
    }

    public List<T> getList() {
      return list;
    }

    public void setList(List<T> list) {
      this.list = list;
    }

    public T[] getArray() {
      return array;
    }

    public void setArray(T[] array) {
      this.array = array;
    }

    public T getFld() {
      return fld;
    }
  }

  static class Child extends Parent<String> {
  }

  @Test
  void shouldResoleveReadonlySetterWithOverload() {
    class BeanClass implements BeanInterface<String> {
      @Override
      public void setId(String id) {
        // Do nothing
      }
    }
    ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
    Reflector reflector = reflectorFactory.findForClass(BeanClass.class);
    assertEquals(String.class, reflector.getSetterType("id"));
  }

  interface BeanInterface<T> {
    void setId(T id);
  }

  @Test
  void shouldSettersWithUnrelatedArgTypesThrowException() throws Exception {
    @SuppressWarnings("unused")
    class BeanClass {
      public void setProp1(String arg) {}
      public void setProp2(String arg) {}
      public void setProp2(Integer arg) {}
      public void setProp2(boolean arg) {}
    }
    ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
    Reflector reflector = reflectorFactory.findForClass(BeanClass.class);

    List<String> setableProps = Arrays.asList(reflector.getSetablePropertyNames());
    assertTrue(setableProps.contains("prop1"));
    assertTrue(setableProps.contains("prop2"));
    assertEquals("prop1", reflector.findPropertyName("PROP1"));
    assertEquals("prop2", reflector.findPropertyName("PROP2"));

    assertEquals(String.class, reflector.getSetterType("prop1"));
    assertNotNull(reflector.getSetInvoker("prop1"));

    Class<?> paramType = reflector.getSetterType("prop2");
    assertTrue(String.class.equals(paramType) || Integer.class.equals(paramType));

    Invoker ambiguousInvoker = reflector.getSetInvoker("prop2");
    Object[] param = String.class.equals(paramType)? new String[]{"x"} : new Integer[]{1};
    when(() -> ambiguousInvoker.invoke(new BeanClass(), param));
    then(caughtException()).isInstanceOf(ReflectionException.class)
        .hasMessageMatching(
            "Ambiguous setters defined for property 'prop2' in class '" + BeanClass.class.getName().replace("$", "\\$")
                + "' with types '(java.lang.String|java.lang.Integer|boolean)' and '(java.lang.String|java.lang.Integer|boolean)'\\.");
  }

  @Test
  public void shouldTwoGettersForNonBooleanPropertyThrowException() throws Exception {
    @SuppressWarnings("unused")
    class BeanClass {
      public Integer getProp1() {return 1;}
      public int getProp2() {return 0;}
      public int isProp2() {return 0;}
    }
    // 注意这里，获取BeanClass的反射对象，其中，这个反射对象中的属性已经包含了bean对象的所有属性和方法，如下：
    // type = {Class@1756} "class org.apache.ibatis.reflection.ReflectorTest$3BeanClass"
    //readablePropertyNames = {String[3]@1778}
    // 0 = "prop2"
    // 1 = "prop1"
    // 2 = "this$0"
    //writablePropertyNames = {String[1]@1782}
    //setMethods = {HashMap@1783}  size = 1
    // 0 = {HashMap$Node@1786} "this$0" ->
    //getMethods = {HashMap@1787}  size = 3
    // 0 = {HashMap$Node@1790} "prop2" ->
    // 1 = {HashMap$Node@1795} "prop1" ->
    // 2 = {HashMap$Node@1797} "this$0" ->
    //setTypes = {HashMap@1798}  size = 1
    // 0 = {HashMap$Node@1801} "this$0" -> "class org.apache.ibatis.reflection.ReflectorTest"
    //getTypes = {HashMap@1802}  size = 3
    // 0 = {HashMap$Node@1805} "prop2" -> "int"
    // 1 = {HashMap$Node@1807} "prop1" -> "class java.lang.Integer"
    // 2 = {HashMap$Node@1809} "this$0" -> "class org.apache.ibatis.reflection.ReflectorTest"
    //defaultConstructor = null
    //caseInsensitivePropertyMap = {HashMap@1810}  size = 3
    // 0 = {HashMap$Node@1813} "PROP2" -> "prop2"
    // 1 = {HashMap$Node@1815} "PROP1" -> "prop1"
    // 2 = {HashMap$Node@1817} "THIS$0" -> "this$0"
    ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
    Reflector reflector = reflectorFactory.findForClass(BeanClass.class);

    List<String> getableProps = Arrays.asList(reflector.getGetablePropertyNames());
    assertTrue(getableProps.contains("prop1"));
    assertTrue(getableProps.contains("prop2"));
    assertEquals("prop1", reflector.findPropertyName("PROP1"));
    assertEquals("prop2", reflector.findPropertyName("PROP2"));

    assertEquals(Integer.class, reflector.getGetterType("prop1"));
    // 重点是这里，该方法获得对应属性方法，通过invoke（方法对应类，参数）来执行getProp1方法，这就是反射执行方法的实质；下面是invoke的内容
    // type = {Class@271} "class java.lang.Integer"
    //method = {Method@1784} "public java.lang.Integer org.apache.ibatis.reflection.ReflectorTest$3BeanClass.getProp1()"
    Invoker getInvoker = reflector.getGetInvoker("prop1");
    assertEquals(Integer.valueOf(1), getInvoker.invoke(new BeanClass(), null));

    Class<?> paramType = reflector.getGetterType("prop2");
    assertEquals(int.class, paramType);

    Invoker ambiguousInvoker = reflector.getGetInvoker("prop2");
    when(() -> ambiguousInvoker.invoke(new BeanClass(), new Integer[] {1}));
    then(caughtException()).isInstanceOf(ReflectionException.class)
        .hasMessageContaining("Illegal overloaded getter method with ambiguous type for property 'prop2' in class '"
            + BeanClass.class.getName()
            + "'. This breaks the JavaBeans specification and can cause unpredictable results.");
  }

  @Test
  public void shouldTwoGettersWithDifferentTypesThrowException() throws Exception {
    @SuppressWarnings("unused")
    class BeanClass {
      public Integer getProp1() {return 1;}
      public Integer getProp2() {return 1;}
      public boolean isProp2() {return false;}
    }
    ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
    Reflector reflector = reflectorFactory.findForClass(BeanClass.class);

    List<String> getableProps = Arrays.asList(reflector.getGetablePropertyNames());
    assertTrue(getableProps.contains("prop1"));
    assertTrue(getableProps.contains("prop2"));
    assertEquals("prop1", reflector.findPropertyName("PROP1"));
    assertEquals("prop2", reflector.findPropertyName("PROP2"));

    assertEquals(Integer.class, reflector.getGetterType("prop1"));
    Invoker getInvoker = reflector.getGetInvoker("prop1");
    assertEquals(Integer.valueOf(1), getInvoker.invoke(new BeanClass(), null));

    Class<?> returnType = reflector.getGetterType("prop2");
    assertTrue(Integer.class.equals(returnType) || boolean.class.equals(returnType));

    Invoker ambiguousInvoker = reflector.getGetInvoker("prop2");
    when(() -> ambiguousInvoker.invoke(new BeanClass(), null));
    then(caughtException()).isInstanceOf(ReflectionException.class)
        .hasMessageContaining("Illegal overloaded getter method with ambiguous type for property 'prop2' in class '"
            + BeanClass.class.getName()
            + "'. This breaks the JavaBeans specification and can cause unpredictable results.");
  }

  @Test
  void shouldAllowTwoBooleanGetters() throws Exception {
    @SuppressWarnings("unused")
    class Bean {
      // JavaBean Spec allows this (see #906)
      public boolean isBool() {return true;}
      public boolean getBool() {return false;}
      public void setBool(boolean bool) {}
    }
    ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
    Reflector reflector = reflectorFactory.findForClass(Bean.class);
    assertTrue((Boolean)reflector.getGetInvoker("bool").invoke(new Bean(), new Byte[0]));
  }

  @Test
  void shouldIgnoreBestMatchSetterIfGetterIsAmbiguous() throws Exception {
    @SuppressWarnings("unused")
    class Bean {
      public Integer isBool() {return Integer.valueOf(1);}
      public Integer getBool() {return Integer.valueOf(2);}
      public void setBool(boolean bool) {}
      public void setBool(Integer bool) {}
    }
    ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
    Reflector reflector = reflectorFactory.findForClass(Bean.class);
    Class<?> paramType = reflector.getSetterType("bool");
    Object[] param = boolean.class.equals(paramType) ? new Boolean[] { true } : new Integer[] { 1 };
    Invoker ambiguousInvoker = reflector.getSetInvoker("bool");
    when(() -> ambiguousInvoker.invoke(new Bean(), param));
    then(caughtException()).isInstanceOf(ReflectionException.class)
        .hasMessageMatching(
            "Ambiguous setters defined for property 'bool' in class '" + Bean.class.getName().replace("$", "\\$")
                + "' with types '(java.lang.Integer|boolean)' and '(java.lang.Integer|boolean)'\\.");
  }
}
