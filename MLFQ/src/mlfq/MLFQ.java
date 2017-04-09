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
import java.util.PriorityQueue;
import java.util.Random;

/**
 *
 * @author dmitry
 */
public class MLFQ {

    public class Process {

        public static final int PROCESS_UNFINISHED = 0;
        public static final int PROCESS_FINISHED = 1;
        public static final int PROCESS_QUANTUM_OVER = 2;
        public static final int PROCESS_BLOCKED = 3;

        public final int pid;
        public final int startTime;
        public int timeToLive;
        public int currentStartTime;
        public final int processTime;
        public final int ioProbability;
        public int currentQueue;

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
            if (this.timeToLive > 0) {

            } else {

            }

            return 0;
        }

        public void changeCurrentQueue(int queue) {
            this.currentQueue = queue;
        }

        public void changeTTL(int newTTL) {
            this.timeToLive = newTTL;
        }

    }

    public class ProcessQueue {

        public final int quantum;
        public final Deque<Process> executionQueue;
        public final int priority;
        public final ProcessPool caller;

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
            ProcessQueue queue = caller.processQueues[priority < (caller.queuesAmount - 1) ? priority + 1 : priority];
            p.changeCurrentQueue(queue.priority);
            p.changeTTL(queue.getQuantum());
            queue.executionQueue.add(p);
        }

        public int getQuantum() {
            return quantum;
        }

        /**
         * It's assumed that the queue will only step if it can step
         */
        public void step() {
            Process executing = executionQueue.peekFirst();
            boolean sliced = false;
            char step;

            step = executing.step();

            this.caller.timestamp++;
            this.caller.currentSlice--;

            if (this.caller.currentSlice == 0) {
                sliced = true;
                this.caller.currentSlice = this.caller.sliceTime;
            }


            /*
                0 - unfinished;
                1 - finished;
                2 - quantum over;
                3 - IO event
             */
            if (step == Process.PROCESS_FINISHED) {
                executing = executionQueue.pollFirst();
                caller.finishedProcesses.put(executing.pid, executing);
            } else if (step == Process.PROCESS_BLOCKED) {
                executing = executionQueue.pollFirst();
                caller.waitQueue.add(executing);
            }

            if (sliced) {

                //execute to the end
                while ((step = executing.step()) < 1) {
                    caller.timestamp++;
                }

                this.caller.resetProcessesPriority();

                if (step == Process.PROCESS_QUANTUM_OVER) {
                    executing = executionQueue.pollFirst();
                    executing.changeCurrentQueue(0);
                    executing.changeTTL(caller.processQueues[0].getQuantum());
                    caller.processQueues[0].executionQueue.addLast(executing);
                }

            } else if (step == Process.PROCESS_QUANTUM_OVER) {
                executing = executionQueue.pollFirst();
                demoteProcess(executing);
            }

        }

        public boolean canStep() {
            return !executionQueue.isEmpty();
        }

    }

    public class ProcessPool {

        public final PriorityQueue<Process> waitQueue;
        public final Map<Integer, Process> finishedProcesses;
        public final ProcessQueue processQueues[];
        public long timestamp;
        public final int queuesAmount;
        public final int sliceTime;
        public int currentSlice;

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

            this.waitQueue = new PriorityQueue<>((Process o1, Process o2) -> o1.currentStartTime - o2.currentStartTime);
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

            for (ProcessQueue queue : processQueues) {
                while ((processInQueue = queue.executionQueue.pollFirst()) != null) {
                    processInQueue.changeCurrentQueue(0);
                    processInQueue.changeTTL(processQueues[0].getQuantum());
                    processQueues[0].executionQueue.add(processInQueue);
                }
            }

            for (Process process : waitQueue) {
                process.changeCurrentQueue(0);
                process.changeTTL(processQueues[0].getQuantum());
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
                if (current.currentStartTime <= timestamp) {
                    processQueues[current.currentQueue].executionQueue.addLast(current);
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
            boolean currentExecuted, someoneExecuted;

            while (!waitQueue.isEmpty()) {
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
                    timestamp++;
                    currentSlice--;

                    if (currentSlice == 0) {
                        currentSlice = sliceTime;
                        resetProcessesPriority();
                    }
                }

            }

        }

    }
}
