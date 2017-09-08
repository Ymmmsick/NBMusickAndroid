package com.msr.nbmusic.app;

import android.app.Activity;

import com.orhanobut.logger.Logger;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Stack;

/**
 * ProjectName：AndroidUtil
 * PackageName: com.standard.util.helper
 * Description：自定义activity的栈管理(栈结构扩展Vector，元素不要求唯一性，可存在多个相同实例)
 * <p>
 * CreateTime: 6/24/2016 12:36
 * Modifier：liminghuang
 * ModifyTime：6/24/2016 12:36
 * Comment：
 *
 * @author liminghuang
 */
public final class BAAppManager {

    // 使用弱引用是因为存在未使用AppManager的finish方法来释放的activity，但mActivityStack并未断开对其应用导致内存泄露的问题
    private Stack<WeakReference<Activity>> mActivityStack;
    private static volatile BAAppManager sInstance;

    private BAAppManager() {
    }

    /**
     * 单例
     *
     * @return 返回AppManager的单例
     */
    public static BAAppManager getInstance() {
        if (sInstance == null) {
            synchronized (BAAppManager.class) {
                if (sInstance == null) {
                    sInstance = new BAAppManager();
                }
            }
        }
        return sInstance;
    }

    /**
     * 添加Activity到堆栈
     *
     * @param activity activity实例
     */
    public void addActivity(Activity activity) {
        if (mActivityStack == null) {
            mActivityStack = new Stack<>();
        }
        mActivityStack.add(new WeakReference<>(activity));
    }

    /**
     * 检查弱引用是否释放，若释放，则从栈中清理掉该元素
     */
    public void checkWeakReference() {
        if (mActivityStack != null) {
            for (Iterator<WeakReference<Activity>> it = mActivityStack.iterator(); it.hasNext(); ) {
                WeakReference<Activity> activityReference = it.next();
                Activity temp = activityReference.get();
                if (temp == null) {
                    it.remove();// 使用迭代器来进行安全的加锁操作
                }
            }
        }
    }

    /**
     * 获取当前Activity（栈中最后一个压入的）
     *
     * @return 当前（栈顶）activity
     */
    public Activity currentActivity() {
        checkWeakReference();
        if (mActivityStack != null && !mActivityStack.isEmpty()) {
            return mActivityStack.lastElement().get();
        }
        return null;
    }

    /**
     * 结束除当前activtiy以外的所有activity
     * 注意：不能使用foreach遍历并发删除，会抛出java.util.ConcurrentModificationException的异常
     *
     * @param activtiy 不需要结束的activity
     */
    public void finishOtherActivity(Activity activtiy) {
        if (mActivityStack != null && activtiy != null) {
            for (Iterator<WeakReference<Activity>> it = mActivityStack.iterator(); it.hasNext(); ) {
                WeakReference<Activity> activityReference = it.next();
                Activity temp = activityReference.get();
                if (temp == null) {// 清理掉已经释放的activity
                    it.remove();
                    continue;
                }
                if (temp != activtiy) {
                    it.remove();// 使用迭代器来进行安全的加锁操作
                    temp.finish();
                }
            }
        }
    }

    /**
     * 结束除这一类activtiy以外的所有activity
     *
     * @param cls 指定的某类activity
     */
    public void finishOtherActivity(Class<?> cls) {
        if (mActivityStack != null) {
            for (Iterator<WeakReference<Activity>> it = mActivityStack.iterator(); it.hasNext(); ) {
                WeakReference<Activity> activityReference = it.next();
                Activity activity = activityReference.get();
                if (activity == null) {// 清理掉已经释放的activity
                    it.remove();
                    continue;
                }
                if (!activity.getClass().equals(cls)) {
                    it.remove();// 使用迭代器来进行安全的加锁操作
                    activity.finish();
                }
            }
        }
    }

    /**
     * 结束当前Activity（堆栈中最后一个压入的）
     */
    public void finishActivity() {
        Activity activity = currentActivity();
        if (activity != null) {
            finishActivity(activity);
        }
    }

    /**
     * 结束指定的Activity
     *
     * @param activity 指定的activity实例
     */
    public void finishActivity(Activity activity) {
        if (activity != null) {
            for (Iterator<WeakReference<Activity>> it = mActivityStack.iterator(); it.hasNext(); ) {
                WeakReference<Activity> activityReference = it.next();
                Activity temp = activityReference.get();
                if (temp == null) {// 清理掉已经释放的activity
                    it.remove();
                    continue;
                }
                if (temp == activity) {
                    it.remove();
                }
            }
            activity.finish();
        }
    }

    /**
     * 移除指定的Activity
     *
     * @param activity 指定的activity实例
     */
    public void removeActivity(Activity activity) {
        if (activity != null) {
            for (Iterator<WeakReference<Activity>> it = mActivityStack.iterator(); it.hasNext(); ) {
                WeakReference<Activity> activityReference = it.next();
                Activity temp = activityReference.get();
                if (temp == null) {// 清理掉已经释放的activity
                    it.remove();
                    continue;
                }
                if (temp == activity) {
                    it.remove();
                }
            }
        }
    }

    /**
     * 结束指定类名的所有Activity
     *
     * @param cls 指定的类的class
     */
    public void finishActivity(Class<?> cls) {
        if (mActivityStack != null) {
            for (Iterator<WeakReference<Activity>> it = mActivityStack.iterator(); it.hasNext(); ) {
                WeakReference<Activity> activityReference = it.next();
                Activity activity = activityReference.get();
                if (activity == null) {// 清理掉已经释放的activity
                    it.remove();
                    continue;
                }
                if (activity.getClass().equals(cls)) {
                    it.remove();
                    activity.finish();
                }
            }
        }
    }

    /**
     * 结束所有Activity
     */
    public void finishAllActivity() {
        if (mActivityStack != null) {
            for (Iterator<WeakReference<Activity>> it = mActivityStack.iterator(); it.hasNext(); ) {
                WeakReference<Activity> activityReference = it.next();
                Activity activity = activityReference.get();
                if (activity != null) {
                    activity.finish();
                }
            }
            mActivityStack.clear();
        }
    }

    /**
     * 退出应用程序
     */
    public void exitApp() {
        try {
            finishAllActivity();
            // 退出JVM(java虚拟机),释放所占内存资源,0表示正常退出(非0的都为异常退出)
            System.exit(0);
            // 从操作系统中结束掉当前程序的进程
            android.os.Process.killProcess(android.os.Process.myPid());
        } catch (Exception e) {
            Logger.e("Exit exception", e);
        }
    }
}
