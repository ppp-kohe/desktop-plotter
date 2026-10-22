package org.autogui.plot.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntPredicate;

///reducing row-size of matrix data.
///{@link PlotterDataReducer#reduce(List, int)} is the convenient method for {@link PlotterData} to shrink to the given row size.
///
/// The reducing method is basically taking a column-data `{a,b,c,...}` and convert to a mean-value `{average(a,b,c,...)}` if the values are numbers.
///  To consider, repeatedly applying the method, it saves *count*s of summarized numbers;
///   Initially, the counts are 1 and the data can be described as `{a:1, b:1, c:1}` and after conversion, it will be `{average(a,b,c):3}`.
///   After that, the data might have other new values like `{average(a,b,c):3, d:1, e:1}`,
///    and then it can convert by `{average(average(a,b,c)*3, d*1, e*1): 3+1+1}`.
///
/// In the class, those counts are saved as a new (or existing) column named as `#RowCount`. So, after reducing,
///  the `#RowCount` column will be appended as a new column containing counts of each number of summarized rows.
/// Note the column might be unnecessary for displaying plots,
///   then you can exclude the column by {@link PlotterData#filteredColumns(IntPredicate)} with {@link #excludeRowCountColumnByHeaderName(List)}
///     as like the call `data.filteredColumns(PlotterDataReducer.excludeRowCountColumnByHeaderName(data))`.
public abstract class PlotterDataReducer {
    protected int dataPartColumnStartIndex = 1;
    protected int dataPartRowStartIndex = 1;

    public void reduce() {
        int rowSize = size();
        int rowDataSize = size() - dataPartRowStartIndex();
        int groupSize = (rowDataSize + (rowDataSize % reducedSize())) / reducedSize();

        int rowCountColumn = this.rowCountColumn();

        int currentGroupSize = 0;
        List<Integer> groupRows = new ArrayList<>(groupSize);
        for (int i = 0; i < dataPartRowStartIndex(); ++i) {
            generateRowAsIs(i);
        }
        for (int i = dataPartRowStartIndex(); i < rowSize; ++i) {
            groupRows.add(i);
            currentGroupSize++;
            if (currentGroupSize == groupSize) {
                reduceRow(groupRows, rowCountColumn);

                groupRows.clear();
                currentGroupSize = 0;
            }
        }
        if (!groupRows.isEmpty()) {
            reduceRow(groupRows, rowCountColumn);
        }
    }

    private void reduceRow(List<Integer> groupRows, int rowCountColumn) {
        int colSize = columnSize();
        long[] groupCounts = new long[groupRows.size()];
        if (rowCountColumn != -1) {
            Arrays.setAll(groupCounts, i -> {
                int row = groupRows.get(i);
                var countVal = getDataAt(row, rowCountColumn);
                if (countVal instanceof Long n) {
                    return n;
                } else {
                    return 1L;
                }
            });
        } else {
            Arrays.setAll(groupCounts, i -> 1L);
        }
        long countSum = Arrays.stream(groupCounts).sum();
        List<Object> reduced = new ArrayList<>(colSize + 1);
        int startIndex = dataPartColumnStartIndex();
        for (int col = 0; col < colSize; ++col) {
            if (col < startIndex) {
                reduced.add(getDataAt(groupRows.getFirst(), col));
            } else if (col == rowCountColumn) {
                reduced.add(countSum);
            } else {
                reduced.add(reduceColumn(groupRows, col, groupCounts, countSum));
            }
        }
        if (rowCountColumn == -1) {
            reduced.add(countSum);
        }
        reduced(groupRows, reduced);
    }

    @SuppressWarnings("unchecked")
    private Object reduceColumn(List<Integer> groupRows, int col, long[] groupCounts, long countSum) {
        double[] data = new double[groupRows.size()];
        boolean allNum = true;
        boolean allComp = true;
        List<Comparable<?>> colComparables = new ArrayList<>(groupRows.size());
        Object center = null;
        int centerIdx = groupRows.size() / 2;
        for (int i = 0, s = groupRows.size(); i < s; ++i) {
            int row = groupRows.get(i);
            var obj = getDataAt(row, col);
            if (obj instanceof Number n) {
                data[i] = n.doubleValue();
            } else if (obj instanceof Comparable<?> comp) {
                colComparables.add(comp);
                allNum= false;
            } else {
                allComp = false;
                allNum = false;
            }
            if (centerIdx == i) {
                center = obj;
            }
        }
        if (allNum) {
            double average = 0;
            for (int i = 0, l = data.length; i < l; ++i) {
                average += (data[i] / countSum * groupCounts[i]);
            }
            return average;
        } else if (allComp) {
            colComparables.sort((Comparator<Comparable<?>>) Comparator.naturalOrder());
            return colComparables.get(colComparables.size() / 2);
        } else {
            return center;
        }
    }
    public int dataPartColumnStartIndex() {
        return dataPartColumnStartIndex;
    }
    public int dataPartRowStartIndex() {
        return dataPartRowStartIndex;
    }

    public PlotterDataReducer setDataPartColumnStartIndex(int dataPartColumnStartIndex) {
        this.dataPartColumnStartIndex = dataPartColumnStartIndex;
        return this;
    }

    public PlotterDataReducer setDataPartRowStartIndex(int dataPartRowStartIndex) {
        this.dataPartRowStartIndex = dataPartRowStartIndex;
        return this;
    }

    public abstract void generateRowAsIs(int row);
    public abstract void reduced(List<Integer> groupRows, List<Object> reduced);
    public abstract Object getDataAt(int row, int col);
    public abstract int columnSize();
    public abstract int rowCountColumn();
    public abstract int reducedSize();
    public abstract int size();

    public static String rowCountColumnName = "#RowCount";

    public static IntPredicate excludeRowCountColumnByHeaderName(List<List<Object>> data) {
        int countIdx = data.getFirst().indexOf(rowCountColumnName);
        return i -> i != countIdx;
    }

    public static PlotterData reduce(List<List<Object>> data, int reducedSize) {
        return new PlotterDataReducerForList(data, reducedSize).run();
    }

    public static class PlotterDataReducerForList extends PlotterDataReducer {
        protected PlotterData output;
        protected List<List<Object>> data;
        protected int reducedSize;
        protected int columnSize;
        protected int rowCountColumn;


        public PlotterDataReducerForList(List<List<Object>> data, int reducedSize) {
            this.data = data;
            this.reducedSize = reducedSize;
            if (data instanceof PlotterData plotterData) {
                columnSize = plotterData.getColumnSize();
            } else {
                columnSize = data.stream()
                        .mapToInt(List::size)
                        .max().orElse(0);
            }
            if (!data.isEmpty()) {
                var top = data.getFirst();
                rowCountColumn = top.indexOf(rowCountColumnName);
            }
            output = PlotterData.create(reducedSize + dataPartRowStartIndex());
        }

        @Override
        public PlotterDataReducerForList setDataPartColumnStartIndex(int dataPartColumnStartIndex) {
            super.setDataPartColumnStartIndex(dataPartColumnStartIndex);
            return this;
        }

        @Override
        public PlotterDataReducerForList setDataPartRowStartIndex(int dataPartRowStartIndex) {
            super.setDataPartRowStartIndex(dataPartRowStartIndex);
            return this;
        }

        public PlotterData run() {
            reduce();
            return output;
        }

        @Override
        public int size() {
            return data.size();
        }

        @Override
        public int reducedSize() {
            return reducedSize;
        }

        @Override
        public int columnSize() {
            return columnSize;
        }

        @Override
        public int rowCountColumn() {
            return rowCountColumn;
        }

        @Override
        public Object getDataAt(int row, int col) {
            var rowList = data.get(row);
            if (col < rowList.size()) {
                return rowList.get(col);
            } else {
                return 0.0;
            }
        }

        @Override
        public void generateRowAsIs(int row) {
            var rowList = data.get(row);
            if (rowCountColumn == -1) {
                var newRowList = new ArrayList<>(rowList.size() + 1);
                newRowList.addAll(rowList);
                if (row < dataPartColumnStartIndex()) {
                    newRowList.add(rowCountColumnName);
                } else {
                    newRowList.add(1L);
                }
                output.add(newRowList);
            } else {
                output.add(rowList);
            }
        }

        @Override
        public void reduced(List<Integer> groupRows, List<Object> reduced) {
            output.add(reduced);
        }
    }

    ///a reducer implementation for {@link PlotterDataBuilder}'s internal data;
    /// it will be automatically created and called in the running process.
    public static class PlotterDataReducerForBuilder extends PlotterDataReducerForList {
        protected PlotterDataBuilder.CellAggregation cellAggregation;

        public PlotterDataReducerForBuilder(PlotterDataBuilder.CellAggregation cellAggregation, int reducedSize) {
            super(cellAggregation.build(), reducedSize);
            this.cellAggregation = cellAggregation;
        }

        @Override
        public void reduce() {
            super.reduce();
            cellAggregation.set(output);
        }
    }
}
