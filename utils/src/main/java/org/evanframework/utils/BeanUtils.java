package org.evanframework.utils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.dozer.DozerBeanMapper;
import org.springframework.util.Assert;

import net.sf.cglib.beans.BeanCopier;

/**
 * BeanUtils;
 * 
 * @author shen.wei
 * @version Date: 2010-10-16 上午11:25:04
 */
public class BeanUtils {
	private final static String DEFAULT_CHARSET = "UTF-8";

	// private static final Logger logger =
	// LoggerFactory.getLogger(BeanUtils.class);

	/**
	 * 持有Dozer单例, 避免重复创建DozerMapper消耗资源.
	 */
	private static DozerBeanMapper dozer = new DozerBeanMapper();

	private final static Map<Class<?>, Map<Class<?>, BeanCopier>> beanCopiers = new ConcurrentHashMap<Class<?>, Map<Class<?>, BeanCopier>>(
			128);

	public static void eachProperties(Object bean, EachPropertiesHandler eachPropertiesHandler) {
		eachPropertiesInner(bean.getClass(), bean, eachPropertiesHandler);
	}

	/**
	 * 将bean转换层map
	 * 
	 * @param bean
	 *  Map<String, Object>
	 */
	public static Map<String, Object> beanToMap(Object bean) {
		Map<String, Object> map = new HashMap<String, Object>();
		beanToMapInner(bean.getClass(), bean, map);
		return map;
	}

	/**
	 * 将bean转换成queryString<br>
	 * <br>
	 * 返回格式：
	 * 
	 * <pre>
	 * property1=1&property2=2
	 * </pre>
	 * <p>
	 * shen.wei create at 2013-11-6
	 * 上午11:00:56
	 * </p>
	 * 
	 * @param bean
	 * @param dateFormat
	 *  String
	 */
	public static String beanToQueryString(Object bean, String dateFormat, String charset) {
		StringBuffer string = new StringBuffer();
		beanToQueryStringInner(bean.getClass(), string, bean, dateFormat, charset);
		if (string.length() > 0) {
			string.delete(0, 1);
		}
		return string.toString();
	}

	/**
	 * 将bean转换成queryString<br>
	 * 编码：utf-8
	 * 
	 * @param bean
	 * @param dateFormat
	 *  <p>
	 *         author: ShenWei<br>
	 *         create at 2015年5月20日 上午11:18:05
	 */
	public static String beanToQueryString(Object bean, String dateFormat) {
		return beanToQueryString(bean, dateFormat, DEFAULT_CHARSET);
	}

	/**
	 * 将bean转换成queryString<br>
	 * 编码：utf-8<br>
	 * 日期格式：yyyy-MM-dd
	 * 
	 * @param bean
	 *  <p>
	 *         author: ShenWei<br>
	 *         create at 2015年5月20日 上午11:18:13
	 */
	public static String beanToQueryString(Object bean) {
		return beanToQueryString(bean, "yyyy-MM-dd", DEFAULT_CHARSET);
	}

	/**
	 * 对象快速复制
	 * <pre>
	 *Demo demo = new Demo();
	 *demo.setXX();
	 *...
	 *DemoQuery query = BeanUtils.quickMap(demo, DemoQuery.class);
	 * </pre>
	 * 
	 * @param source
	 * @param targetClass
	 *  <p>
	 *         author: ShenWei<br>
	 *         create at 2015年5月20日 上午11:20:33
	 */
	public static <T> T quickMap(Object source, Class<T> targetClass) {
		if (source == null) {
			return null;
		}
		BeanCopier beanCopier = getBeanCopier(source.getClass(), targetClass);

		T to = null;

		try {
			to = targetClass.newInstance();
		} catch (InstantiationException e) {
			throw new UnsupportedOperationException("Class " + targetClass + "not hava constructor is no params", e);
		} catch (IllegalAccessException e) {
			throw new UnsupportedOperationException("Class " + targetClass + "not hava constructor is no params", e);
		}

		beanCopier.copy(source, to, null);
		return to;
	}

	/**
	 * 对象快速复制
	 */
	public static <T> List<T> quickMapList(Collection<?> sourceList, Class<T> targetClass) {
		List<T> targetList = new ArrayList<T>();
		if (sourceList.isEmpty()) {
			return targetList;
		}

		Iterator<?> it = sourceList.iterator();
		BeanCopier beanCopier = getBeanCopier(it.next().getClass(), targetClass);
		try {
			for (Object source : sourceList) {
				T target;
				target = targetClass.newInstance();
				beanCopier.copy(source, target, null);
				targetList.add(target);
			}
		} catch (Exception e) {
			throw new UnsupportedOperationException("Class " + targetClass + "not hava constructor is no params", e);
		}
		return targetList;
	}

	/**
	 * 对象快速拷贝
	 */
	public static void quickCopy(Object source, Object target) {
		Assert.notNull(source,"source must not null");
		Assert.notNull(target,"target must not null");
		BeanCopier beanCopier = getBeanCopier(source.getClass(), target.getClass());
		beanCopier.copy(source, target, null);
	}

	/**
	 * 对象深拷贝
	 */
	public static <T> T adviceMap(Object source, Class<T> destinationClass) {
		if (source == null) {
			return null;
		}
		return dozer.map(source, destinationClass);
	}

	/**
	 * 对象深拷贝
	 */
	public static <T> List<T> adviceMapList(Collection<?> sourceList, Class<T> destinationClass) {
		List<T> destinationList = new ArrayList<T>();
		for (Object sourceObject : sourceList) {
			T destinationObject = dozer.map(sourceObject, destinationClass);
			destinationList.add(destinationObject);
		}
		return destinationList;
	}

	/**
	 * 对象深拷贝
	 */
	public static void adviceCopy(Object source, Object destinationObject) {
		Assert.notNull(source,"source must not null");
		Assert.notNull(destinationObject,"destinationObject must not null");
		dozer.map(source, destinationObject);
	}

	/**
	 * 获取一个对象的对象名
	 * 
	 * @param po
	 *  objectName
	 */
	public static String getClassName(Object po) {
		String returnString = StringUtils.capitalize(po.getClass().getSimpleName());
		return returnString;
	}

	/**
	 * 获取一个类的对象名
	 * 
	 * @param c
	 *  objectName
	 */
	public static String getObjectName(Class<?> c) {
		String returnString = StringUtils.uncapitalize(c.getSimpleName());
		return returnString;
	}

	private static void beanToMapInner(Class<?> thisClass, Object bean, Map<String, Object> map) {
		Method[] methods = thisClass.getDeclaredMethods();
		for (Method method : methods) {
			String methodName = method.getName();
			if (methodName.length() > 3 && methodName.startsWith("get")) {
				pubValueToMap(bean, map, method, 3);
			} else if (methodName.length() > 2 && methodName.startsWith("is")) {
				pubValueToMap(bean, map, method, 2);
			}
		}
		Class<?> superClass = thisClass.getSuperclass();
		if (superClass != null && !Object.class.equals(superClass)) {
			beanToMapInner(superClass, bean, map);
		}
	}

	private static void pubValueToMap(Object bean, Map<String, Object> map, Method method, int methedNamePrefixLength) {
		Object value = invoke(bean, method);
		if (value != null) {
			String methodName = method.getName();
			String fieldName = StringUtils.uncapitalize(StringUtils.substring(methodName, methedNamePrefixLength));
			map.put(fieldName, value);
		}
	}

	private static void pubValueToString(Object bean, StringBuffer string, Method method, int methedNamePrefixLength,
			String dateFormat, String charset) {
		Object value = invoke(bean, method);
		if (value != null) {
			boolean tag = true;
			if (Boolean.class.isInstance(value)) {
				tag = Boolean.TRUE.equals(value);
			}
			if (tag) {
				String methodName = method.getName();
				String fieldName = StringUtils.uncapitalize(StringUtils.substring(methodName, methedNamePrefixLength));

				if (Date.class.isInstance(value)) {
					value = DateUtils.format((Date) value, dateFormat);
				}
				try {
					value = URLEncoder.encode(value + "", charset);
				} catch (UnsupportedEncodingException e) {
					throw new UnsupportedOperationException("unsupported encoding exception", e);
				}

				string.append("&" + fieldName + "=" + value);
			}
		}
	}

	private static void beanToQueryStringInner(Class<?> thisClass, StringBuffer string, Object bean, String dateFormat,
			String charset) {
		Method[] methods = thisClass.getDeclaredMethods();
		for (Method method : methods) {
			String methodName = method.getName();
			if (methodName.length() > 3 && methodName.startsWith("get")) {
				pubValueToString(bean, string, method, 3, dateFormat, charset);
			} else if (methodName.length() > 2 && methodName.startsWith("is")) {
				pubValueToString(bean, string, method, 2, dateFormat, charset);
			}
		}

		Class<?> superClass = thisClass.getSuperclass();
		if (superClass != null && !Object.class.equals(superClass)) {
			beanToQueryStringInner(superClass, string, bean, dateFormat, charset);
		}
	}

	private static BeanCopier getBeanCopier(Class<?> sourceClass, Class<?> targetClass) {
		Map<Class<?>, BeanCopier> mapInner = beanCopiers.get(sourceClass);
		BeanCopier beanCopier = null;
		if (mapInner == null) {
			mapInner = new ConcurrentHashMap<Class<?>, BeanCopier>(128);
			beanCopier = BeanCopier.create(sourceClass, targetClass, false);
			mapInner.put(targetClass, beanCopier);
			beanCopiers.put(sourceClass, mapInner);
		} else {
			beanCopier = mapInner.get(targetClass);
			if (beanCopier == null) {
				beanCopier = BeanCopier.create(sourceClass, targetClass, false);
				mapInner.put(targetClass, beanCopier);
			}
		}
		return beanCopier;
	}

	private static void eachPropertiesInner(Class<?> thisClass, Object bean, EachPropertiesHandler eachPropertiesHandler) {
		Method[] methods = thisClass.getDeclaredMethods();
		for (Method method : methods) {
			String methodName = method.getName();
			if (methodName.length() > 3 && methodName.startsWith("get")) {
				pubValueToHandler(bean, method, 3, eachPropertiesHandler);
			} else if (methodName.length() > 2 && methodName.startsWith("is")) {
				pubValueToHandler(bean, method, 2, eachPropertiesHandler);
			}
		}
		Class<?> superClass = thisClass.getSuperclass();
		if (superClass != null && !Object.class.equals(superClass)) {
			eachPropertiesInner(superClass, bean, eachPropertiesHandler);
		}
	}

	private static void pubValueToHandler(Object bean, Method method, int methedNamePrefixLength,
			EachPropertiesHandler eachPropertiesHandler) {
		Object value = invoke(bean, method);

		if (value != null) {
			String methodName = method.getName();
			String fieldName = StringUtils.uncapitalize(StringUtils.substring(methodName, methedNamePrefixLength));
			eachPropertiesHandler.handler(fieldName, value);
		}
	}

	private static Object invoke(Object bean, Method method) {
		Object value = null;
		try {
			value = method.invoke(bean);
		} catch (IllegalAccessException e) {
			throw new UnsupportedOperationException(e);
		} catch (InvocationTargetException e) {
			throw new UnsupportedOperationException(e.getTargetException().getMessage(), e);
		}
		return value;
	}

	public interface EachPropertiesHandler {
		void handler(String property, Object value);
	}

	/**
	 * 利用反射通过get方法获取bean中字段fieldName的值
	 * @param bean
	 * @param fieldName
	 *
	 * @throws Exception
	 */
	private static Object getFieldValue(Object bean, String fieldName)
			throws Exception {
		StringBuffer result = new StringBuffer();
		String methodName = result.append("get")
				.append(fieldName.substring(0, 1).toUpperCase())
				.append(fieldName.substring(1)).toString();
		Method method = bean.getClass().getMethod(methodName, new Class[] {});
		method.setAccessible(true);
		Object rObject = method.invoke(bean, new Object[0]);
		return rObject;
	}

	/**
	 * 利用发射调用bean.set方法将value设置到字段
	 * @param bean
	 * @param fieldName
	 * @param value
	 * @throws Exception
	 */
	private static void setFieldValue(Object bean, String fieldName, Object value)
			throws Exception {
		StringBuffer result = new StringBuffer();
		String methodName = result.append("set")
				.append(fieldName.substring(0, 1).toUpperCase())
				.append(fieldName.substring(1)).toString();

		/**
		 * 利用发射调用bean.set方法将value设置到字段
		 */
		Method method=bean.getClass().getMethod(methodName,String.class);
		method.setAccessible(true);
		method.invoke(bean,value);
	}

	/**
	 * 将一个 Map 对象转化为一个 JavaBean
	 *
	 * @param type
	 *            要转化的类型
	 * @param map
	 *            包含属性值的 map
	 *  转化出来的 JavaBean 对象
	 * @throws IntrospectionException
	 *             如果分析类属性失败
	 * @throws IllegalAccessException
	 *             如果实例化 JavaBean 失败
	 * @throws InstantiationException
	 *             如果实例化 JavaBean 失败
	 * @throws InvocationTargetException
	 *             如果调用属性的 setter 方法失败
	 */
	public static Object convertBean(Class type, Map map) throws IntrospectionException, IllegalAccessException,
			InstantiationException, InvocationTargetException {
		BeanInfo beanInfo = Introspector.getBeanInfo(type); // 获取类属性
		Object obj = type.newInstance(); // 创建 JavaBean 对象

		// 给 JavaBean 对象的属性赋值
		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
		for (int i = 0; i < propertyDescriptors.length; i++) {
			PropertyDescriptor descriptor = propertyDescriptors[i];
			String propertyName = descriptor.getName();

			if (map.containsKey(propertyName)) {
				// 下面一句可以 try 起来，这样当一个属性赋值失败的时候就不会影响其他属性赋值。
				Object value = map.get(propertyName);

				Object[] args = new Object[1];
				args[0] = value;

				descriptor.getWriteMethod().invoke(obj, args);
			}
		}
		return obj;
	}

	/**
	 * 将一个 JavaBean 对象转化为一个 Map
	 *
	 * @param bean
	 *            要转化的JavaBean 对象
	 *  转化出来的 Map 对象
	 * @throws IntrospectionException
	 *             如果分析类属性失败
	 * @throws IllegalAccessException
	 *             如果实例化 JavaBean 失败
	 * @throws InvocationTargetException
	 *             如果调用属性的 setter 方法失败
	 */
	public static Map convertMap(Object bean) throws IntrospectionException, IllegalAccessException,
			InvocationTargetException {
		Class type = bean.getClass();
		Map returnMap = new HashMap();
		BeanInfo beanInfo = Introspector.getBeanInfo(type);

		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
		for (int i = 0; i < propertyDescriptors.length; i++) {
			PropertyDescriptor descriptor = propertyDescriptors[i];
			String propertyName = descriptor.getName();
			if (!propertyName.equals("class")) {
				Method readMethod = descriptor.getReadMethod();
				Object result = readMethod.invoke(bean, new Object[0]);
				if (result != null) {
					returnMap.put(propertyName, result);
				} else {
					returnMap.put(propertyName, "");
				}
			}
		}
		return returnMap;
	}

	/**
	 * 去掉bean中所有属性为字符串的前后空格
	 * @param bean
	 * @throws Exception
	 */
	public static void beanAttributeValueTrim(Object bean) throws Exception {
		if(bean!=null){
			//获取所有的字段包括public,private,protected,private
			Field[] fields = bean.getClass().getDeclaredFields();
			for (int i = 0; i < fields.length; i++) {
				Field f = fields[i];
				if (f.getType().getName().equals("java.lang.String")) {
					String key = f.getName();//获取字段名
					Object value = getFieldValue(bean, key);
					if (null==value){
						continue;
					}
					setFieldValue(bean, key, value.toString().trim());
				}
			}
		}
	}
}
