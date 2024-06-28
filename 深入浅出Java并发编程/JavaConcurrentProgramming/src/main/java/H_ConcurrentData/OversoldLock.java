package H_ConcurrentData;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/6/26 9:04
 * @description: 加锁防止超卖
 */
public class OversoldLock {

    private int selectStockCountByDB(int id) {
        //根据ID查询商品库存数量
        String sql = "select stock_count from goods where id = ?";
        return id;
    }

    private int selectStockCountByDBForUpdate(int id) {
        String sql = "select stock_count from goods where id = ? for update";
        return 0;
    }

    private void cutStock(int id, int count) {
        //根据id 减库存
        String sql = "update goods set stock_count = stock_count - ? where id = ?";
    }

    private int cutStockCompareVersion(int id, int count, int version) {
        //根据id 减库存
        String sql = "update goods set stock_count = stock_count - ? where id = ? and stock_count = ?";
        return 1;
    }

    /**
     * 存在并发问题的购买
     *
     * @param id    商品ID
     * @param count 数量
     * @return 是否购买成功
     */
    public boolean buy(int id, int count) {
        boolean res = false;
        //根据ID查询商品库存数量  select stock_count from goods where id = ?
        int stockCount = selectStockCountByDB(id);
        //由于读操作和写操作不是原子性，在此期间可能并发查到 stockCount > 0 导致超卖
        if (stockCount > 0) {
            //减库存 update goods set stock_count = stock_count - ? where id = ?
            cutStock(id, count);
            res = true;
        }
        return res;
    }

    private static final Map<Integer, Object> synchronizedMap = new ConcurrentHashMap<>();

    public boolean synchronizedBuy(int id, int count) {
        boolean res = false;
        //所有商品都用同一把锁粒度太粗，每个商品ID对应一把锁
        Object lockObj = synchronizedMap.computeIfAbsent(id, k -> new Object());
        //加锁保证原子性
        synchronized (lockObj) {
            //根据ID查询商品库存数量
            int stockCount = selectStockCountByDB(id);
            if (stockCount > 0) {
                //减库存
                cutStock(id, count);
                res = true;
            }
        }
        return res;
    }

    private static final Map<Integer, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    public boolean JUCLockBuy(int id, int count) {
        boolean res = false;
        //所有商品都用同一把锁粒度太粗，每个商品ID对应一把锁
        ReentrantLock lock = lockMap.computeIfAbsent(id, k -> new ReentrantLock());

        try {
            if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
                //根据ID查询商品库存数量
                int stockCount = selectStockCountByDB(id);
                if (stockCount > 0) {
                    //减库存
                    cutStock(id, count);
                    res = true;
                }
            }
        } catch (InterruptedException e) {
            //等待被中断 log.error
        } finally {
            //如果持有锁就释放  （来到这里可能是等待被中断导致的，这种情况不用释放锁）
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        return res;
    }

    private static final Map<Integer, AtomicInteger> casMap = new ConcurrentHashMap<>();

    public boolean casBuy(int id, int count) {
        boolean res = false;
        AtomicInteger stockAtomic = casMap.computeIfAbsent(id, k -> {
            //内存里没有就查库
            int stock = selectStockCountByDB(id);
            return new AtomicInteger(stock);
        });

        //当前库存数量
        int stockCount = stockAtomic.get();
        if (stockCount > 0) {
            //判断乐观锁是否成功
            boolean flag;
            //失败重试 自旋
            do {
                stockCount = stockAtomic.get();
                //用库存数量当作比较对象(版本号)  如果增加库存可能会导致ABA问题，可以用携带版本号解决 AtomicStampedReference
                flag = stockAtomic.compareAndSet(stockCount, stockCount - count);
            } while (!flag);
            res = true;
        }
        return res;
    }

    public boolean xLockBuy(int id, int count) {
        boolean res = false;
        //读时加X锁(其他事务读相同商品会被阻塞) select stock_count from goods where id = ? for update
        int stockCount = selectStockCountByDBForUpdate(id);
        if (stockCount > 0) {
            //减库存 update goods set stock_count = stock_count - ? where id = ?
            cutStock(id, count);
            res = true;
        }
        return res;
    }

    public boolean OptimisticLockBuy(int id, int count) {
        boolean res = false;
        //根据ID查询商品库存数量 select stock_count from goods where id = ?
        int stockCount = selectStockCountByDBForUpdate(id);
        if (stockCount > 0) {
            //修改行数 判断乐观锁是否成功
            int row;
            //失败重试 自旋
            do {
                //查询最新数据
                stockCount = selectStockCountByDBForUpdate(id);
                //用库存数量当作比较对象(版本号)  如果增加库存可能会导致ABA问题，可以用自增版本号解决
                //减库存 update goods set stock_count = stock_count - ? where id = ? and stock_count = ?
                row = cutStockCompareVersion(id, count, stockCount);
            } while (row == 0);
            res = true;
        }
        return res;
    }


    private static RedissonClient redisson;

    static {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        config.setCodec(new JsonJacksonCodec());
        redisson = Redisson.create(config);
    }

    public boolean DistributedLock(int id, int count) {
        boolean res = false;
        RLock lock = redisson.getLock("stock_lock_" + id);
        lock.lock(100, TimeUnit.MILLISECONDS);
        try {
            //根据ID查询商品库存数量
            int stockCount = selectStockCountByDB(id);
            if (stockCount > 0) {
                //减库存
                cutStock(id, count);
                res = true;
            }
        } finally {
            lock.unlock();
        }
        return res;
    }

    public boolean lua(int id, int count) {
        StringBuilder scriptBuilder = new StringBuilder();
        // 如果给定的商品不存在，则返回-1表示失败
        scriptBuilder.append("local stock = tonumber(redis.call('GET', KEYS[1])) ");
        scriptBuilder.append("if stock > 0 then ");
        scriptBuilder.append("redis.call('DECRBY', KEYS[1], tonumber(ARGV[1])) ");
        scriptBuilder.append("return 1 ");
        scriptBuilder.append("end ");
        scriptBuilder.append("return -1 ");
        String scriptStr = scriptBuilder.toString();

        // 执行Lua脚本
        RScript script = redisson.getScript();
        Object result = script.eval(RScript.Mode.READ_WRITE,
                scriptStr,
                RScript.ReturnType.INTEGER,
                Collections.singletonList("stock_lua_" + id),
                count);
        return (Long) result > 0;
    }

}
