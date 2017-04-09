/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mlfq;

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
        private int currentQueue;

        public Process(int pid, int startTime, int processTime, int ioProbability) {
            this.currentStartTime = this.startTime = startTime;
            this.timeToLive = this.processTime = processTime;
            this.pid = pid;
            this.ioProbability = ioProbability;
        }

        public Process(int pid, int startTime, int processTime, int ioProbability, int currentQueue) {
            this(pid, startTime, processTime, ioProbability);
            this.currentQueue = currentQueue;
        }

        public char step() {
            /*
                0 - unfinished;
                1 - finished;
                2 - quantum over;
                3 - IO event
             */
            return 0;
        }

        public void changeCurrentQueue(int queue) {
            this.currentQueue = queue;
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

        public int getCurrentQueue() {
            return currentQueue;
        }

    }

    private class ProcessQueue {

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
            queue.executionQueue.add(p);
        }

        public int getQuantum() {
            return quantum;
        }

        /**
         * It's assumed that the queue will only step if it can step
         */
        public void step() {
            Process executing = executionQueue.pollFirst();
            boolean sliced = false;
            char step = 0;

            while ((step = executing.step()) == 0) {
                this.caller.timestamp++;
                this.caller.currentSlice--;

                if (this.caller.currentSlice == 0) {
                    sliced = true;
                    this.caller.currentSlice = this.caller.sliceTime;
                }
            }

            if (sliced) {
                this.caller.resetProcessesPriority();
                caller.getProcessQueues()[0].getExecutionQueue().addLast(executing);
            } else {
                // if it's a quantum over event
                if (step == 2) {
                    demoteProcess(executing);
                    // if it's a io event
                } else if (step == 3) {
                    caller.getWaitQueue().add(executing);
                }
            }
        }

        public boolean canStep() {
            return !executionQueue.isEmpty();
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

    public class ProcessPool {

        private final PriorityQueue<Process> waitQueue;
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
            int queueQuantum[] = new int[queues];

            for (int i = 0; i < queues; i++) {
                this.processQueues[i] = new ProcessQueue(this, quantumList[i], i);
            }

            this.waitQueue = new PriorityQueue<>((Process o1, Process o2) -> o1.currentStartTime - o2.currentStartTime);
            this.timestamp = 0;
            this.queuesAmount = queues;
            this.currentSlice = this.sliceTime = sliceTime;
        }

        /**
         * Reset the priority of the waiting queue processes
         */
        public void resetProcessesPriority() {
            for (ProcessQueue queue : processQueues) {
                for (Process p : queue.getExecutionQueue()) {
                    p.changeCurrentQueue(0);
                }
            }

            for (Process shell : waitQueue) {
                shell.changeCurrentQueue(0);
            }
        }

        /**
         * Recover the processes which currentStartTime is less or equals than
         * the current timestamp
         *
         */
        public void recoverWaiting() {
            Process current;
            while (!waitQueue.isEmpty()) {
                current = waitQueue.peek();
                if (current.getCurrentStartTime() <= this.timestamp) {
                    processQueues[current.getCurrentQueue()].getExecutionQueue().addLast(current);
                    waitQueue.remove();
                } else {
                    return;
                }
            }
        }

        public void insertProcess(int pid, int startTime, int processTime, int ioProbability) {
            waitQueue.add(new Process(pid, startTime, processTime, ioProbability, 0));
        }

        public void runMLFQ() {

            while (!waitQueue.isEmpty()) {

                recoverWaiting();

                for (ProcessQueue currentQueue : processQueues) {
                    while (currentQueue.canStep()) {

                        currentQueue.step();

                    }
                }

            }

        }

        public PriorityQueue<Process> getWaitQueue() {
            return waitQueue;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public int getQueuesAmount() {
            return queuesAmount;
        }

        public ProcessQueue[] getProcessQueues() {
            return processQueues;
        }

        public int getSliceTime() {
            return sliceTime;
        }

        public int getCurrentSlice() {
            return currentSlice;
        }

    }
}
