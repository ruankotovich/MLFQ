/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mlfq;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

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
    private int executionTime;
    private int waitTime;
    private int spentTime;
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
        this.spentTime = 0;
        this.ioProbability = ioProbability;
        rand = new Random();
    }

    public Process(int pid, int startTime, int processTime, int ioProbability, int currentQueue, int currentQuantum) {
        this(pid, startTime, processTime, ioProbability);
        this.currentQueue = currentQueue;
        this.timeToLive = currentQuantum;
    }

    public char step(ProcessPool pool) {
        int ioOper;
        MLFQ.printLog("\t[EVENT] Running process " + getPid() + " on queue " + getCurrentQueue() + "[ ttl : " + this.timeToLive + " , lifetime : " + this.lifeTime + ", processT : " + this.processTime + "]");
        this.lifeTime++;
        this.timeToLive--;
        this.increaseSpentTime(1);
        MLFQ.printLog(", now [ ttl : " + this.timeToLive + " , lifetime : " + this.lifeTime + ", processT : " + this.processTime + "]");
        if (this.lifeTime < this.processTime) {

            if (this.timeToLive > 0) {
                ioOper = 1 + rand.nextInt(101);

                if (this.ioProbability >= ioOper) {
                    this.currentStartTime = (int) (pool.getTimestamp() + (1 + rand.nextInt(6)));
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

    public void increaseSpentTime(int val) {
        this.spentTime += val;
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

    public int getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(int executionTime) {
        this.executionTime = executionTime;
    }

    public int getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }

    public int getSpentTime() {
        return spentTime;
    }

    public void setSpentTime(int spentTime) {
        this.spentTime = spentTime;
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

        step = executing.step(caller);
        caller.getProcessOrder().append("P").append(executing.getPid()).append(" ");
        MLFQ.printStepType(step);
        this.caller.increaseTimestamp();
        this.caller.decreaseSliceTime();

        if (this.caller.getCurrentSlice() <= 0) {
            sliced = true;
            MLFQ.printLog("\n[THREAD] Current time : " + caller.getTimestamp() + " / Current Slice Time : " + caller.getCurrentSlice() + "\n");
            MLFQ.printLog("\t[EVENT] A Refresh occurs");
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
            executing.setExecutionTime((int) (caller.getTimestamp() - executing.getStartTime()));
            executing.setWaitTime(executing.getExecutionTime() - executing.getSpentTime());
            caller.getFinishedProcesses().put(executing.getPid(), executing);
        } else if (step == Process.PROCESS_BLOCKED) {
            executing = executionQueue.pollFirst();
            executing.changeTTL(caller.getProcessQueues()[executing.getCurrentQueue()].getQuantum());
            caller.insertProcess(executing);
        }

        if (sliced) {
            if (step == Process.PROCESS_UNFINISHED) {
                executing = executionQueue.pollFirst();
                MLFQ.printLog(", executing process til the end.\n");
                //execute to the end if unfinished
                if (step < Process.PROCESS_FINISHED) {
                    while ((step = executing.step(caller)) < Process.PROCESS_FINISHED) {
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
    private final StringBuffer processOrder;
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
        this.processOrder = new StringBuffer();
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

    public void insertProcess(Process p) {
        MLFQ.printLog("\t[EVENT] Inserting process " + p.getPid() + " on wait queue [ pid : " + p.getPid() + ", startTime : " + p.getCurrentStartTime() + ", processT : " + p.getProcessTime() + ", ioProb : " + p.getIoProbability() + "]\n");
        waitQueue.add(p);
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

    public StringBuffer getProcessOrder() {
        return processOrder;
    }

}

public class MLFQ {

    @Deprecated
    public static void printLog(String toLog) {
        System.out.print(toLog);
    }

    @Deprecated
    public static void printStepType(char step) {
        switch (step) {
            case 0:
                System.out.print(" - UNFINISHED\n");
                break;
            case 1:
                System.out.print(" - FINISHED\n");
                break;
            case 2:
                System.out.print(" - QUANTUM ENDED\n");
                break;
            case 3:
                System.out.print(" - BLOCKED\n");
                break;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int processCount;
        int time, execution, iobound;

        processCount = scanner.nextInt();

        while (processCount > 0) {

            ProcessPool processPool = new ProcessPool(1, new int[]{2}, 200);

            for (int i = 1; i <= processCount; i++) {
                time = scanner.nextInt();
                execution = scanner.nextInt();
                iobound = scanner.nextInt();
                processPool.insertProcess(i, time, execution, iobound);
            }

            processPool.runMLFQ();
            double waitSum = 0, executionSum = 0;

            MLFQ.printLog("\n\n -- ALL PROCESS HAVE BEEN EXECUTED -- \n\n");

            Set<Entry<Integer, Process>> keys = processPool.getFinishedProcesses().entrySet();

            for (Entry<Integer, Process> key : keys) {
                waitSum += key.getValue().getWaitTime();
                executionSum += key.getValue().getExecutionTime();
            }
            System.out.println("Tempo médio de execução: " + executionSum / processPool.getFinishedProcesses().size());
            System.out.println("Tempo médio de espera: " + waitSum / processPool.getFinishedProcesses().size());
            System.out.println(processPool.getProcessOrder());
            processCount = scanner.nextInt();
        }
    }
}
