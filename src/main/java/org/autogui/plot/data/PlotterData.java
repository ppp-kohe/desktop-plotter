package org.autogui.plot.data;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

///row and column matrix data utility;
///* an implementation class of {@link List}`<List<Object>>`
///* dynamically expands the size (rows x columns);
///   {@link #add(List)}, {@link #add(int, List)}, {@link #setCell(int, int, Object)} ,
///   {@link #addCell(int, int, Object)}, {@link #fillCell(int, int, Object)}, and so on.
/// Also, {@link #getRow(int)} expands rows.
///* if the specified index is over rows and columns sizes, returns 0.0 ({@link #getZero()});
///* {@link PlotterDataColumn} can manipulate column data at once.
public class PlotterData extends AbstractList<List<Object>> {
    protected ArrayList<List<Object>> data;
    protected int columnSize;

    public static PlotterData create() {
        return new PlotterData();
    }

    public static PlotterData create(int capacity) {
        return new PlotterData(capacity);
    }

    public PlotterData() {
        data = new ArrayList<>();
    }

    public PlotterData(int capacity) {
        data = new ArrayList<>(capacity);
    }

    protected PlotterData(ArrayList<List<Object>> data) {
        this.data = data;
    }

    @Override
    public List<Object> get(int index) {
        return data.get(index);
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean add(List<Object> objects) {
        updateColumnSize(objects);
        return data.add(objects);
    }

    @Override
    public void add(int index, List<Object> element) {
        updateColumnSize(element);
        data.add(index, element);
    }

    @Override
    public List<Object> set(int index, List<Object> element) {
        var v = data.set(index, element);
        updateColumnSize(element);
        return v;
    }

    protected void updateColumnSize(List<Object> es) {
        if (es != null) {
            columnSize = Math.max(columnSize, es.size());
        }
    }

    public int getColumnSize() {
        return columnSize;
    }

    public int getRowSize() {
        return size();
    }

    public Object getZero() {
        return 0.0;
    }

    public List<Object> getRow(int row) {
        int cols = getColumnSize();
        for (int r = getRowSize(); r <= row; ++r) {
            data.add(new ArrayList<>(cols));
        }
        return get(row);
    }

    public Object getCell(int row, int col) {
        if (row >= data.size()) {
            return getZero();
        } else {
            List<Object> rowList = get(row);
            if (col >= rowList.size()) {
                return getZero();
            } else {
                return rowList.get(col);
            }
        }
    }

    public <E> PlotterData setCell(int row, int col, E e) {
        List<Object> rowList = getRow(row);
        for (int c = rowList.size(); c <= col; ++c) {
            rowList.add(getZero());
        }
        rowList.set(col, e);
        updateColumnSize(rowList);
        return this;
    }

    public <E> PlotterData addCell(int row, int col, E e) {
        List<Object> rowList = getRow(row);
        for (int c = rowList.size(); c < col; ++c) {
            rowList.add(col, getZero());
        }
        rowList.add(col, e);
        updateColumnSize(rowList);
        return this;
    }

    public <E> PlotterData fillCell(int rowSize, int colSize, E e) {
        for (int r = 0; r < rowSize; ++r) {
            for (int c = 0; c < colSize; ++c) {
                setCell(r, c, e);
            }
        }
        return this;
    }

    public PlotterDataColumn getColumn(int col) {
        return new PlotterDataColumn(this, col);
    }

    public PlotterDataColumn getColumnFirst() {
        return getColumn(0);
    }

    public PlotterDataColumn getColumnLast() {
        return getColumn(Math.max(0, getColumnSize() - 1));
    }

    public PlotterDataColumn addColumn() {
        int i = getColumnSize();
        var col = new PlotterDataColumn(this, i);
        columnSize = i + 1; //considering no rows case
        return col;
    }

    public PlotterDataColumn addColumnFirst() {
        int s = getColumnSize();
        for (int r = 0, rs = getRowSize(); r < rs; ++r) {
            addCell(r, 0, getZero());
        }
        PlotterDataColumn col = new PlotterDataColumn(this, 0);
        columnSize = s + 1; //considering no rows case
        return col;
    }

    public <E> PlotterData addRow(List<E> rows) {
        add(new ArrayList<>(rows));
        return this;
    }

    public <E> PlotterData addRowFirst(List<E> rows) {
        add(0, new ArrayList<>(rows));
        return this;
    }

    public PlotterData addRow() {
        add(new ArrayList<>(getColumnSize()));
        return this;
    }

    public PlotterData addRowFirst() {
        add(0, new ArrayList<>(getColumnSize()));
        return this;
    }

    public static class PlotterDataColumn {
        protected PlotterData owner;
        protected int columnIndex;

        public PlotterDataColumn(PlotterData owner, int columnIndex) {
            this.owner = owner;
            this.columnIndex = columnIndex;
        }

        public PlotterDataColumn shiftColumnIndex() {
            this.columnIndex++;
            return this;
        }

        public PlotterData end() {
            return owner;
        }

        public int getColumnIndex() {
            return columnIndex;
        }

        public PlotterDataColumn moveColumnNext() {
            return owner.getColumn(columnIndex + 1);
        }

        public PlotterDataColumn moveColumnPrevious() {
            return owner.getColumn(columnIndex - 1);
        }

        public <E> PlotterDataColumn set(int rowIndex, E rowValue) {
            owner.setCell(rowIndex, columnIndex, rowValue);
            return this;
        }

        public <E> PlotterDataColumn set(int rowIndex, Function<Integer,E> rowToValue) {
            return set(rowIndex, rowToValue.apply(rowIndex));
        }

        public <E> PlotterDataColumn set(int rowIndex, BiFunction<Integer,Integer,E> rowColumnToValue) {
            return set(rowIndex, rowColumnToValue.apply(rowIndex, columnIndex));
        }

        public <E> PlotterDataColumn setAll(List<E> rowValues) {
            IntStream.range(0, rowValues.size())
                    .forEach(i -> {
                        var e = rowValues.get(i);
                        if (e != null) {
                            set(i, e);
                        }
                    });
            return this;
        }

        @SafeVarargs
        public final <E> PlotterDataColumn setAll(E... rowValues) {
            return setAll(Arrays.asList(rowValues));
        }

        public <E> PlotterDataColumn setAll(Function<Integer,E> rowToValue) {
            return setAll((row, col) -> rowToValue.apply(row));
        }

        public <E> PlotterDataColumn setAll(BiFunction<Integer,Integer,E> rowColumnToValue) {
            IntStream.range(0, owner.getRowSize())
                    .forEach(i -> {
                        var e = rowColumnToValue.apply(i, columnIndex);
                        if (e != null) {
                            set(i, e);
                        }
                    });
            return this;
        }

        public <E> PlotterDataColumn updateAll(IndexAndValue<E> indexAndValue) {
            return updateRowsRange(0, 0, indexAndValue);
        }

        public <E> PlotterDataColumn updateRowsFrom(int rowIndexStart, IndexAndValue<E> indexAndValue) {
            return updateRowsRange(rowIndexStart, 0, indexAndValue);
        }

        public <E> PlotterDataColumn updateRowsTo(int rowIndexEndExclusive, IndexAndValue<E> indexAndValue) {
            return updateRowsRange(0, rowIndexEndExclusive, indexAndValue);
        }

        @SuppressWarnings("unchecked")
        public <E> PlotterDataColumn updateRowsRange(int rowIndexStart, int rowIndexEndExclusive, IndexAndValue<E> indexAndValue) {
            IntStream.range(rowIndexStart, endRowIndex(rowIndexEndExclusive))
                    .forEach(i -> {
                        var e = indexAndValue.apply(i, columnIndex, (E) owner.getCell(i, columnIndex));
                        if (e != null) {
                            set(i, e);
                        }
                    });
            return this;
        }

        private int endRowIndex(int rowIndexEndExclusive) {
            if (rowIndexEndExclusive <= 0) {
                return owner.getRowSize() + rowIndexEndExclusive;
            } else {
                return rowIndexEndExclusive;
            }
        }

        public <E> Stream<E> streamAll() {
            return streamRowRange(0, 0);
        }

        public <E> Stream<E> streamRowFrom(int rowIndexStart) {
            return streamRowRange(rowIndexStart, 0);
        }

        public <E> Stream<E> streamRowsTo(int rowIndexEndExclusive) {
            return streamRowRange(0, rowIndexEndExclusive);
        }

        @SuppressWarnings("unchecked")
        public <E> E get(int rowIndex) {
            return (E) owner.getCell(rowIndex, columnIndex);
        }

        @SuppressWarnings("unchecked")
        public <E> Stream<E> streamRowRange(int fromRowIndex, int toRowIndexExclusive) {
            return IntStream.range(fromRowIndex, endRowIndex(toRowIndexExclusive))
                    .mapToObj(r -> (E) owner.getCell(r, columnIndex));
        }

        @Override
        public String toString() {
            return "column(" + columnIndex + ")";
        }
    }

    public interface IndexAndValue<E> {
        E apply(int rowIndex, int columnIndex, E e);
    }

    @Override
    public String toString() {
        return "[" + IntStream.range(0, getRowSize())
                .mapToObj(this::getRow)
                .map(Object::toString)
                .collect(Collectors.joining(",\n")) + "]";
    }

    public List<List<Object>> filteredColumns(IntPredicate columnFilter) {
        return new PlotterDataFiltered(data, IntStream.range(0, getColumnSize())
                .filter(columnFilter)
                .toArray());
    }

    public static class PlotterDataFiltered extends PlotterData {
        protected int[] columnIndices;

        public PlotterDataFiltered(ArrayList<List<Object>> data, int[] columnIndices) {
            super(data);
            this.columnIndices = columnIndices;
            this.columnSize = columnIndices.length;
        }

        @Override
        public List<Object> get(int index) {
            return new PlotterLists.FilteredList<>(data.get(index), columnIndices);
        }

        @Override
        public List<Object> getRow(int row) {
            if (row < size()) {
                return get(row);
            } else {
                return List.of();
            }
        }

        @Override
        public boolean add(List<Object> objects) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(int index, List<Object> element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Object> set(int index, List<Object> element) {
            throw new UnsupportedOperationException();
        }
    }
}
