/*
 * Copyright 2014 xinjunli (micromagic@sina.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tool;

import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

import static tool.PrivateAccessor.*;

public class PrivateAccessorTest extends TestCase
{
	public void testMemberDifferent()
			throws Exception
	{
		Field f1 = PrivateAccessor.class.getDeclaredField("ML");
		boolean old = f1.isAccessible();
		f1.setAccessible(!old);
		Field f2 = PrivateAccessor.class.getDeclaredField("ML");
		assertFalse(f1 == f2);
		assertFalse(f1.isAccessible() == f2.isAccessible());
	}

	public void testGet()
			throws Exception
	{
		Object obj = new B();
		assertEquals(Integer.valueOf(2), get(obj, "i"));
		assertEquals(Integer.valueOf(1), get(obj, A.class, "i"));
	}

	public void testSet()
			throws Exception
	{
		B obj = new B();
		set(obj, "i", Integer.valueOf(-2));
		set(obj, A.class, "i", Integer.valueOf(-1));
		assertEquals(-2, obj.getI2());
		assertEquals(-1, obj.getI1());
	}

	@SuppressWarnings({"rawtypes"})
	public void testCheckTypes()
			throws Exception
	{
		Class[] types;
		Object[] params;
		Object obj;

		// match 0  Integer => int
		types = new Class[]{int.class};
		params = new Object[]{Integer.valueOf(1)};
		obj = invoke(PrivateAccessor.class, "checkTypes", types, params);
		assertEquals(Integer.valueOf(0), obj);

		// match 2  Byte => int
		types = new Class[]{int.class};
		params = new Object[]{Byte.valueOf((byte) 1)};
		obj = invoke(PrivateAccessor.class, "checkTypes", types, params);
		assertEquals(Integer.valueOf(2), obj);

		// match 3  Byte, Integer => int, long
		types = new Class[]{int.class, long.class};
		params = new Object[]{Byte.valueOf((byte) 1), Integer.valueOf(1)};
		obj = invoke(PrivateAccessor.class, "checkTypes", types, params);
		assertEquals(Integer.valueOf(3), obj);

		// match 1  Integer, Integer => int, long
		types = new Class[]{int.class, long.class};
		params = new Object[]{Integer.valueOf(1), Integer.valueOf(1)};
		obj = invoke(PrivateAccessor.class, "checkTypes", types, params);
		assertEquals(Integer.valueOf(1), obj);

		// match 6  String => Object
		types = new Class[]{Object.class};
		params = new Object[]{""};
		obj = invoke(PrivateAccessor.class, "checkTypes", types, params);
		assertEquals(Integer.valueOf(6), obj);

		// match 2  Character => Comparable
		types = new Class[]{Comparable.class};
		params = new Object[]{Character.valueOf('c')};
		obj = invoke(PrivateAccessor.class, "checkTypes", types, params);
		assertEquals(Integer.valueOf(2), obj);

		// match 4  C => Iterable
		types = new Class[]{Iterable.class};
		params = new Object[]{new C()};
		obj = invoke(PrivateAccessor.class, "checkTypes", types, params);
		assertEquals(Integer.valueOf(4), obj);

		// match 3  C => Collection
		types = new Class[]{Collection.class};
		params = new Object[]{new C()};
		obj = invoke(PrivateAccessor.class, "checkTypes", types, params);
		assertEquals(Integer.valueOf(3), obj);

		// match 2  C => AbstractList
		types = new Class[]{AbstractList.class};
		params = new Object[]{new C()};
		obj = invoke(PrivateAccessor.class, "checkTypes", types, params);
		assertEquals(Integer.valueOf(2), obj);

		// match 1  C => ArrayList
		types = new Class[]{ArrayList.class};
		params = new Object[]{new C()};
		obj = invoke(PrivateAccessor.class, "checkTypes", types, params);
		assertEquals(Integer.valueOf(1), obj);

		// match 2  C => List
		types = new Class[]{List.class};
		params = new Object[]{new C()};
		obj = invoke(PrivateAccessor.class, "checkTypes", types, params);
		assertEquals(Integer.valueOf(2), obj);
	}

	public void testGetMethod()
			throws Exception
	{
		Object[] params;
		Object obj;

		// Byte Byte => none
		params = new Object[]{Byte.valueOf((byte) 1), Byte.valueOf((byte) 1)};
		obj = invoke(PrivateAccessor.class, "getMethod",
				this.getClass(), "test1", false, params);
		assertNull(obj);

		// Byte Byte => void test1(byte a, short b)
		params = new Object[]{Byte.valueOf((byte) 1), Byte.valueOf((byte) 1)};
		obj = invoke(PrivateAccessor.class, "getMethod",
				B.class, "test1", false, params);
		assertEquals(1, ((MethodContainer) obj).match);
		assertEquals(short.class, ((MethodContainer) obj).method.getParameterTypes()[1]);

		// Byte Short => void test1(byte a, short b)
		params = new Object[]{Byte.valueOf((byte) 1), Short.valueOf((short) 1)};
		obj = invoke(PrivateAccessor.class, "getMethod",
				B.class, "test1", false, params);
		assertEquals(0, ((MethodContainer) obj).match);
		assertEquals(short.class, ((MethodContainer) obj).method.getParameterTypes()[1]);

		// Byte Integer => void test1(byte a, int b)
		params = new Object[]{Byte.valueOf((byte) 1), Integer.valueOf(1)};
		obj = invoke(PrivateAccessor.class, "getMethod",
				B.class, "test1", false, params);
		assertEquals(0, ((MethodContainer) obj).match);
		assertEquals(int.class, ((MethodContainer) obj).method.getParameterTypes()[1]);

		// null Integer => void test1(String str, int b)
		params = new Object[]{null, Integer.valueOf(1)};
		obj = invoke(PrivateAccessor.class, "getMethod",
				B.class, "test1", false, params);
		assertEquals(0, ((MethodContainer) obj).match);

		// (String) null => void test1(String str)
		params = new Object[]{cast(String.class, null)};
		obj = invoke(PrivateAccessor.class, "getMethod",
				B.class, "test1", false, params);
		assertEquals(0, ((MethodContainer) obj).match);
		assertEquals(String.class, ((MethodContainer) obj).method.getParameterTypes()[0]);

		// (Object) null => void test1(Object obj)
		params = new Object[]{cast(Object.class, null)};
		obj = invoke(PrivateAccessor.class, "getMethod",
				B.class, "test1", false, params);
		assertEquals(0, ((MethodContainer) obj).match);
		assertEquals(Object.class, ((MethodContainer) obj).method.getParameterTypes()[0]);

		// Character => void test1(Comparable obj)
		params = new Object[]{Character.valueOf('c')};
		obj = invoke(PrivateAccessor.class, "getMethod",
				B.class, "test1", false, params);
		assertEquals(2, ((MethodContainer) obj).match);
		assertEquals(Comparable.class, ((MethodContainer) obj).method.getParameterTypes()[0]);
		
		// B => void test1(Object obj)
		params = new Object[]{new B()};
		obj = invoke(PrivateAccessor.class, "getMethod",
				B.class, "test1", false, params);
		assertEquals(7, ((MethodContainer) obj).match);
		assertEquals(Object.class, ((MethodContainer) obj).method.getParameterTypes()[0]);

		// C => void test1(Collection obj)
		params = new Object[]{new C()};
		obj = invoke(PrivateAccessor.class, "getMethod",
				B.class, "test1", false, params);
		assertEquals(3, ((MethodContainer) obj).match);
		assertEquals(Collection.class, ((MethodContainer) obj).method.getParameterTypes()[0]);
	}

	public void testInvoke()
			throws Exception
	{
		B obj = new B();
		invoke(obj, "setI", Integer.valueOf(10));
		invoke(obj, A.class, "setI", Integer.valueOf(100));
		assertEquals(10, obj.getI2());
		assertEquals(100, obj.getI1());

		invoke(obj, "test1", "", Byte.valueOf((byte) 15));
		assertEquals(15, obj.getI2());
		invoke(obj, "test1", "", cast(int.class, Long.valueOf(16L)));
		assertEquals(16, obj.getI2());
		invoke(obj, "test1", new C(), cast(char.class, Long.valueOf(17L)));
		assertEquals(17, obj.getI1());
	}

	public void testCreate()
			throws Exception
	{
		A a = (A) create(A.class, "", null);
		assertEquals(a.getI1(), 111);
		a = (A) create(A.class, null, new C());
		assertEquals(a.getI1(), 222);
		a = (A) create(A.class, cast(String.class, null), new C());
		assertEquals(a.getI1(), 111);
	}


	void test1(short a, int[] b) {}

}

@SuppressWarnings("rawtypes")
class A
{
	public A() {}
	public A(String a, Collection b)
	{
		this.i = 111;
	}
	public A(Object a, AbstractList b)
	{
		this.i = 222;
	}
	
	private int i = 1;
	public int getI1()
	{
		return this.i;
	}
	@SuppressWarnings("unused")
	private void setI(int i)
	{
		this.i = i;
	}

	void test1(Comparable obj) {}
	void test1(Iterable obj) {}
	void test1(Collection obj) {}

	void test1(byte a, short b) {}

	void test1(Iterable obj, char b)
	{
		this.i = b;
	}

}

class B extends A
{
	private int i = 2;
	public int getI2()
	{
		return this.i;
	}
	@SuppressWarnings("unused")
	private void setI(int i)
	{
		this.i = i;
	}

	void test1(String str, int b)
	{
		this.i = b;
	}

	void test1(String str) {}
	void test1(Object obj) {}

	void test1(byte a, int b) {}

}

class C extends ArrayList<Object>
		implements List<Object>
{
	private static final long serialVersionUID = 1L;

}
