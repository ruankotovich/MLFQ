/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mlfq;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.PriorityQueue;

/**
 *
 * @author dmitry
 */
public class MLFQ {

    public static class Process {

        private int startTime;
        private int endTime;
        private int processTime;
        private int timeToLive;
        private int spentTime;
        private int executionTime;
        private int waitTime;
        private int pid;
        private boolean done;

        public Process(int pid, int startTime, int processTime) {
            this.startTime = startTime;
            this.timeToLive = this.processTime = processTime;
            this.pid = pid;
            this.spentTime = 0;
        }

        public int step(ProcessPool pool) {
            if (this.timeToLive > pool.getQuantum()) {
                this.spentTime += pool.getQuantum();
                this.timeToLive -= pool.getQuantum();
                return pool.getQuantum();
            } else if (this.timeToLive == pool.getQuantum()) {
                this.timeToLive = 0;
                this.spentTime += pool.getQuantum();
                pool.getExecutionPool().removeFirst();
                this.endTime = pool.getTimestamp() + pool.getQuantum();
                this.executionTime = this.endTime - this.startTime;
                this.waitTime = this.executionTime - this.spentTime;
                pool.incrementSum(this.waitTime, this.executionTime);
                return pool.getQuantum();
            } else {
                int ttl = this.timeToLive;
                this.spentTime += ttl;
                this.timeToLive = 0;
                this.endTime = pool.getTimestamp() + ttl;
                this.executionTime = this.endTime - this.startTime;
                this.waitTime = this.executionTime - this.spentTime;
                pool.incrementSum(this.waitTime, this.executionTime);
                pool.getExecutionPool().removeFirst();
                return pool.getQuantum() - ttl;
            }
        }

        public boolean isDone() {
            return done;
        }

        public void setDone(boolean done) {
            this.done = done;
        }

        public int getPid() {
            return pid;
        }

        public void setPid(int pid) {
            this.pid = pid;
        }

        public int getStartTime() {
            return startTime;
        }

        public void setStartTime(int startTime) {
            this.startTime = startTime;
        }

        public int getEndTime() {
            return endTime;
        }

        public void setEndTime(int endTime) {
            this.endTime = endTime;
        }

        public int getProcessTime() {
            return processTime;
        }

        public void setProcessTime(int processTime) {
            this.processTime = processTime;
        }

        public int getTimeToLive() {
            return timeToLive;
        }

        public void setTimeToLive(int timeToLive) {
            this.timeToLive = timeToLive;
        }

    }

    public static class ProcessPool {

        private final int quantum;
        private int waitSum, processSum;
        private final PriorityQueue<Process> pool;
        private final Deque<Process> executionPool;
        private int timestamp;

        public void incrementSum(int wait, int proccess) {
            this.waitSum += wait;
            this.processSum += proccess;
        }

        public int getWaitSum() {
            return waitSum;
        }

        public int getProcessSum() {
            return processSum;
        }

        public int getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(int timestamp) {
            this.timestamp = timestamp;
        }

        public int getQuantum() {
            return quantum;
        }

        public PriorityQueue<Process> getPool() {
            return pool;
        }

        public Deque<Process> getExecutionPool() {
            return executionPool;
        }

        private ProcessPool(int quantum) {
            this.quantum = quantum;
            this.pool = new PriorityQueue<>((Process o1, Process o2) -> o1.startTime - o2.startTime);
            this.executionPool = new ArrayDeque<>();
            this.waitSum = this.processSum = 0;
        }

        public void notifyPool() {
            while (!pool.isEmpty()) {
                if (pool.peek().getStartTime() <= this.timestamp) {
                    executionPool.addLast(pool.poll());
                } else {
                    break;
                }
            }
        }

        public boolean canStep() {
            return !(pool.isEmpty() && executionPool.isEmpty());
        }

        public void insertProcess(int pid, int startTime, int processTime) {
            pool.add(new Process(pid, startTime, processTime));
        }

        public String step() {
            String process = null;
            notifyPool();
            Process current = executionPool.peekFirst();
            if (current != null) {
                process = "P" + current.pid;

                int taken = current.step(this);
                timestamp += taken;
                notifyPool();

                if (current.getTimeToLive() > 0) {
                    executionPool.addLast(executionPool.pollFirst());
                }
            } else {
                timestamp++;
            }
            return process;
        }
    }
}
