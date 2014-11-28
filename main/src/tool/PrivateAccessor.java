
package tool;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * 类的私有方法及私有属性的访问工具. <p>
 * 当然用这个工具来访问那些非私有的成员也是可以的.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class PrivateAccessor
{
	/**
	 * 构造一个指定类型的null值. <p>
	 * 如下面两个方法: <p>
	 * void test(String str)<p>
	 * void test(Object obj)<p>
	 * 如果你直接使用test(null)来调用编译时就会出错, 这时候需要通过强制
	 * 类型转换指定一个类型, 如: test((String) null).
	 * 这个方法就相当于对一个null值进行强制类型转换.
	 *
	 * @param type  null值的类型
	 */
	public static Object nullValue(Class type)
	{
		return new NullValue(type);
	}

	/**
	 * 调用一个类的构造函数来创建对象.
	 *
	 * @param c       需要构造的类
	 * @param params  构造的参数
	 */
	public static Object create(Class c, Object... params)
			throws Exception
	{
		Constructor[] cArr = c.getDeclaredConstructors();
		int paramCount = params == null ? 0 : params.length;
		Constructor constructor = null;
		int match = Integer.MAX_VALUE;
		for (int i = 0; i < cArr.length; i++)
		{
			Class[] types = cArr[i].getParameterTypes();
			if (types.length == paramCount)
			{
				int tmp = checkTypes(types, params);
				if (tmp != -1 && tmp <= match)
				{
					constructor = cArr[i];
					match = tmp;
				}
			}
		}
		if (constructor == null)
		{
			throw new NoSuchMethodException("Constructor:" + c.getName());
		}
		if (!constructor.isAccessible())
		{
			constructor.setAccessible(true);
			Object obj = constructor.newInstance(params);
			constructor.setAccessible(false);
			return obj;
		}
		return constructor.newInstance(params);
	}

	/**
	 * 调用一个对象或类的(静态)方法.
	 *
	 * @param obj     方法所在的对象, 如果是静态方法则可直接给出其class
	 * @param method  需要调用的方法名称
	 * @param params  方法的参数
	 */
	public static Object invoke(Object obj, String method,
			Object... params)
			throws Exception
	{
		return invoke(obj, null, method, params);
	}

	/**
	 * 调用一个对象或类的(静态)方法. <p>
	 * 增加了一个class类型的参数用于指定方法在哪个类中, 如在A中有个私有
	 * 方法set, B继承了A也有个私有方法set, 那如果直接给出B的实例那只能
	 * 调用到B中的方法set, 如果需要调用A中的方法set, 那就需要在这里type
	 * 参数中将class指定为A.
	 *
	 * @param obj     方法所在的对象, 如果是静态方法则可直接给出其class
	 * @param type    方法所在的class
	 * @param method  需要调用的方法名称
	 * @param params  方法的参数
	 */
	public static Object invoke(Object obj, Class type,
			String method, Object... params)
			throws Exception
	{
		Class c;
		if ((c = type) == null)
		{
			if (obj instanceof Class)
			{
				c = (Class) obj;
			}
			else
			{
				c = obj.getClass();
			}
		}
		MethodContainer mc = getMethod(c, method, obj instanceof Class, params);
		if (mc == null)
		{
			throw new NoSuchMethodException(method);
		}
		Method m = mc.method;
		if (!m.isAccessible())
		{
			m.setAccessible(true);
			Object r = null;
			try
			{
				r = m.invoke(obj, params);
			}
			catch (InvocationTargetException ex)
			{
				Throwable cause = ex.getCause();
				throw cause instanceof Exception ? (Exception) cause : ex;
			}
			m.setAccessible(false);
			return r;
		}
		return m.invoke(obj, params);
	}

	/**
	 * 在给出了指定类的情况下, 获取需要的方法.
	 */
	private static MethodContainer getMethod(Class c, String method, boolean needStatic,
			Object[] params)
			throws Exception
	{
		Method[] mArr = c.getDeclaredMethods();
		int paramCount = params == null ? 0 : params.length;
		Method m = null;
		int match = Integer.MAX_VALUE;
		for (int i = 0; i < mArr.length; i++)
		{
			Class[] types = mArr[i].getParameterTypes();
			if (method.equals(mArr[i].getName()) && types.length == paramCount)
			{
				if (needStatic && !Modifier.isStatic(mArr[i].getModifiers()))
				{
					continue;
				}
				int tmp = checkTypes(types, params);
				if (tmp != -1 && tmp <= match)
				{
					m = mArr[i];
					match = tmp;
				}
			}
		}
		if (m != null && match == 0)
		{
			return new MethodContainer(m, match);
		}
		Class superClass = c.getSuperclass();
		if (superClass != null && superClass != Object.class)
		{
			MethodContainer mc = getMethod(superClass, method, needStatic, params);
			if (mc != null && mc.match < match)
			{
				return mc;
			}
		}
		return m != null ? new MethodContainer(m, match) : null;
	}

	/**
	 * 检查参数列表与给出的参数类型是否一致.
	 *
	 * @return  -1 表示不一致
	 *           0 表示完全一致
	 *          >1 表示匹配度, 数字越大匹配度越低
	 */
	private static int checkTypes(Class[] types, Object[] objs)
	{
		if (types.length == 0)
		{
			return 0;
		}
		int match = 0;
		nextType:
		for (int i = 0; i < types.length; i++)
		{
			if (objs[i] != null)
			{
				Class t = objs[i].getClass() ;
				if (t == NullValue.class)
				{
					t = ((NullValue) objs[i]).type;
				}
				if (t != types[i])
				{
					if (types[i].isAssignableFrom(t))
					{
						// 匹配的是父类, 需要降低匹配度
						match += getInheritLevel(t, types[i], 1);
						continue nextType;
					}
					if (types[i].isPrimitive())
					{
						Class[] tArr = (Class[]) wrapperIndex.get(t);
						for (int j = 0; j < tArr.length; j++)
						{
							if (tArr[j] == types[i])
							{
								match += j;
								continue nextType;
							}
						}
						// 此类型不匹配
						return -1;
					}
					else
					{
						// 如果不是基本类型, 则不匹配
						return -1;
					}
				}
			}
			else if (types[i].isPrimitive())
			{
				// 对于基本类型, 给出的参数不能为null
				return -1;
			}
		}
		return match;
	}

	/**
	 * 获取继承关系的层级.
	 */
	private static int getInheritLevel(Class c, Class type, int nowLevel)
	{
		Class p = c.getSuperclass();
		int minLevel;
		if (p == type)
		{
			// 如果父类是Object匹配度将5级
			minLevel = type == Object.class ? nowLevel + 5 : nowLevel;
		}
		else
		{
			minLevel = Integer.MAX_VALUE >> 1;
			if (p != null)
			{
				minLevel = getInheritLevel(p, type, nowLevel + 1);
			}
		}
		Class[] iArr = c.getInterfaces();
		for (int i = 0; i < iArr.length; i++)
		{
			int tmp;
			if (iArr[i] == type)
			{
				// 接口的匹配度需要降一级
				tmp = nowLevel + 1;
			}
			else
			{
				tmp = getInheritLevel(iArr[i], type, nowLevel + 1);
			}
			if (tmp < minLevel)
			{
				minLevel = tmp;
			}
		}
		return minLevel;
	}

	/**
	 * 获取一个对象或类的(静态)属性的值.
	 *
	 * @param obj    属性值所在的对象, 如果是静态属性则可直接给出其class
	 * @param field  需要获取的属性名称
	 */
	public static Object get(Object obj, String field)
			throws Exception
	{
		return get(obj, null, field);
	}

	/**
	 * 获取一个对象或类的(静态)属性的值. <p>
	 * 增加了一个class类型的参数用于指定属性在哪个类中, 如在A中有个私有属性i,
	 * B继承了A也有个私有属性i, 那如果直接给出B的实例那只能获取到B中的属性i,
	 * 如果需要获取A中的属性i, 那就需要在这里type参数中将class指定为A.
	 *
	 * @param obj    属性值所在的对象, 如果是静态属性则可直接给出其class
	 * @param type   属性所在的class
	 * @param field  需要获取的属性名称
	 */
	public static Object get(Object obj, Class type, String field)
			throws Exception
	{
		Class c;
		if ((c = type) == null)
		{
			if (obj instanceof Class)
			{
				c = (Class) obj;
			}
			else
			{
				c = obj.getClass();
			}
		}
		Field f = getField(c, field, obj instanceof Class);
		if (!f.isAccessible())
		{
			f.setAccessible(true);
			Object r = f.get(obj);
			f.setAccessible(false);
			return r;
		}
		return f.get(obj);
	}

	/**
	 * 设置一个对象或类的(静态)属性的值.
	 *
	 * @param obj    属性值所在的对象, 如果是静态属性则可直接给出其class
	 * @param field  需要设置的属性名称
	 * @param value  需要设置的值
	 */
	public static void set(Object obj, String field, Object value)
			throws Exception
	{
		set(obj, null, field, value);
	}

	/**
	 * 设置一个对象或类的(静态)属性的值. <p>
	 * 增加了一个class类型的参数用于指定属性在哪个类中, 如在A中有个私有属性i,
	 * B继承了A也有个私有属性i, 那如果直接给出B的实例那只能设置B中的属性i, 如果
	 * 需要设置A中的属性i, 那就需要在这里type参数中将class指定为A.
	 *
	 * @param obj    属性值所在的对象, 如果是静态属性则可直接给出其class
	 * @param type   属性所在的class
	 * @param field  需要设置的属性名称
	 * @param value  需要设置的值
	 */
	public static void set(Object obj, Class type, String field,
			Object value)
			throws Exception
	{
		Class c;
		if ((c = type) == null)
		{
			if (obj instanceof Class)
			{
				c = (Class) obj;
			}
			else
			{
				c = obj.getClass();
			}
		}
		Field f = getField(c, field, obj instanceof Class);
		if (!f.isAccessible())
		{
			f.setAccessible(true);
			f.set(obj, value);
			f.setAccessible(false);
		}
		else
		{
			f.set(obj, value);
		}
	}

	/**
	 * 在给出了指定类的情况下, 获取需要的属性.
	 */
	private static Field getField(Class c, String field, boolean needStatic)
			throws Exception
	{
		try
		{
			Field f = c.getDeclaredField(field);
			if (needStatic && !Modifier.isStatic(f.getModifiers()))
			{
				throw new NoSuchFieldException("Field " + f.getName() 
						+ " is't static.");
			}
			return f;
		}
		catch (Exception ex)
		{
			Class superClass = c.getSuperclass();
			if (superClass != null && superClass != Object.class)
			{
				return getField(superClass, field, needStatic);
			}
			throw ex;
		}
	}

	// 外覆类的索引表
	private static Map wrapperIndex = new HashMap();
	static
	{
		wrapperIndex.put(Boolean.class, new Class[]{boolean.class});
		wrapperIndex.put(Character.class, new Class[]{char.class, int.class, long.class});
		wrapperIndex.put(Byte.class, new Class[]{byte.class, short.class, int.class, long.class});
		wrapperIndex.put(Short.class, new Class[]{short.class, int.class, long.class});
		wrapperIndex.put(Integer.class, new Class[]{int.class, long.class});
		wrapperIndex.put(Float.class, new Class[]{float.class, double.class});
		wrapperIndex.put(Double.class, new Class[]{double.class});
	}

	static final class NullValue
	{
		private Class type;
		public NullValue(Class type)
		{
			if (type == null)
			{
				throw new NullPointerException("type is null.");
			}
			this.type = type;
		}

	}

	static class MethodContainer
	{
		Method method;
		int match;
		public MethodContainer(Method method, int match)
		{
			this.match = match;
			this.method = method;
		}

	}

}