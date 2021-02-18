package fedoresko.home.strutures;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.Consumer;

public class MutablePriorityQueueTest {

    @Test
    public void test() throws Exception {

        MutablePriorityQueue<WeightedLabelWithListener> queue = new MutablePriorityQueue<>(
                value -> value::setListener);

        WeightedLabelWithListener first = new WeightedLabelWithListener("First", 1);
        WeightedLabelWithListener second = new WeightedLabelWithListener("Second", 2);
        WeightedLabelWithListener third = new WeightedLabelWithListener("Third", 3);
        WeightedLabelWithListener fourth = new WeightedLabelWithListener("Fourth", 4);
        WeightedLabelWithListener f5 = new WeightedLabelWithListener("F5", 5);
        WeightedLabelWithListener f6 = new WeightedLabelWithListener("F6", 6);

        queue.add(fourth);
        queue.add(first);
        queue.add(second);
        queue.add(f5);
        queue.add(third);
        queue.add(f6);

        printQueue(queue);

        System.out.println("Mutating");
        first.setWeight(5);
        second.setWeight(10);
        printQueue(queue);

        queue.add(new WeightedLabelWithListener("node", 13));
        queue.add(new WeightedLabelWithListener("node", 11));
        queue.add(new WeightedLabelWithListener("node", 7));
        queue.add(new WeightedLabelWithListener("node", 9));


        printQueue(queue);

        pollQueue(queue);

    }

    private <E> void pollQueue(MutablePriorityQueue<E> queue) {
        System.out.println("Polling");
        while (!queue.isEmpty()) {
            System.out.println(queue.poll());
        }
    }

    private <E> void printQueue(MutablePriorityQueue<E> queue) {
        for (E el : queue) {
            System.out.println(el);
        }
        System.out.println(queue.depth()); //should be log N ))
        System.out.println();
    }

    @Test
    public void randomAddRem() {
        MutablePriorityQueue<WeightedLabelWithListener> queue = new MutablePriorityQueue<>(
                value -> value::setListener);

        ArrayList<WeightedLabelWithListener> list = new ArrayList<>();

        for (int j = 0; j < 10; j++) {
            int r = (int)(100*Math.random());
            WeightedLabelWithListener label = new WeightedLabelWithListener("Label " + r, r);
            list.add(label);
            queue.add(label);
        }

        printQueue(queue);

        for (int i = 0; i < 10; i++) {
            System.out.println();
            for (int j = 0; j < 5; j++) {
                queue.remove(list.remove((int)(list.size()*Math.random())));
            }
            for (int j = 0; j < 5; j++) {
                int r = (int)(100*Math.random());
                WeightedLabelWithListener label = new WeightedLabelWithListener("Label " + r, r);
                list.add(label);
                queue.add(label);
            }

            printQueue(queue);
        }

        pollQueue(queue);
    }


    @Test
    public void randomMutate() {
        Map<Integer,  Consumer<Integer> > listeners = new HashMap<>();

        MutablePriorityQueue<Integer> queue = new MutablePriorityQueue<>(
                value -> listener -> listeners.put(value.intValue(), listener));

        ArrayList<Integer> list = new ArrayList<>();

        for (int j = 0; j < 10; j++) {
            list.add(j);
            queue.add(j); //use initial numbers as keys for listener map
        }

        printQueue(queue);

        for (int i = 0; i < 10; i++) {
            System.out.println();
            for (int j = 0; j < 5; j++) {
                Integer elem = list.get((int) (list.size() * Math.random()));
                listeners.get(elem).accept((int) (100 * Math.random())); // replace old value with new one
            }
            printQueue(queue);
        }

        pollQueue(queue);
    }

    @Test
    public void comparison() {
        final int N_ELEMENTS = 1000000;

        System.out.println("Measuring queue without mutations...");

        long start = System.nanoTime();
        PriorityQueue<Integer> reference = new PriorityQueue<>();
        for (int i = 0 ; i < N_ELEMENTS/2; i++) {
            reference.add((int) (Integer.MAX_VALUE * Math.random()));
        }
        for (int i = 0 ; i < N_ELEMENTS/2; i++) {
            reference.poll();
            reference.add((int) (Integer.MAX_VALUE * Math.random()));
        }
        while (!reference.isEmpty()) {
            reference.poll();
        }
        double refTime = System.nanoTime() - start;
        System.out.println("Reference:     "+ refTime/1000000 +"ms");

        start = System.nanoTime();
        MutablePriorityQueue<Integer> queue = new MutablePriorityQueue<>();
        for (int i = 0 ; i < N_ELEMENTS/2; i++) {
            queue.add((int) (Integer.MAX_VALUE * Math.random()));
        }
        for (int i = 0 ; i < N_ELEMENTS/2; i++) {
            queue.poll();
            queue.add((int) (Integer.MAX_VALUE * Math.random()));
        }
        while (!queue.isEmpty()) {
            queue.poll();
        }
        double mTime = System.nanoTime() - start;
        System.out.println("Mutable queue: "+ mTime/1000000 +"ms");
        System.out.println("Rate: "+(mTime/refTime));


        System.out.println("Measuring queue with mutations...");
        ArrayList<WeightedLabelWithListener> elements = new ArrayList<>();
        for (int i = 0; i < N_ELEMENTS; i++) {
            int val = (int) (Integer.MAX_VALUE * Math.random());
            elements.add(new WeightedLabelWithListener("L"+val, val) );
        }

        start = System.nanoTime();
        PriorityQueue<WeightedLabelWithListener> reference1 = new PriorityQueue<>();
        for (int i = 0 ; i < N_ELEMENTS/2; i++) {
            reference1.add(elements.get(i));
        }
        for (int i = 0 ; i < N_ELEMENTS/2; i++) {
            reference1.remove(elements.get(i));
            reference1.add(elements.get(i + N_ELEMENTS/2));
        }
        while (!reference1.isEmpty()) {
            reference1.poll();
        }
        refTime = System.nanoTime() - start;
        System.out.println("Reference:     "+ refTime/1000000 +"ms");

        start = System.nanoTime();
        MutablePriorityQueue<WeightedLabelWithListener> queue1 = new MutablePriorityQueue<>(value -> value::setListener);
        for (int i = 0 ; i < N_ELEMENTS/2; i++) {
            queue1.add(elements.get(i));
        }
        for (int i = 0 ; i < N_ELEMENTS/2; i++) {
            elements.get(i).setWeight((int) (Integer.MAX_VALUE * Math.random()));
        }
        while (!queue1.isEmpty()) {
            queue1.poll();
        }
        mTime = System.nanoTime() - start;
        System.out.println("Mutable queue: "+ mTime/1000000 +"ms");
        System.out.println("Rate: "+(mTime/refTime));

    }

    private static class WeightedLabel implements Comparable<WeightedLabel> {
        final String label;
        int weight;

        WeightedLabel(String label, int weight) {
            this.weight = weight;
            this.label = label;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }

        @Override
        public int compareTo(WeightedLabel o) {
            return Integer.compare(weight, o.weight);
        }

        @Override
        public String toString() {
            return weight + " " + label;
        }
    }

    private static class WeightedLabelWithListener extends WeightedLabel {
        private Consumer<WeightedLabelWithListener> listener;

        WeightedLabelWithListener(String label, int weight) {
            super(label, weight);
        }

        @Override
        public void setWeight(int weight) {
            super.setWeight(weight);
            if (listener != null) {
                listener.accept(this);
            }
        }

        public void setListener(Consumer<WeightedLabelWithListener> listener) {
            this.listener = listener;
        }
    }
}
