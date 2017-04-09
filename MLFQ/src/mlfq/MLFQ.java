/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mlfq;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;

/**
 *
 * @author dmitry
 */
public class MLFQ {

    public class Process {

        private final int pid;
        private final int startTime;
        private int timeToLive;
        private int currentStartTime;
        private final int processTime;
        private final int ioProbability;

        public Process(int pid, int startTime, int processTime, int ioProbability) {
            this.currentStartTime = this.startTime = startTime;
            this.timeToLive = this.processTime = processTime;
            this.pid = pid;
            this.ioProbability = ioProbability;
        }

        public int getPid() {
            return pid;
        }

        public int getStartTime() {
            return startTime;
        }

        public int getProcessTime() {
            return processTime;
        }

        public int getTimeToLive() {
            return timeToLive;
        }

        public int getCurrentStartTime() {
            return currentStartTime;
        }

        public int getIoProbability() {
            return ioProbability;
        }

    }

    public class ProcessQueue {

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
         */
        public void demoteProcess(Process p) {

        }

        public int getQuantum() {
            return quantum;
        }

        public void step() {
        }

        public boolean canStep() {
            return !executionQueue.isEmpty();
        }

        public Deque<Process> getExecutionQueue() {
            return executionQueue;
        }

    }

    public class WaitingShell {

        private final Process process;
        private int queue;

        public WaitingShell(Process process, int queue) {
            this.process = process;
            this.queue = queue;
        }

        public void resetQueue() {
            this.queue = 0;
        }

        public Process getProcess() {
            return process;
        }

        public int getQueue() {
            return queue;
        }

    }

    public class ProcessPool {

        private final PriorityQueue<WaitingShell> waitQueue;
        private final ProcessQueue processQueue[];
        private long timestamp;
        private final int queuesAmount;

        public ProcessPool(int queues, int[] quantumList) {

            if (quantumList.length < queues) {
                throw new Error("There ain't quantum numbers enought on list");
            }

            if (queues < 1) {
                throw new Error("It even exists? A 0 length queue pool?");
            }

            Random random = new Random();

            this.processQueue = new ProcessQueue[queues];
            int queueQuantum[] = new int[queues];

            for (int i = 0; i < queues; i++) {
                this.processQueue[i] = new ProcessQueue(this, quantumList[i], i);
            }

            this.waitQueue = new PriorityQueue<>((WaitingShell o1, WaitingShell o2) -> o1.getProcess().currentStartTime - o2.getProcess().currentStartTime);
            this.timestamp = 0;
            this.queuesAmount = queues;
        }

        /**
         * Reset the priority of the waiting queue processes
         */
        public void resetProcessesPriority() {
            for (WaitingShell shell : waitQueue) {
                shell.resetQueue();
            }
        }

        /**
         * Recover the processes which currentStartTime is less or equals than
         * the current timestamp
         *
         */
        public void recoverWaiting() {
            WaitingShell current;
            while (!waitQueue.isEmpty()) {
                current = waitQueue.peek();
                if (current.getProcess().getCurrentStartTime() <= this.timestamp) {
                    processQueue[current.getQueue()].getExecutionQueue().addLast(current.getProcess());
                    waitQueue.remove();
                } else {
                    return;
                }
            }
        }

        public void insertProcess(int pid, int startTime, int processTime, int ioProbability) {
            waitQueue.add(new WaitingShell(new Process(pid, startTime, processTime, ioProbability), 0));
        }

        public void runMLFQ() {

        }

        public ProcessQueue[] getProcessQueue() {
            return processQueue;
        }

        public PriorityQueue<WaitingShell> getWaitQueue() {
            return waitQueue;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public int getQueuesAmount() {
            return queuesAmount;
        }

    }
}
