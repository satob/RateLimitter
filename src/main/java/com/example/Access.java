package com.example;

/**
 * アクセス1回分の情報を保持する。
 * アクセス元アドレスは文字列で表現するため、用途に合わせて表現をFQDN/IPv4/IPv6どれかに固定すること。
 * アクセス時刻はSystem.currentTimeMillis()の結果を想定している。
 */
public class Access {
    /** アクセス元アドレス */
    private String address;
    /** アクセス時刻（ミリ秒） */
    private long accessTime;

    /**
     * コンストラクタ
     * @param address アクセス元アドレス
     * @param accessTime アクセス時刻（ミリ秒）
     */
    public Access(String address, long accessTime) {
        super();
        this.address = address;
        this.accessTime = accessTime;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getAccessTime() {
        return accessTime;
    }

    public void setAccessTime(long accessTime) {
        this.accessTime = accessTime;
    }
}