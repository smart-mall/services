/**
 * Copyright (c) 2016-2019 人人开源 All rights reserved.
 *
 * https://www.renren.io
 *
 * 版权所有，侵权必究！
 */

package common.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring 容器工具：持有容器引用，供非托管对象静态取 Bean。
 *
 * <p>容器启动时由 Spring 回调 {@link #setApplicationContext(ApplicationContext)} 写入引用；
 * 本类没被扫描到或容器未启动时该引用为 {@code null}，此时调用取 Bean 的方法会抛 NPE。
 */
@Component
public class SpringContextUtils implements ApplicationContextAware {
	/** 容器引用，由 Spring 在启动阶段写入；未注入时为 null */
	public static ApplicationContext applicationContext; 

	/**
	 * {@inheritDoc}
	 *
	 * <p>把容器引用写入静态字段，使本类可以在任意位置静态取 Bean。
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		SpringContextUtils.applicationContext = applicationContext;
	}

	/**
	 * 按名字取 Bean。
	 *
	 * @param name Bean 名
	 * @return Bean 实例
	 * @throws BeansException 容器里没有该名字的 Bean 时抛出
	 */
	public static Object getBean(String name) {
		return applicationContext.getBean(name);
	}

	/**
	 * 按名字与类型取 Bean。
	 *
	 * @param name Bean 名
	 * @param requiredType 期望的类型
	 * @param <T> Bean 类型
	 * @return Bean 实例
	 * @throws BeansException 容器里没有该 Bean，或类型不匹配时抛出
	 */
	public static <T> T getBean(String name, Class<T> requiredType) {
		return applicationContext.getBean(name, requiredType);
	}

	/**
	 * 判断容器里是否存在指定名字的 Bean。
	 *
	 * @param name Bean 名
	 * @return 存在返回 {@code true}
	 */
	public static boolean containsBean(String name) {
		return applicationContext.containsBean(name);
	}

	/**
	 * 判断指定名字的 Bean 是否为单例。
	 *
	 * @param name Bean 名
	 * @return 是单例返回 {@code true}
	 * @throws BeansException 容器里没有该名字的 Bean 时抛出
	 */
	public static boolean isSingleton(String name) {
		return applicationContext.isSingleton(name);
	}

	/**
	 * 取指定名字的 Bean 的类型。
	 *
	 * @param name Bean 名
	 * @return Bean 的类型；名字不存在时返回 {@code null}
	 */
	public static Class<? extends Object> getType(String name) {
		return applicationContext.getType(name);
	}

}