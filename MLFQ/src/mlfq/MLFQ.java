/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mlfq;

import com.sun.tracing.ProbeName;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

/**
 *
 * @author dmitry
 */
class Process {

    public static final int PROCESS_UNFINISHED = 0;
    public static final int PROCESS_FINISHED = 1;
    public static final int PROCESS_QUANTUM_OVER = 2;
    public static final int PROCESS_BLOCKED = 3;

    private final int pid;
    private final Random rand;
    private final int startTime;
    private int timeToLive;
    private int currentStartTime;
    private final int processTime;
    private int lifeTime;
    private final int ioProbability;
    private int currentQueue;

    public Process(int pid, int startTime, int processTime, int ioProbability) {
        this.currentStartTime = this.startTime = startTime;
        this.processTime = processTime;
        this.lifeTime = 0;
        this.timeToLive = 0;
        this.pid = pid;
        this.ioProbability = ioProbability;
        rand = new Random();
    }

    public Process(int pid, int startTime, int processTime, int ioProbability, int currentQueue, int currentQuantum) {
        this(pid, startTime, processTime, ioProbability);
        this.currentQueue = currentQueue;
        this.timeToLive = currentQuantum;
    }

    public char step() {
        int ioOper;
        MLFQ.printLog("\t[EVENT] Running process " + getPid() + " on queue " + getCurrentQueue() + "[ ttl : " + this.timeToLive + " , lifetime : " + this.lifeTime + ", processT : " + this.processTime + "]");
        this.lifeTime++;
        this.timeToLive--;
        MLFQ.printLog(", now [ ttl : " + this.timeToLive + " , lifetime : " + this.lifeTime + ", processT : " + this.processTime + "]");
        if (this.lifeTime < this.processTime) {

            if (this.timeToLive > 0) {
                ioOper = rand.nextInt(101);

                if (this.ioProbability > ioOper) {
                    return Process.PROCESS_BLOCKED;
                } else {
                    return Process.PROCESS_UNFINISHED;
                }

            } else {
                return Process.PROCESS_QUANTUM_OVER;
            }

        } else {
            return Process.PROCESS_FINISHED;
        }
    }

    public void changeCurrentQueue(int queue) {
        this.currentQueue = queue;
    }

    public void changeTTL(int newTTL) {
        this.timeToLive = newTTL;
    }

    public int getPid() {
        return pid;
    }

    public int getStartTime() {
        return startTime;
    }

    public int getTimeToLive() {
        return timeToLive;
    }

    public int getCurrentStartTime() {
        return currentStartTime;
    }

    public int getProcessTime() {
        return processTime;
    }

    public int getIoProbability() {
        return ioProbability;
    }

    public int getCurrentQueue() {
        return currentQueue;
    }

}

class ProcessQueue {

    private final int quantum;
    private final Deque<Process> executionQueue;
    private final int priority;
    private final ProcessPool caller;

    public ProcessQueue(ProcessPool calling, int quantum, int priority) {
        this.quantum = quantum;
        this.executionQueue = new LinkedList<>();
        this.priority = priority;
        this.caller = calling;
    }

    /**
     * Put the process one queue bellow;
     *
     * @param p - process that will be demoted
     */
    public void demoteProcess(Process p) {
        ProcessQueue queue = caller.getProcessQueues()[priority < (caller.getQueuesAmount() - 1) ? priority + 1 : priority];
        p.changeCurrentQueue(queue.getPriority());
        p.changeTTL(queue.getQuantum());
        queue.getExecutionQueue().add(p);
    }

    /**
     * It's assumed that the queue will only step if it can step
     */
    public void step() {
        MLFQ.printLog("\n[THREAD] Current time : " + caller.getTimestamp() + " / Current Slice Time : " + caller.getCurrentSlice() + "\n");
        Process executing = executionQueue.peekFirst();
        boolean sliced = false;
        char step;

        step = executing.step();
        MLFQ.printStepType(step);
        this.caller.increaseTimestamp();
        this.caller.decreaseSliceTime();

        if (this.caller.getCurrentSlice() <= 0) {
            sliced = true;
            MLFQ.printLog("\n[THREAD] Current time : " + caller.getTimestamp() + " / Current Slice Time : " + caller.getCurrentSlice() + "\n");
            MLFQ.printLog("\t[EVENT] A Slice occurs");
            this.caller.resetSliceTime();
        }


        /*
                0 - unfinished;
                1 - finished;
                2 - quantum over;
                3 - blocked (io)
         */
        if (step == Process.PROCESS_FINISHED) {
            executing = executionQueue.pollFirst();
            caller.getFinishedProcesses().put(executing.getPid(), executing);
        } else if (step == Process.PROCESS_BLOCKED) {
            executing = executionQueue.pollFirst();
            executing.changeTTL(caller.getProcessQueues()[executing.getCurrentQueue()].getQuantum());
            caller.getWaitQueue().add(executing);
        }

        if (sliced) {
            if (step == Process.PROCESS_UNFINISHED) {
                executing = executionQueue.pollFirst();
                MLFQ.printLog(", executing process til the end.\n");
                //execute to the end if unfinished
                if (step < Process.PROCESS_FINISHED) {
                    while ((step = executing.step()) < Process.PROCESS_FINISHED) {
                        caller.increaseTimestamp();
                        caller.decreaseSliceTime();
                        MLFQ.printStepType(step);
                    }
                    caller.increaseTimestamp();
                    caller.decreaseSliceTime();
                    MLFQ.printStepType(step);
                }

                switch (step) {
                    case Process.PROCESS_FINISHED:
                        caller.getFinishedProcesses().put(executing.getPid(), executing);
                        break;
                    case Process.PROCESS_BLOCKED:
                        executing.changeTTL(caller.getProcessQueues()[executing.getCurrentQueue()].quantum);
                        caller.getWaitQueue().add(executing);
                        break;
                    default:
                        break;
                }
            } else {
                MLFQ.printLog(", but the process has already finished/blocked/overquoted.\n");
            }

            this.caller.resetProcessesPriority();

            if (step == Process.PROCESS_QUANTUM_OVER) {
                executing.changeCurrentQueue(0);
                executing.changeTTL(caller.getProcessQueues()[0].getQuantum());
                caller.getProcessQueues()[0].executionQueue.addLast(executing);
            }

        } else if (step == Process.PROCESS_QUANTUM_OVER) {
            executing = executionQueue.pollFirst();
            demoteProcess(executing);
        }

    }

    public boolean canStep() {
        return !executionQueue.isEmpty();
    }

    public int getQuantum() {
        return quantum;
    }

    public Deque<Process> getExecutionQueue() {
        return executionQueue;
    }

    public int getPriority() {
        return priority;
    }

    public ProcessPool getCaller() {
        return caller;
    }

}

class ProcessPool {

    private final PriorityQueue<Process> waitQueue;
    private final Map<Integer, Process> finishedProcesses;
    private final ProcessQueue processQueues[];
    private long timestamp;
    private final int queuesAmount;
    private final int sliceTime;
    private int currentSlice;

    public ProcessPool(int queues, int[] quantumList, int sliceTime) {

        if (quantumList.length < queues) {
            throw new Error("There ain't quantum numbers enought on list");
        }

        if (queues < 1) {
            throw new Error("It even exists? A 0 length queue pool?");
        }

        Random random = new Random();

        this.processQueues = new ProcessQueue[queues];
        this.finishedProcesses = new HashMap<>();
        int queueQuantum[] = new int[queues];

        for (int i = 0; i < queues; i++) {
            this.processQueues[i] = new ProcessQueue(this, quantumList[i], i);
        }

        this.waitQueue = new PriorityQueue<>((Process o1, Process o2) -> o1.getCurrentStartTime() - o2.getCurrentStartTime());
        this.timestamp = 0;
        this.queuesAmount = queues;
        this.currentSlice = this.sliceTime = sliceTime;
    }

    /**
     * Reset the priority of the waiting queue processes
     */
    public void resetProcessesPriority() {
        Process processInQueue;

        recoverWaiting();
        MLFQ.printLog("\n[THREAD] Current time : " + timestamp + " / Current Slice Time : " + currentSlice + "\n");
        MLFQ.printLog("\t[EVENT] Performing a HardReset on Process Priority\n");

        for (ProcessQueue queue : processQueues) {
            if (queue.getPriority() > 0) {
                while ((processInQueue = queue.getExecutionQueue().pollFirst()) != null) {
                    processInQueue.changeCurrentQueue(0);
                    processInQueue.changeTTL(processQueues[0].getQuantum());
                    processQueues[0].getExecutionQueue().add(processInQueue);
                }
            }
        }

        for (Process process : waitQueue) {
            process.changeCurrentQueue(0);
            process.changeTTL(processQueues[0].getQuantum());
        }

    }

    public void increaseTimestamp() {
        this.timestamp++;
    }

    public void decreaseSliceTime() {
        this.currentSlice--;
    }

    public void resetSliceTime() {
        this.currentSlice = this.sliceTime;
    }

    /**
     * Recover the processes which currentStartTime is less or equals than the
     * current timestamp
     *
     */
    public void recoverWaiting() {
        Process current;
        while (!waitQueue.isEmpty()) {
            current = waitQueue.peek();
            if (current.getCurrentStartTime() <= timestamp) {
                MLFQ.printLog("\n[THREAD] Current time : " + timestamp + " / Current Slice Time : " + currentSlice + "\n");
                MLFQ.printLog("\t[EVENT] Inserting process " + current.getPid() + " on queue " + current.getCurrentQueue() + "\n");
                processQueues[current.getCurrentQueue()].getExecutionQueue().addLast(current);
                waitQueue.remove();
            } else {
                return;
            }
        }
    }

    public void insertProcess(int pid, int startTime, int processTime, int ioProbability) {
        MLFQ.printLog("\t[EVENT] Inserting process " + pid + " on wait queue [ pid : " + pid + ", startTime : " + startTime + ", processT : " + processTime + ", ioProb : " + ioProbability + "]\n");
        waitQueue.add(new Process(pid, startTime, processTime, ioProbability, 0, processQueues[0].getQuantum()));
    }

    public void runMLFQ() {
        boolean currentExecuted, someoneExecuted, empty;

        while (true) {
            someoneExecuted = false;

            recoverWaiting();

            for (ProcessQueue currentQueue : processQueues) {

                currentExecuted = false;

                if (currentQueue.canStep()) {
                    currentExecuted = true;
                    someoneExecuted = true;
                    currentQueue.step();
                }

                if (currentExecuted) {
                    break;
                }
            }

            if (!someoneExecuted) {

                if (waitQueue.isEmpty()) {
                    return;
                }
                MLFQ.printLog("\n[THREAD] Empty queue, Current time : " + timestamp + " / Current Slice Time : " + currentSlice + "\n");
                timestamp++;
                currentSlice--;

                if (currentSlice == 0) {
                    currentSlice = sliceTime;
                    resetProcessesPriority();
                }
            }

        }

    }

    public PriorityQueue<Process> getWaitQueue() {
        return waitQueue;
    }

    public Map<Integer, Process> getFinishedProcesses() {
        return finishedProcesses;
    }

    public ProcessQueue[] getProcessQueues() {
        return processQueues;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getQueuesAmount() {
        return queuesAmount;
    }

    public int getSliceTime() {
        return sliceTime;
    }

    public int getCurrentSlice() {
        return currentSlice;
    }

}

public class MLFQ {

    @Deprecated
    public static void printLog(String toLog) {
        System.err.print(toLog);
    }

    @Deprecated
    public static void printStepType(char step) {
        switch (step) {
            case 0:
                System.err.print(" - UNFINISHED\n");
                break;
            case 1:
                System.err.print(" - FINISHED\n");
                break;
            case 2:
                System.err.print(" - QUANTUM ENDED\n");
                break;
            case 3:
                System.err.print(" - BLOCKED\n");
                break;
        }
    }

    public static void main(String[] args) {
        ProcessPool processPool = new ProcessPool(3, new int[]{2, 4, 8}, 10);
        processPool.insertProcess(0, 0, 2, 0);
        processPool.insertProcess(1, 0, 50, 0);
        processPool.insertProcess(2, 10, 2, 0);
        processPool.runMLFQ();
    }
}
