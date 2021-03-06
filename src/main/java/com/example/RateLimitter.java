package com.example;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * nftablesのlimit rateのような単位時間内でのアクセス数制限を行う。
 * Limit the number of access per a specified window like nftables "limit rate".
 * 注意：
 * allowAccess()によってアクセス可能と判定されたアクセスのみをアクセス数のカウント対象としている。
 * そのため、アクセス不可の時間内に何度アクセス試行をしても、アクセス不可の時間が伸びることはない。
 * アクセス不可の時間内もカウント対象に入れる場合、allowAccess()からaddAccess()を呼び出すタイミングを
 * 判定処理の前に持ってくること。
 */
public class RateLimitter implements Serializable {
    /** アクセスを追加順に保持するキュー */
    /* 実装メモ：
     * Java SE APIには、size()がO(1)で、peek()ができて、かつDequeでない単純なQueueの実装はない模様。
     * 代案として、size()がO(1)で、peek()ができるArrayDequeのインスタンスを
     * Queueインタフェース越しに操作している。
     */
    Queue<Access> accessList = new ArrayDeque<>();
    /** アクセス数をアドレスごとに計数するためのマップ */
    /* 実装メモ：
     * Setには最低限アクセス時刻さえ格納されていれば処理は可能だが、
     * CGNなどで同一アクセス元からのアクセスが同一ミリ秒内に発生した場合のハンドリングを避けるため
     * Accessオブジェクトをそのまま格納している。
     * Queueにも持っているオブジェクトのため、これにより消費メモリが増えることはないはず。
     */
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
    public synchronized boolean allowAccess(String address, long accessTime) {
        /* 実装メモ：
         * 複数スレッドから同時に呼び出される可能性があるため、安全のためsynchronized指定している。
         * リクエスト処理完了までロックする実装ではなく、処理時間も短いため問題にはならないと考えているが、
         * 必要であればQueue・Map・SetをConcurrent処理に対応した実装に置き換えること。
         */
        Access newAccess = new Access(address, accessTime);
        clearOldAccess(newAccess);

        Set<Access> targetAccesses = accessMap.get(newAccess.getKey());
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
        /* 実装メモ：
         * メソッドにくくりだしているのは、処理の見通しを良くするための他に、
         * アクセス不可の時間内もカウント対象に入れたい場合の修正を容易にするため。
         */
        accessList.add(newAccess);
        if (accessMap.get(newAccess.getKey()) == null) {
            Set<Access> newSet = new HashSet<>();
            newSet.add(newAccess);
            accessMap.put(newAccess.getKey(), newSet);
        } else {
            accessMap.get(newAccess.getKey()).add(newAccess);
        }
    }

    /**
     * 渡されたアクセスの時刻を基準に、古すぎる（タイムウィンドウを超過した）アクセスをキューとマップから削除する。
     */
    private void clearOldAccess(Access newAccess) {
        /* 実装メモ：
         * 1つのオブジェクトをQueueとSetの両方から参照していて
         * 「Queueからオブジェクトを削除したら、Setからも同オブジェクトを削除する」という処理をしたい場合、
         * 弱参照は使えない（Queueからオブジェクトが削除されても、Setからの参照はGCまでは生き残る）。
         * そのためここではQueueとSetの両方から明示的にAccessオブジェクトを削除している。
         */
        Access oldestAcceess = accessList.peek();
        while (oldestAcceess != null && oldestAcceess.getAccessTime() < newAccess.getAccessTime() - timeWindow) {
            /* 実装メモ：
             * メモリを不必要に食わないように、タイムウィンドウを超過したアクセスは
             * 別アドレスからのアクセス含め削除していく。
             * そのため、Queueにはアクセス元アドレス関係なしに追加された順にAccessオブジェクトを登録しておき
             * Queueからオブジェクトをremoveしたらアクセス元アドレス関係なしに
             * 古い順にアクセス記録が出てくるようにしておく。
             * Setが空になったらSet自体もMapから削除する。
             */
            accessList.poll();
            accessMap.get(oldestAcceess.getKey()).remove(oldestAcceess);
            if (accessMap.get(oldestAcceess.getKey()).isEmpty()) {
                accessMap.remove(oldestAcceess.getKey());
            }
            oldestAcceess = accessList.peek();
        }
    }
}


/**
 * アクセス1回分の情報を保持する。
 * アクセス元アドレスは文字列で表現するため、用途に合わせて表現をFQDN/IPv4/IPv6どれかに固定すること。
 * アクセス時刻はSystem.currentTimeMillis()の結果を想定している。
 */
class Access implements Serializable {
    /** アクセスを識別するキー（アクセス元アドレス、アドレス＋ポート番号、ユーザIDなど） */
    private final String key;
    /** アクセス時刻（ミリ秒） */
    private final long accessTime;

    /**
     * コンストラクタ
     * @param key アクセスを識別するキー（アクセス元アドレス、アドレス＋ポート番号、ユーザIDなど）
     * @param accessTime アクセス時刻（ミリ秒）
     */
    public Access(String key, long accessTime) {
        super();
        this.key = key;
        this.accessTime = accessTime;
    }

    public String getKey() {
        return key;
    }

    public long getAccessTime() {
        return accessTime;
    }
}