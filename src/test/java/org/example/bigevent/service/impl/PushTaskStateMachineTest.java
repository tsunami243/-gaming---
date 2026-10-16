package org.example.bigevent.service.impl;
import org.example.bigevent.mapper.PushTaskMapper;
import org.example.bigevent.pojo.PushTask;
import org.example.bigevent.pojo.PushTaskStatus;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 状态机单元测试：覆盖合法流转、非法流转、乐观锁冲突、状态持久化。
 */
class PushTaskStateMachineTest {

    @Test
    void transitionTableCoversResumeLifecycle() {
        assertTrue(PushTaskStatus.PENDING.canTransitionTo(PushTaskStatus.SENDING));
        assertTrue(PushTaskStatus.SENDING.canTransitionTo(PushTaskStatus.SUCCESS));
        assertTrue(PushTaskStatus.SENDING.canTransitionTo(PushTaskStatus.FAILED));
        assertTrue(PushTaskStatus.SENDING.canTransitionTo(PushTaskStatus.DEAD));
        assertTrue(PushTaskStatus.FAILED.canTransitionTo(PushTaskStatus.SENDING));
        assertTrue(PushTaskStatus.FAILED.canTransitionTo(PushTaskStatus.DEAD));
        assertTrue(PushTaskStatus.DEAD.canTransitionTo(PushTaskStatus.SUCCESS));
    }

    @Test
    void illegalTransitionIsRejected() {
        assertFalse(PushTaskStatus.PENDING.canTransitionTo(PushTaskStatus.SUCCESS));
        assertFalse(PushTaskStatus.SUCCESS.canTransitionTo(PushTaskStatus.DEAD));
        assertFalse(PushTaskStatus.DEAD.canTransitionTo(PushTaskStatus.PENDING));
    }

    @Test
    void stateMachinePersistsTransitionAndInvokesHandler() {
        FakeMapper mapper = new FakeMapper();
        PushTaskStateMachine machine = new PushTaskStateMachine(mapper);
        PushTask task = new PushTask();
        task.setMessageId("m1");
        task.setStatus(PushTaskStatus.PENDING.name());
        mapper.insert(task);

        boolean ok = machine.transition(task, PushTaskStatus.PENDING, PushTaskStatus.SENDING, null);
        assertTrue(ok);
        assertEquals("SENDING", mapper.statusOf("m1"));
        assertEquals("SENDING", task.getStatus());

        boolean ok2 = machine.transition(task, PushTaskStatus.SENDING, PushTaskStatus.FAILED, "45009 频控");
        assertTrue(ok2);
        assertEquals("FAILED", mapper.statusOf("m1"));
        assertEquals("45009 频控", mapper.errorOf("m1"));
    }

    @Test
    void illegalTransitionThrows() {
        FakeMapper mapper = new FakeMapper();
        PushTaskStateMachine machine = new PushTaskStateMachine(mapper);
        PushTask task = new PushTask();
        task.setMessageId("m2");
        task.setStatus(PushTaskStatus.PENDING.name());
        assertThrows(IllegalStateException.class,
            () -> machine.transition(task, PushTaskStatus.PENDING, PushTaskStatus.DEAD, null));
    }

    @Test
    void concurrentConflictReturnsFalse() {
        FakeMapper mapper = new FakeMapper();
        PushTaskStateMachine machine = new PushTaskStateMachine(mapper);
        PushTask task = new PushTask();
        task.setMessageId("m3");
        task.setStatus(PushTaskStatus.PENDING.name());
        mapper.insert(task);
        mapper.setStatus("m3", "SENDING"); // 模拟其他消费者已抢先流转

        boolean ok = machine.transition(task, PushTaskStatus.PENDING, PushTaskStatus.SENDING, null);
        assertFalse(ok);
    }

    /** 内存版 Mapper：模拟乐观锁 WHERE status = ? 语义 */
    static class FakeMapper implements PushTaskMapper {
        private final java.util.Map<String, String> statuses = new java.util.concurrent.ConcurrentHashMap<>();
        private final java.util.Map<String, String> errors = new java.util.concurrent.ConcurrentHashMap<>();

        @Override public void insert(PushTask task) { statuses.put(task.getMessageId(), task.getStatus()); }
        @Override public PushTask findByMessageId(String messageId) {
            PushTask t = new PushTask();
            t.setMessageId(messageId);
            t.setStatus(statuses.getOrDefault(messageId, "PENDING"));
            return t;
        }
        @Override public int transition(String messageId, String fromStatus, String toStatus, String error) {
            String current = statuses.get(messageId);
            if (current == null || !current.equals(fromStatus)) {
                return 0; // 乐观锁：状态已被其他线程改变
            }
            statuses.put(messageId, toStatus);
            if (error != null) {
                errors.put(messageId, error);
            }
            return 1;
        }
        @Override public int bumpRetry(String messageId) { return 1; }
        @Override public int updateError(String messageId, String error) { errors.put(messageId, error); return 1; }
        @Override public List<PushTask> listByStatus(String status) { return List.of(); }
        @Override public long countByStatus(String status) { return 0; }

        String statusOf(String messageId) { return statuses.get(messageId); }
        String errorOf(String messageId) { return errors.get(messageId); }
        void setStatus(String messageId, String status) { statuses.put(messageId, status); }
    }
}
