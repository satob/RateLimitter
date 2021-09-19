package com.example;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * nftablesのlimit rateのような単位時間内でのアクセス数制限を行う。
 * Limit the number of access per a specified window like nftables "limit rate".
 */
public class RateLimitter {
    /** アクセスを追加順に保持するキュー */
    Queue<Access> accessList = new ArrayDeque<>();
    /** アクセス数をアドレスごとに計数するためのマップ */
    Map<String, Set<Access>> accessMap = new HashMap<>();
    /** アクセス数を制限するタイムウィンドウ（ミリ秒） */
    private final long timeWindow;
    /** アクセス回数の上限 */
    private final int accessLimit;

    /**
     * タイムウィンドウとアクセス回数上限を指定してインスタンスを作成する。
     * @param timeWindow タイムウィンドウ幅（ミリ秒）
     * @param accessLimit アクセス回数上限。この回数まではアクセスが許可される。
     */
    public RateLimitter(long timeWindow, int accessLimit) {
        this.timeWindow = timeWindow;
        this.accessLimit = accessLimit;
    }

    /**
     * デフォルトのタイムウィンドウ（60秒）とアクセス回数上限（10回）を指定してインスタンスを作成する。
     */
    public RateLimitter() {
        this.timeWindow = 60*1000;
        this.accessLimit = 10;
    }

    /**
     * アクセスが許可されるか判定する。
     * @return アクセス可能な場合はtrue、アクセス不可の場合はfalse
     */
    public synchronized boolean allowAccess(Access newAccess) {
        clearOldAccess(newAccess);

        Set<Access> targetAccesses = accessMap.get(newAccess.getAddress());
        if (targetAccesses != null && targetAccesses.size() >= accessLimit) {
            return false;
        } else {
            addAccess(newAccess);
            return true;
        }
    }

    /**
     * 渡されたアクセスをキューとマップに追加する。
     */
    private void addAccess(Access newAccess) {
        accessList.add(newAccess);
        if (accessMap.get(newAccess.getAddress()) == null) {
            Set<Access> newSet = new HashSet<>();
            newSet.add(newAccess);
            accessMap.put(newAccess.getAddress(), newSet);
        } else {
            accessMap.get(newAccess.getAddress()).add(newAccess);
        }
    }

    /**
     * 渡されたアクセスの時刻を基準に、古すぎる（タイムウィンドウを超過した）アクセスをキューとマップから削除する。
     */
    private void clearOldAccess(Access newAccess) {
        Access oldestAcceess = accessList.peek();
        while (oldestAcceess != null && oldestAcceess.getAccessTime() < newAccess.getAccessTime() - timeWindow) {
            accessList.poll();
            accessMap.get(oldestAcceess.getAddress()).remove(oldestAcceess);
            if (accessMap.get(oldestAcceess.getAddress()).isEmpty()) {
                accessMap.remove(oldestAcceess.getAddress());
            }
            oldestAcceess = accessList.peek();
        }
    }
}