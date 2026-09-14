package com.sky.context;

public class BaseContext {
    // 线程局部变量，用于存储当前线程的用户ID
    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {threadLocal.set(id);}

    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
    }

}
