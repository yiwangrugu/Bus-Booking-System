package Util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockManager {
    private static final Map<Integer, Lock> busLocks = new ConcurrentHashMap<>();
    private static final Map<Integer, Lock> orderLocks = new ConcurrentHashMap<>();

    public static Lock getBusLock(int bno) {
        return busLocks.computeIfAbsent(bno, k -> new ReentrantLock());
    }

    public static Lock getOrderLock(int btno) {
        return orderLocks.computeIfAbsent(btno, k -> new ReentrantLock());
    }

    public static void releaseBusLock(int bno) {
        Lock lock = busLocks.get(bno);
        if (lock != null) {
            lock.unlock();
        }
    }

    public static void releaseOrderLock(int btno) {
        Lock lock = orderLocks.get(btno);
        if (lock != null) {
            lock.unlock();
        }
    }
}
