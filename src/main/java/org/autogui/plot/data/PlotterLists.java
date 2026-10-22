package org.autogui.plot.data;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

public class PlotterLists {

    public static class HeaderRowList<F,T> extends AbstractList<T> {
        protected List<List<F>> data;
        protected BiFunction<List<List<F>> , Integer, T> gen;

        public HeaderRowList(List<List<F>> data, BiFunction<List<List<F>>, Integer, T> gen) {
            this.data = data;
            this.gen = gen;
        }

        @Override
        public int size() {
            return data.size() > 0 ? data.get(0).size() : 0;
        }

        @Override
        public T get(int index) {
            return gen.apply(data, index);
        }
    }

    public static class HeaderColumnList<F,T> extends AbstractList<T> {
        protected List<List<F>> data;
        protected BiFunction<List<F>, Integer, T> gen;

        public HeaderColumnList(List<List<F>> data, BiFunction<List<F>, Integer, T> gen) {
            this.data = data;
            this.gen = gen;
        }

        @Override
        public int size() {
            return data.size();
        }

        @Override
        public T get(int index) {
            return gen.apply(data.get(index), index);
        }
    }

    public static class FloatHeaderRowList<H> extends AbstractList<Object> {
        protected float[][] data;
        protected BiFunction<float[][], Integer, H> gen;

        public FloatHeaderRowList(float[][] data, BiFunction<float[][], Integer, H> gen) {
            this.data = data;
            this.gen = gen;
        }

        @Override
        public int size() {
            return data.length > 0 ? data[0].length : 0;
        }

        @Override
        public Object get(int index) {
            return gen.apply(data, index);
        }
    }

    public static class FloatHeaderColumnList<H> extends AbstractList<Object> {
        protected float[][] data;
        protected BiFunction<float[], Integer, H> gen;

        public FloatHeaderColumnList(float[][] data, BiFunction<float[], Integer, H> gen) {
            this.data = data;
            this.gen = gen;
        }

        @Override
        public int size() {
            return data.length;
        }

        @Override
        public Object get(int index) {
            return gen.apply(data[index], index);
        }
    }

    public static class FloatMatrixList<H> extends AbstractList<List<Object>> {
        protected List<H> headerRow;
        protected List<H> headerColumn;
        protected float[][] data;

        public FloatMatrixList(float[][] data) {
            this.data = data;
        }

        @SuppressWarnings("unchecked")
        public FloatMatrixList(float[][] data, List<H> headerRow, List<H> headerColumn) {
            this.headerRow = headerRow;
            this.headerColumn = headerColumn;
            this.data = data;
            if (headerRow != null && headerColumn != null) {
                int rowSize = data.length;
                //it needs not to modify headerColumn
                if (rowSize > 0) {
                    int colSize = data[0].length;
                    if (colSize == headerRow.size()) { //no top value
                        this.headerRow = new ArrayList<>(headerRow);
                        this.headerRow.add(0, (H) "");
                    }
                }
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<Object> get(int index) {
            if (headerRow == null) {
                return getTableRow(index);
            } else if (index == 0) {
                return List.class.cast(headerRow);
            } else {
                return getTableRow(index - 1);
            }
        }

        private List<Object> getTableRow(int index) {
            Object h = null;
            if (headerColumn != null) {
                if (index < headerColumn.size()) {
                    h = headerColumn.get(index);
                } else {
                    h = "";
                }
            }
            return new FloatVectorList(data[index], h);
        }

        @Override
        public int size() {
            return (headerRow == null ? 0 : 1) + data.length;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof FloatMatrixList mat && mat.data == data) {
                return true;
            }
            return super.equals(o);
        }
    }

    public static class FloatVectorList extends AbstractList<Object> {
        protected Object header;
        protected float[] data;

        public FloatVectorList(float[] data) {
            this.data = data;
        }

        public FloatVectorList(float[] data, Object header) {
            this.data = data;
            this.header = header;
        }

        @Override
        public Object get(int index) {
            if (header == null) {
                return data[index];
            } else if (index == 0) {
                return header;
            } else {
                return data[index - 1];
            }
        }

        @Override
        public int size() {
            return (header == null ? 0 : 1) + data.length;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof FloatVectorList && ((FloatVectorList)o).data == data) {
                return true;
            }
            return super.equals(o);
        }
    }

    public static class DoubleHeaderRowList extends AbstractList<Object> {
        protected double[][] data;
        protected BiFunction<double[][], Integer, Object> gen;

        public DoubleHeaderRowList(double[][] data, BiFunction<double[][], Integer, Object> gen) {
            this.data = data;
            this.gen = gen;
        }

        @Override
        public int size() {
            return data.length > 0 ? data[0].length : 0;
        }

        @Override
        public Object get(int index) {
            return gen.apply(data, index);
        }
    }

    public static class DoubleHeaderColumnList extends AbstractList<Object> {
        protected double[][] data;
        protected BiFunction<double[], Integer, Object> gen;

        public DoubleHeaderColumnList(double[][] data, BiFunction<double[], Integer, Object> gen) {
            this.data = data;
            this.gen = gen;
        }

        @Override
        public int size() {
            return data.length;
        }

        @Override
        public Object get(int index) {
            return gen.apply(data[index], index);
        }
    }

    public static class DoubleMatrixList<T> extends AbstractList<List<Object>> {
        protected List<T> headerRow;
        protected List<T> headerColumn;
        protected double[][] data;

        public DoubleMatrixList(double[][] data) {
            this.data = data;
        }

        @SuppressWarnings("unchecked")
        public DoubleMatrixList(double[][] data, List<T> headerRow, List<T> headerColumn) {
            this.headerRow = headerRow;
            this.headerColumn = headerColumn;
            this.data = data;
            if (headerRow != null && headerColumn != null) {
                this.headerRow = new ArrayList<>(headerRow);
                this.headerRow.add(0, (T) "");
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<Object> get(int index) {
            if (headerRow == null) {
                return getTableRow(index);
            } else if (index == 0) {
                return List.class.cast(headerRow);
            } else {
                return getTableRow(index - 1);
            }
        }

        private List<Object> getTableRow(int index) {
            Object h = null;
            if (headerColumn != null) {
                if (index < headerColumn.size()) {
                    h = headerColumn.get(index);
                } else {
                    h = "";
                }
            }
            return new DoubleVectorList(data[index], h);
        }

        @Override
        public int size() {
            return (headerRow == null ? 0 : 1) + data.length;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof DoubleMatrixList mat && mat.data == data) {
                return true;
            }
            return super.equals(o);
        }
    }

    public static class DoubleVectorList extends AbstractList<Object> {
        protected Object header;
        protected double[] data;

        public DoubleVectorList(double[] data) {
            this.data = data;
        }

        public DoubleVectorList(double[] data, Object header) {
            this.data = data;
            this.header = header;
        }

        @Override
        public Object get(int index) {
            if (header == null) {
                return data[index];
            } else if (index == 0) {
                return header;
            } else {
                return data[index - 1];
            }
        }

        @Override
        public int size() {
            return (header == null ? 0 : 1) + data.length;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof DoubleVectorList && ((DoubleVectorList)o).data == data) {
                return true;
            }
            return super.equals(o);
        }
    }

    public static <E> FilteredList<E> filteredExclude(List<E> list, int... excluded) {
        Arrays.sort(excluded);
        return new FilteredList<>(list, IntStream.range(0, list.size())
                .filter(index -> Arrays.binarySearch(excluded, index) < 0)
                .toArray());
    }

    public static <E> FilteredList<E> filteredInclude(List<E> list, int... included) {
        Arrays.sort(included);
        if (included.length > list.size()) {
            included = Arrays.copyOf(included, list.size());
        }
        for (int i = 1, l = included.length; i < l; ++i) {
            if (included[i - 1] == included[i]) { //has duplication
                included = Arrays.stream(included)
                        .distinct()
                        .toArray();
                break;
            }
        }
        return new FilteredList<>(list, included);
    }

    public static <E> FilteredList<E> filteredInclude(List<E> list, IntPredicate included) {
        return new FilteredList<>(list, IntStream.range(0, list.size())
                .filter(included)
                .toArray());
    }


    public static class FilteredList<E> extends AbstractList<E> {
        protected List<E> data;
        protected int[] included;

        public FilteredList(List<E> data, int[] included) {
            this.data = data;
            this.included = included; //no duplication, ordered, index.length <= data.size()
        }

        @Override
        public int size() {
            return included.length;
        }

        @Override
        public E get(int index) {
            return data.get(included[index]);
        }
    }
}
