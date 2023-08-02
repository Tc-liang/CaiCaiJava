package A_内存管理篇.A1_HotSpot对象探秘;

/**
 * @Author: Caicai
 * @Date: 2023-08-01 11:56
 * @Description: 轻量级锁膨胀
 * 加锁
 * 1.如果是无锁状态才复制mark word 并CAS 替换为线程地址 成功则获取锁
 * 2.如果获取了锁查看当前线程是否持有锁（可重入）
 * 3.其他情况说明有竞争进行膨胀，将mark word中改为指向重量级锁
 * <p>
 * 解锁
 * 1.查看复制的头信息为空说明可重入
 * 2.复制的头信息 与 对象mark word相等，说明没有竞争 尝试CAS将mark word替换回去 成功释放，失败则是有竞争（其他线程膨胀了）
 * 3.有竞争情况 使用 重量级锁释放流程
 * <p>
 * 膨胀
 * 1、已膨胀 （直接退出）
 * 2、膨胀中  （循环等待）
 * 3、轻量级锁膨胀 （创建monitor对象，注入属性，CAS修改mark word）
 * 4、无锁膨胀 （创建monitor对象，CAS修改mark word）
 * <p>
 * 源码位置 open jdk 8 (hotspot -> src -> share -> vm -> runtime -> synchronizer.cpp)
 * <p>
 * <p>
 * 在真正阻塞前会尝试自旋获取锁，避免阻塞开销
 * 固定+动态自旋  成功获取锁会增大自旋次数，失败则减少 （评估系统竞争，竞争大就减少自旋次数）
 * 源码位置 open jdk 8 (hotspot -> src -> share -> vm -> runtime -> objectMonitor.cpp) TrySpin_VaryDuration 方法
 */
public class F_LightLockToHeavyLock {
}
