package java.util.concurrent.locks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import sun.misc.Unsafe;

public abstract class AbstractQueuedLongSynchronizer extends AbstractOwnableSynchronizer implements Serializable {
    private static final long HEAD;
    static final long SPIN_FOR_TIMEOUT_THRESHOLD = 1000;
    private static final long STATE;
    private static final long TAIL;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long serialVersionUID = 7373984972572414692L;
    private volatile transient Node head;
    private volatile long state;
    private volatile transient Node tail;

    public class ConditionObject implements Condition, Serializable {
        private static final int REINTERRUPT = 1;
        private static final int THROW_IE = -1;
        private static final long serialVersionUID = 1173984872572414699L;
        private transient Node firstWaiter;
        private transient Node lastWaiter;

        private Node addConditionWaiter() {
            Node t = this.lastWaiter;
            if (!(t == null || t.waitStatus == -2)) {
                unlinkCancelledWaiters();
                t = this.lastWaiter;
            }
            Node node = new Node(-2);
            if (t == null) {
                this.firstWaiter = node;
            } else {
                t.nextWaiter = node;
            }
            this.lastWaiter = node;
            return node;
        }

        private void doSignal(Node first) {
            Node node;
            do {
                node = first.nextWaiter;
                this.firstWaiter = node;
                if (node == null) {
                    this.lastWaiter = null;
                }
                first.nextWaiter = null;
                if (!AbstractQueuedLongSynchronizer.this.transferForSignal(first)) {
                    node = this.firstWaiter;
                    first = node;
                } else {
                    return;
                }
            } while (node != null);
        }

        private void doSignalAll(Node first) {
            this.firstWaiter = null;
            this.lastWaiter = null;
            do {
                Node next = first.nextWaiter;
                first.nextWaiter = null;
                AbstractQueuedLongSynchronizer.this.transferForSignal(first);
                first = next;
            } while (first != null);
        }

        private void unlinkCancelledWaiters() {
            Node t = this.firstWaiter;
            Node trail = null;
            while (t != null) {
                Node next = t.nextWaiter;
                if (t.waitStatus != -2) {
                    t.nextWaiter = null;
                    if (trail == null) {
                        this.firstWaiter = next;
                    } else {
                        trail.nextWaiter = next;
                    }
                    if (next == null) {
                        this.lastWaiter = trail;
                    }
                } else {
                    trail = t;
                }
                t = next;
            }
        }

        public final void signal() {
            if (AbstractQueuedLongSynchronizer.this.isHeldExclusively()) {
                Node first = this.firstWaiter;
                if (first != null) {
                    doSignal(first);
                    return;
                }
                return;
            }
            throw new IllegalMonitorStateException();
        }

        public final void signalAll() {
            if (AbstractQueuedLongSynchronizer.this.isHeldExclusively()) {
                Node first = this.firstWaiter;
                if (first != null) {
                    doSignalAll(first);
                    return;
                }
                return;
            }
            throw new IllegalMonitorStateException();
        }

        public final void awaitUninterruptibly() {
            Node node = addConditionWaiter();
            long savedState = AbstractQueuedLongSynchronizer.this.fullyRelease(node);
            boolean interrupted = false;
            while (!AbstractQueuedLongSynchronizer.this.isOnSyncQueue(node)) {
                LockSupport.park(this);
                if (Thread.interrupted()) {
                    interrupted = true;
                }
            }
            if (AbstractQueuedLongSynchronizer.this.acquireQueued(node, savedState) || interrupted) {
                AbstractQueuedLongSynchronizer.selfInterrupt();
            }
        }

        private int checkInterruptWhileWaiting(Node node) {
            if (Thread.interrupted()) {
                return AbstractQueuedLongSynchronizer.this.transferAfterCancelledWait(node) ? -1 : 1;
            } else {
                return 0;
            }
        }

        private void reportInterruptAfterWait(int interruptMode) throws InterruptedException {
            if (interruptMode == -1) {
                throw new InterruptedException();
            } else if (interruptMode == 1) {
                AbstractQueuedLongSynchronizer.selfInterrupt();
            }
        }

        public final void await() throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            Node node = addConditionWaiter();
            long savedState = AbstractQueuedLongSynchronizer.this.fullyRelease(node);
            int interruptMode = 0;
            while (!AbstractQueuedLongSynchronizer.this.isOnSyncQueue(node)) {
                LockSupport.park(this);
                int checkInterruptWhileWaiting = checkInterruptWhileWaiting(node);
                interruptMode = checkInterruptWhileWaiting;
                if (checkInterruptWhileWaiting != 0) {
                    break;
                }
            }
            if (AbstractQueuedLongSynchronizer.this.acquireQueued(node, savedState) && interruptMode != -1) {
                interruptMode = 1;
            }
            if (node.nextWaiter != null) {
                unlinkCancelledWaiters();
            }
            if (interruptMode != 0) {
                reportInterruptAfterWait(interruptMode);
            }
        }

        public final long awaitNanos(long nanosTimeout) throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            long deadline = System.nanoTime() + nanosTimeout;
            long initialNanos = nanosTimeout;
            Node node = addConditionWaiter();
            long savedState = AbstractQueuedLongSynchronizer.this.fullyRelease(node);
            int interruptMode = 0;
            while (!AbstractQueuedLongSynchronizer.this.isOnSyncQueue(node)) {
                if (nanosTimeout <= 0) {
                    AbstractQueuedLongSynchronizer.this.transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout > AbstractQueuedLongSynchronizer.SPIN_FOR_TIMEOUT_THRESHOLD) {
                    LockSupport.parkNanos(this, nanosTimeout);
                }
                int checkInterruptWhileWaiting = checkInterruptWhileWaiting(node);
                interruptMode = checkInterruptWhileWaiting;
                if (checkInterruptWhileWaiting != 0) {
                    break;
                }
                nanosTimeout = deadline - System.nanoTime();
            }
            if (AbstractQueuedLongSynchronizer.this.acquireQueued(node, savedState) && interruptMode != -1) {
                interruptMode = 1;
            }
            if (node.nextWaiter != null) {
                unlinkCancelledWaiters();
            }
            if (interruptMode != 0) {
                reportInterruptAfterWait(interruptMode);
            }
            long remaining = deadline - System.nanoTime();
            return remaining <= initialNanos ? remaining : Long.MIN_VALUE;
        }

        public final boolean awaitUntil(Date deadline) throws InterruptedException {
            long abstime = deadline.getTime();
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            Node node = addConditionWaiter();
            long savedState = AbstractQueuedLongSynchronizer.this.fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (!AbstractQueuedLongSynchronizer.this.isOnSyncQueue(node)) {
                if (System.currentTimeMillis() < abstime) {
                    LockSupport.parkUntil(this, abstime);
                    int checkInterruptWhileWaiting = checkInterruptWhileWaiting(node);
                    interruptMode = checkInterruptWhileWaiting;
                    if (checkInterruptWhileWaiting != 0) {
                        break;
                    }
                }
                timedout = AbstractQueuedLongSynchronizer.this.transferAfterCancelledWait(node);
                break;
            }
            if (AbstractQueuedLongSynchronizer.this.acquireQueued(node, savedState) && interruptMode != -1) {
                interruptMode = 1;
            }
            if (node.nextWaiter != null) {
                unlinkCancelledWaiters();
            }
            if (interruptMode != 0) {
                reportInterruptAfterWait(interruptMode);
            }
            if (timedout) {
                return false;
            }
            return true;
        }

        public final boolean await(long time, TimeUnit unit) throws InterruptedException {
            long nanosTimeout = unit.toNanos(time);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            long deadline = System.nanoTime() + nanosTimeout;
            Node node = addConditionWaiter();
            long savedState = AbstractQueuedLongSynchronizer.this.fullyRelease(node);
            boolean timedout = false;
            long nanosTimeout2 = nanosTimeout;
            nanosTimeout = 0;
            while (!AbstractQueuedLongSynchronizer.this.isOnSyncQueue(node)) {
                if (nanosTimeout2 <= 0) {
                    timedout = AbstractQueuedLongSynchronizer.this.transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout2 > AbstractQueuedLongSynchronizer.SPIN_FOR_TIMEOUT_THRESHOLD) {
                    LockSupport.parkNanos(this, nanosTimeout2);
                }
                int checkInterruptWhileWaiting = checkInterruptWhileWaiting(node);
                nanosTimeout = checkInterruptWhileWaiting;
                if (checkInterruptWhileWaiting != 0) {
                    break;
                }
                nanosTimeout2 = deadline - System.nanoTime();
            }
            if (AbstractQueuedLongSynchronizer.this.acquireQueued(node, savedState) && nanosTimeout != -1) {
                nanosTimeout = 1;
            }
            if (node.nextWaiter != null) {
                unlinkCancelledWaiters();
            }
            if (nanosTimeout != null) {
                reportInterruptAfterWait(nanosTimeout);
            }
            if (timedout) {
                return false;
            }
            return true;
        }

        final boolean isOwnedBy(AbstractQueuedLongSynchronizer sync) {
            return sync == AbstractQueuedLongSynchronizer.this;
        }

        protected final boolean hasWaiters() {
            if (AbstractQueuedLongSynchronizer.this.isHeldExclusively()) {
                for (Node w = this.firstWaiter; w != null; w = w.nextWaiter) {
                    if (w.waitStatus == -2) {
                        return true;
                    }
                }
                return false;
            }
            throw new IllegalMonitorStateException();
        }

        protected final int getWaitQueueLength() {
            if (AbstractQueuedLongSynchronizer.this.isHeldExclusively()) {
                int n = 0;
                for (Node w = this.firstWaiter; w != null; w = w.nextWaiter) {
                    if (w.waitStatus == -2) {
                        n++;
                    }
                }
                return n;
            }
            throw new IllegalMonitorStateException();
        }

        protected final Collection<Thread> getWaitingThreads() {
            if (AbstractQueuedLongSynchronizer.this.isHeldExclusively()) {
                ArrayList<Thread> list = new ArrayList();
                for (Node w = this.firstWaiter; w != null; w = w.nextWaiter) {
                    if (w.waitStatus == -2) {
                        Thread t = w.thread;
                        if (t != null) {
                            list.add(t);
                        }
                    }
                }
                return list;
            }
            throw new IllegalMonitorStateException();
        }
    }

    protected AbstractQueuedLongSynchronizer() {
    }

    protected final long getState() {
        return this.state;
    }

    protected final void setState(long newState) {
        U.putLongVolatile(this, STATE, newState);
    }

    protected final boolean compareAndSetState(long expect, long update) {
        return U.compareAndSwapLong(this, STATE, expect, update);
    }

    private Node enq(Node node) {
        while (true) {
            Node oldTail = this.tail;
            if (oldTail != null) {
                U.putObject(node, Node.PREV, oldTail);
                if (compareAndSetTail(oldTail, node)) {
                    oldTail.next = node;
                    return oldTail;
                }
            } else {
                initializeSyncQueue();
            }
        }
    }

    private Node addWaiter(Node mode) {
        Node node = new Node(mode);
        while (true) {
            Node oldTail = this.tail;
            if (oldTail != null) {
                U.putObject(node, Node.PREV, oldTail);
                if (compareAndSetTail(oldTail, node)) {
                    oldTail.next = node;
                    return node;
                }
            } else {
                initializeSyncQueue();
            }
        }
    }

    private void setHead(Node node) {
        this.head = node;
        node.thread = null;
        node.prev = null;
    }

    private void unparkSuccessor(Node node) {
        int ws = node.waitStatus;
        if (ws < 0) {
            node.compareAndSetWaitStatus(ws, 0);
        }
        Node s = node.next;
        if (s == null || s.waitStatus > 0) {
            s = null;
            Node p = this.tail;
            while (p != node && p != null) {
                if (p.waitStatus <= 0) {
                    s = p;
                }
                p = p.prev;
            }
        }
        if (s != null) {
            LockSupport.unpark(s.thread);
        }
    }

    private void doReleaseShared() {
        while (true) {
            Node h = this.head;
            if (!(h == null || h == this.tail)) {
                int ws = h.waitStatus;
                if (ws == -1) {
                    if (h.compareAndSetWaitStatus(-1, 0)) {
                        unparkSuccessor(h);
                    }
                } else if (ws == 0 && !h.compareAndSetWaitStatus(0, -3)) {
                }
            }
            if (h == this.head) {
                return;
            }
        }
    }

    private void setHeadAndPropagate(Node node, long propagate) {
        Node node2;
        Node h = this.head;
        setHead(node);
        if (propagate <= 0 && h != null && h.waitStatus >= 0) {
            node2 = this.head;
            h = node2;
            if (node2 != null && h.waitStatus >= 0) {
                return;
            }
        }
        node2 = node.next;
        if (node2 == null || node2.isShared()) {
            doReleaseShared();
        }
    }

    private void cancelAcquire(Node node) {
        if (node != null) {
            Node node2;
            node.thread = null;
            Node pred = node.prev;
            while (pred.waitStatus > 0) {
                node2 = pred.prev;
                pred = node2;
                node.prev = node2;
            }
            node2 = pred.next;
            node.waitStatus = 1;
            if (node == this.tail && compareAndSetTail(node, pred)) {
                pred.compareAndSetNext(node2, null);
            } else {
                if (pred != this.head) {
                    int i = pred.waitStatus;
                    int ws = i;
                    if ((i == -1 || (ws <= 0 && pred.compareAndSetWaitStatus(ws, -1))) && pred.thread != null) {
                        Node next = node.next;
                        if (next != null && next.waitStatus <= 0) {
                            pred.compareAndSetNext(node2, next);
                        }
                        node.next = node;
                    }
                }
                unparkSuccessor(node);
                node.next = node;
            }
        }
    }

    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        int ws = pred.waitStatus;
        if (ws == -1) {
            return true;
        }
        if (ws > 0) {
            do {
                Node node2 = pred.prev;
                pred = node2;
                node.prev = node2;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            pred.compareAndSetWaitStatus(ws, -1);
        }
        return false;
    }

    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }

    private final boolean parkAndCheckInterrupt() {
        LockSupport.park(this);
        return Thread.interrupted();
    }

    final boolean acquireQueued(Node node, long arg) {
        boolean interrupted = false;
        while (true) {
            try {
                Node p = node.predecessor();
                if (p == this.head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null;
                    return interrupted;
                } else if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()) {
                    interrupted = true;
                }
            } catch (Throwable th) {
                cancelAcquire(node);
            }
        }
    }

    private void doAcquireInterruptibly(long arg) throws InterruptedException {
        Node node = addWaiter(Node.EXCLUSIVE);
        while (true) {
            try {
                Node p = node.predecessor();
                if (p == this.head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null;
                    return;
                } else if (shouldParkAfterFailedAcquire(p, node)) {
                    if (parkAndCheckInterrupt()) {
                        throw new InterruptedException();
                    }
                }
            } catch (Throwable th) {
                cancelAcquire(node);
            }
        }
    }

    private boolean doAcquireNanos(long arg, long nanosTimeout) throws InterruptedException {
        if (nanosTimeout <= 0) {
            return false;
        }
        long deadline = System.nanoTime() + nanosTimeout;
        Node node = addWaiter(Node.EXCLUSIVE);
        while (true) {
            try {
                Node p = node.predecessor();
                if (p == this.head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null;
                    return true;
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0) {
                    break;
                }
                if (shouldParkAfterFailedAcquire(p, node) && nanosTimeout > SPIN_FOR_TIMEOUT_THRESHOLD) {
                    LockSupport.parkNanos(this, nanosTimeout);
                }
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
            } finally {
                cancelAcquire(node);
            }
        }
        return false;
    }

    private void doAcquireShared(long arg) {
        Node p;
        long r;
        Node node = addWaiter(Node.SHARED);
        boolean interrupted = false;
        while (true) {
            try {
                p = node.predecessor();
                if (p == this.head) {
                    r = tryAcquireShared(arg);
                    if (r >= 0) {
                        break;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()) {
                    interrupted = true;
                }
            } catch (Throwable th) {
                cancelAcquire(node);
            }
        }
        setHeadAndPropagate(node, r);
        p.next = null;
        if (interrupted) {
            selfInterrupt();
        }
    }

    private void doAcquireSharedInterruptibly(long arg) throws InterruptedException {
        Node node = addWaiter(Node.SHARED);
        while (true) {
            try {
                Node p = node.predecessor();
                if (p == this.head) {
                    long r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null;
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node)) {
                    if (parkAndCheckInterrupt()) {
                        throw new InterruptedException();
                    }
                }
            } catch (Throwable th) {
                cancelAcquire(node);
            }
        }
    }

    private boolean doAcquireSharedNanos(long arg, long nanosTimeout) throws InterruptedException {
        if (nanosTimeout <= 0) {
            return false;
        }
        long deadline = System.nanoTime() + nanosTimeout;
        Node node = addWaiter(Node.SHARED);
        while (true) {
            try {
                Node p = node.predecessor();
                if (p == this.head) {
                    long r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null;
                        return true;
                    }
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0) {
                    break;
                }
                if (shouldParkAfterFailedAcquire(p, node) && nanosTimeout > SPIN_FOR_TIMEOUT_THRESHOLD) {
                    LockSupport.parkNanos(this, nanosTimeout);
                }
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
            } finally {
                cancelAcquire(node);
            }
        }
        return false;
    }

    protected boolean tryAcquire(long arg) {
        throw new UnsupportedOperationException();
    }

    protected boolean tryRelease(long arg) {
        throw new UnsupportedOperationException();
    }

    protected long tryAcquireShared(long arg) {
        throw new UnsupportedOperationException();
    }

    protected boolean tryReleaseShared(long arg) {
        throw new UnsupportedOperationException();
    }

    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

    public final void acquire(long arg) {
        if (!tryAcquire(arg) && acquireQueued(addWaiter(Node.EXCLUSIVE), arg)) {
            selfInterrupt();
        }
    }

    public final void acquireInterruptibly(long arg) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        } else if (!tryAcquire(arg)) {
            doAcquireInterruptibly(arg);
        }
    }

    public final boolean tryAcquireNanos(long arg, long nanosTimeout) throws InterruptedException {
        if (!Thread.interrupted()) {
            return tryAcquire(arg) || doAcquireNanos(arg, nanosTimeout);
        } else {
            throw new InterruptedException();
        }
    }

    public final boolean release(long arg) {
        if (!tryRelease(arg)) {
            return false;
        }
        Node h = this.head;
        if (!(h == null || h.waitStatus == 0)) {
            unparkSuccessor(h);
        }
        return true;
    }

    public final void acquireShared(long arg) {
        if (tryAcquireShared(arg) < 0) {
            doAcquireShared(arg);
        }
    }

    public final void acquireSharedInterruptibly(long arg) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        } else if (tryAcquireShared(arg) < 0) {
            doAcquireSharedInterruptibly(arg);
        }
    }

    public final boolean tryAcquireSharedNanos(long arg, long nanosTimeout) throws InterruptedException {
        if (!Thread.interrupted()) {
            return tryAcquireShared(arg) >= 0 || doAcquireSharedNanos(arg, nanosTimeout);
        } else {
            throw new InterruptedException();
        }
    }

    public final boolean releaseShared(long arg) {
        if (!tryReleaseShared(arg)) {
            return false;
        }
        doReleaseShared();
        return true;
    }

    public final boolean hasQueuedThreads() {
        return this.head != this.tail;
    }

    public final boolean hasContended() {
        return this.head != null;
    }

    public final Thread getFirstQueuedThread() {
        return this.head == this.tail ? null : fullGetFirstQueuedThread();
    }

    /* JADX WARNING: Missing block: B:7:0x0013, code skipped:
            if (r0 == null) goto L_0x0015;
     */
    /* JADX WARNING: Missing block: B:15:0x0028, code skipped:
            if (r0 != null) goto L_0x002a;
     */
    /* JADX WARNING: Missing block: B:16:0x002a, code skipped:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Thread fullGetFirstQueuedThread() {
        Node s;
        Thread thread;
        Thread st;
        Node node = this.head;
        Node h = node;
        if (node != null) {
            node = h.next;
            s = node;
            if (node != null && s.prev == this.head) {
                thread = s.thread;
                st = thread;
            }
        }
        node = this.head;
        h = node;
        if (node != null) {
            node = h.next;
            s = node;
            if (node != null && s.prev == this.head) {
                thread = s.thread;
                st = thread;
            }
        }
        thread = null;
        s = this.tail;
        while (s != null && s != this.head) {
            st = s.thread;
            if (st != null) {
                thread = st;
            }
            s = s.prev;
        }
        return thread;
    }

    public final boolean isQueued(Thread thread) {
        if (thread != null) {
            for (Node p = this.tail; p != null; p = p.prev) {
                if (p.thread == thread) {
                    return true;
                }
            }
            return false;
        }
        throw new NullPointerException();
    }

    final boolean apparentlyFirstQueuedIsExclusive() {
        Node node = this.head;
        Node h = node;
        if (node != null) {
            node = h.next;
            Node s = node;
            if (!(node == null || s.isShared() || s.thread == null)) {
                return true;
            }
        }
        return false;
    }

    public final boolean hasQueuedPredecessors() {
        Node t = this.tail;
        Node h = this.head;
        if (h != t) {
            Node node = h.next;
            Node s = node;
            if (node == null || s.thread != Thread.currentThread()) {
                return true;
            }
        }
        return false;
    }

    public final int getQueueLength() {
        int n = 0;
        for (Node p = this.tail; p != null; p = p.prev) {
            if (p.thread != null) {
                n++;
            }
        }
        return n;
    }

    public final Collection<Thread> getQueuedThreads() {
        ArrayList<Thread> list = new ArrayList();
        for (Node p = this.tail; p != null; p = p.prev) {
            Thread t = p.thread;
            if (t != null) {
                list.add(t);
            }
        }
        return list;
    }

    public final Collection<Thread> getExclusiveQueuedThreads() {
        ArrayList<Thread> list = new ArrayList();
        for (Node p = this.tail; p != null; p = p.prev) {
            if (!p.isShared()) {
                Thread t = p.thread;
                if (t != null) {
                    list.add(t);
                }
            }
        }
        return list;
    }

    public final Collection<Thread> getSharedQueuedThreads() {
        ArrayList<Thread> list = new ArrayList();
        for (Node p = this.tail; p != null; p = p.prev) {
            if (p.isShared()) {
                Thread t = p.thread;
                if (t != null) {
                    list.add(t);
                }
            }
        }
        return list;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(super.toString());
        stringBuilder.append("[State = ");
        stringBuilder.append(getState());
        stringBuilder.append(", ");
        stringBuilder.append(hasQueuedThreads() ? "non" : "");
        stringBuilder.append("empty queue]");
        return stringBuilder.toString();
    }

    final boolean isOnSyncQueue(Node node) {
        if (node.waitStatus == -2 || node.prev == null) {
            return false;
        }
        if (node.next != null) {
            return true;
        }
        return findNodeFromTail(node);
    }

    private boolean findNodeFromTail(Node node) {
        for (Node p = this.tail; p != node; p = p.prev) {
            if (p == null) {
                return false;
            }
        }
        return true;
    }

    final boolean transferForSignal(Node node) {
        if (!node.compareAndSetWaitStatus(-2, 0)) {
            return false;
        }
        Node p = enq(node);
        int ws = p.waitStatus;
        if (ws > 0 || !p.compareAndSetWaitStatus(ws, -1)) {
            LockSupport.unpark(node.thread);
        }
        return true;
    }

    final boolean transferAfterCancelledWait(Node node) {
        if (node.compareAndSetWaitStatus(-2, 0)) {
            enq(node);
            return true;
        }
        while (!isOnSyncQueue(node)) {
            Thread.yield();
        }
        return false;
    }

    final long fullyRelease(Node node) {
        try {
            long savedState = getState();
            if (release(savedState)) {
                return savedState;
            }
            throw new IllegalMonitorStateException();
        } catch (Throwable th) {
            node.waitStatus = 1;
        }
    }

    public final boolean owns(ConditionObject condition) {
        return condition.isOwnedBy(this);
    }

    public final boolean hasWaiters(ConditionObject condition) {
        if (owns(condition)) {
            return condition.hasWaiters();
        }
        throw new IllegalArgumentException("Not owner");
    }

    public final int getWaitQueueLength(ConditionObject condition) {
        if (owns(condition)) {
            return condition.getWaitQueueLength();
        }
        throw new IllegalArgumentException("Not owner");
    }

    public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
        if (owns(condition)) {
            return condition.getWaitingThreads();
        }
        throw new IllegalArgumentException("Not owner");
    }

    static {
        try {
            STATE = U.objectFieldOffset(AbstractQueuedLongSynchronizer.class.getDeclaredField("state"));
            HEAD = U.objectFieldOffset(AbstractQueuedLongSynchronizer.class.getDeclaredField("head"));
            TAIL = U.objectFieldOffset(AbstractQueuedLongSynchronizer.class.getDeclaredField("tail"));
            Class cls = LockSupport.class;
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    private final void initializeSyncQueue() {
        Unsafe unsafe = U;
        long j = HEAD;
        Node node = new Node();
        Node h = node;
        if (unsafe.compareAndSwapObject(this, j, null, node)) {
            this.tail = h;
        }
    }

    private final boolean compareAndSetTail(Node expect, Node update) {
        return U.compareAndSwapObject(this, TAIL, expect, update);
    }
}
