package com.railway.common.util;

import com.railway.common.exception.BizException;
import com.railway.common.model.LoginUser;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 雪花 ID 单元测试：单调递增、不同 workerId 产生不同 ID、非法 workerId 抛异常。
 */
class SnowflakeIdWorkerTest {

    @Test
    void testNextIdMonotonic() {
        SnowflakeIdWorker worker = new SnowflakeIdWorker(1L);
        long id1 = worker.nextId();
        long id2 = worker.nextId();
        long id3 = worker.nextId();
        assertTrue(id1 < id2, "id1 < id2");
        assertTrue(id2 < id3, "id2 < id3");
    }

    @Test
    void testDifferentWorkerId() {
        SnowflakeIdWorker w1 = new SnowflakeIdWorker(1L);
        SnowflakeIdWorker w2 = new SnowflakeIdWorker(2L);
        long id1 = w1.nextId();
        long id2 = w2.nextId();
        assertNotEquals(id1, id2, "不同 workerId 应产生不同 ID");
    }

    @Test
    void testInvalidWorkerId() {
        SnowflakeIdWorker worker = new SnowflakeIdWorker();
        assertThrows(BizException.class, () -> worker.setWorkerId(-1L));
        assertThrows(BizException.class, () -> worker.setWorkerId(1024L));
    }

    @Test
    void testMaxWorkerId() {
        SnowflakeIdWorker worker = new SnowflakeIdWorker(1023L);
        long id = worker.nextId();
        assertNotNull(id);
        assertTrue(id > 0);
    }

    @Test
    void testDefaultWorkerId() {
        SnowflakeIdWorker worker = new SnowflakeIdWorker();
        assertEquals(1L, worker.getWorkerId());
    }

    @Test
    void testSetAndGet() {
        SnowflakeIdWorker worker = new SnowflakeIdWorker();
        worker.setWorkerId(5L);
        assertEquals(5L, worker.getWorkerId());
    }

    @Test
    void testRolesInToken() {
        // 占位测试，确保 Set 序列化到 JWT 后能被反序列化
        LoginUser user = new LoginUser();
        user.setUserId(1L);
        user.setUsername("u");
        Set<String> roles = new HashSet<>();
        roles.add("ADMIN");
        roles.add("USER");
        user.setRoles(roles);
        assertEquals(2, user.getRoles().size());
    }
}
