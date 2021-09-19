package com.example;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.example.Access;
import com.example.RateLimitter;

class RateLimitterTest {

    @Test
    void allowAccessTest() {
        // 10回目まではアクセスが許可されること
        RateLimitter limitter = new RateLimitter();
        for(int i=0; i<10; i++) {
            assertTrue(limitter.allowAccess(new Access("127.0.0.1", System.currentTimeMillis())));
        }
    }

    @Test
    void denyAccessTest() {
        // 10回目まではアクセスが許可され、11回目はアクセスが拒否されること
        RateLimitter limitter = new RateLimitter();
        for(int i=0; i<10; i++) {
            assertTrue(limitter.allowAccess(new Access("127.0.0.1", System.currentTimeMillis())));
        }
        assertFalse(limitter.allowAccess(new Access("127.0.0.1", System.currentTimeMillis())));
    }

    @Test
    void allowAccessFromTwoOrMoreAddressTest() {
        // 複数のアドレスからアクセスした場合に、別々にアクセス回数がカウントされること
        RateLimitter limitter = new RateLimitter();
        for(int i=0; i<10; i++) {
            assertTrue(limitter.allowAccess(new Access("127.0.0.1", System.currentTimeMillis())));
        }
        for(int i=0; i<10; i++) {
            assertTrue(limitter.allowAccess(new Access("127.0.0.2", System.currentTimeMillis())));
        }
    }

    @Test
    void denyAccessFromTwoOrMoreAddressTest() {
        // 複数のアドレスからアクセスした場合に、別々にアクセス回数の許可・拒否が判定されること
        RateLimitter limitter = new RateLimitter();
        for(int i=0; i<10; i++) {
            assertTrue(limitter.allowAccess(new Access("127.0.0.1", System.currentTimeMillis())));
        }
        assertFalse(limitter.allowAccess(new Access("127.0.0.1", System.currentTimeMillis())));
        for(int i=0; i<10; i++) {
            assertTrue(limitter.allowAccess(new Access("127.0.0.2", System.currentTimeMillis())));
        }
        assertFalse(limitter.allowAccess(new Access("127.0.0.2", System.currentTimeMillis())));
    }

    @Test
    void oldAccessRemoveTest() {
        // タイムウィンドウが過ぎたアクセスは削除されること
        RateLimitter limitter = new RateLimitter();
        for(int i=0; i<10; i++) {
            assertTrue(limitter.allowAccess(new Access("127.0.0.1", System.currentTimeMillis())));
        }
        // 渡されたアクセス元以外からのアクセスも、タイムウィンドウが過ぎたら削除されること
        for(int i=0; i<10; i++) {
            assertTrue(limitter.allowAccess(new Access("127.0.0.2", System.currentTimeMillis())));
        }
        assertTrue(limitter.allowAccess(new Access("127.0.0.1", System.currentTimeMillis() + 60*1000 + 1)));
        assertTrue(limitter.accessList.size() < 10);
        // メモリ消費を抑えるため、空になったSetも削除されること
        assertTrue(limitter.accessMap.get("127.0.0.2") == null);
    }

    @Test
    void setLimitTest() {
        // タイムウィンドウとアクセス回数上限をコンストラクタで指定できること
        RateLimitter limitter = new RateLimitter(1000, 3);
        for(int i=0; i<3; i++) {
            assertTrue(limitter.allowAccess(new Access("127.0.0.1", System.currentTimeMillis())));
        }
        assertFalse(limitter.allowAccess(new Access("127.0.0.1", System.currentTimeMillis())));
        assertTrue(limitter.allowAccess(new Access("127.0.0.1", System.currentTimeMillis() + 1000 + 1)));
    }
}
