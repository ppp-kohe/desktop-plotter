package org.autogui.plot.data;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

///row and column matrix data utility;
///* an implementation class of {@link List}`<List<Object>>`
///* dynamically expands the size (rows x columns);
///   {@link #add(List)}, {@link #add(int, List)}, {@link #setCell(int, int, Object)} ,
///   {@link #addCell(int, int, Object)}, {@link #fillCell(int, int, Object)}, and so on.
/// Also, {@link #getRow(int)} expands rows.
///* if the specified index is over rows and columns sizes, returns 0 ({@link #getZero()});
///* {@link PlotterDataColumn} can manipulate column data at once.
public class PlotterData extends AbstractList<List<Object>> {
    protected ArrayList<List<Object>> data;
    protected ArrayList<PlotterDataColumn> columns;
    protected int columnSize;

    public static PlotterData create() {
        return new PlotterData();
    }

    public static PlotterData create(int capacity) {
        return new PlotterData(capacity);
    }

    public PlotterData() {
        data = new ArrayList<>();
        columns = new ArrayList<>();
    }

    public PlotterData(int capacity) {
        data = new ArrayList<>(capacity);
        columns = new ArrayList<>(capacity);
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
        columns.stream()
                .filter(c -> c.getRowIndex() >= index)
                .forEach(PlotterDataColumn::shiftRowIndex);

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
        return data.get(row);
    }

    public Object getCell(int row, int col) {
        if (row >= data.size()) {
            return getZero();
        } else {
            List<Object> rowList = data.get(row);
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
        for (int c = columns.size(), cs = Math.min(columnSize - 1, col); c <= cs; ++c) {
            columns.add(new PlotterDataColumn(this, c));
        }
        return columns.get(col);
    }

    public PlotterDataColumn getColumnFirst() {
        return getColumn(0);
    }

    public PlotterDataColumn getColumnLast() {
        return getColumn(Math.max(0, getColumnSize() - 1));
    }

    public <E> PlotterDataColumn addColumn() {
        int i = getColumnSize();
        PlotterDataColumn col = new PlotterDataColumn(this, i)
                .fillRest(getZero())
                .moveRowFirst();
        if (i > 0) {
            getColumn(i - 1); //expand
        }
        columns.add(col);
        columnSize = i + 1; //considering no rows case
        return col;
    }

    public <E> PlotterDataColumn addColumnFirst() {
        int s = getColumnSize();
        for (int r = 0, rs = getRowSize(); r < rs; ++r) {
            addCell(r, 0, getZero());
        }
        columns.forEach(PlotterDataColumn::shiftColumnIndex);
        PlotterDataColumn col = new PlotterDataColumn(this, 0);
        columns.add(0, col);
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
        protected int rowIndex = 0;
        protected int columnIndex;

        public PlotterDataColumn(PlotterData owner, int columnIndex) {
            this.owner = owner;
            this.columnIndex = columnIndex;
        }

        public PlotterDataColumn(PlotterData owner, int rowIndex, int columnIndex) {
            this.owner = owner;
            this.rowIndex = rowIndex;
            this.columnIndex = columnIndex;
        }

        public PlotterDataColumn shiftColumnIndex() {
            this.columnIndex++;
            return this;
        }

        public PlotterDataColumn shiftRowIndex() {
            this.rowIndex++;
            return this;
        }

        public PlotterData end() {
            return owner;
        }

        public int getRowIndex() {
            return rowIndex;
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

        public PlotterDataColumn moveRowFirst() {
            rowIndex = 0;
            return this;
        }

        public PlotterDataColumn moveRowLast() {
            rowIndex = owner.getRowSize() - 1;
            return this;
        }

        public PlotterDataColumn moveRowLastNext() {
            rowIndex = owner.getRowSize();
            return this;
        }

        public PlotterDataColumn moveRowNext() {
            ++rowIndex;
            return this;
        }

        public PlotterDataColumn moveRowPrevious() {
            rowIndex = Math.max(0, rowIndex - 1);
            return this;
        }

        public PlotterDataColumn moveRow(int row) {
            rowIndex = row;
            return this;
        }

        public <E> PlotterDataColumn set(E rowValue) {
            owner.setCell(rowIndex, columnIndex, rowValue);
            ++rowIndex;
            return this;
        }

        public <E> PlotterDataColumn setAll(List<E> rowValues) {
            rowValues.forEach(this::set);
            return this;
        }

        public <E> PlotterDataColumn setAll(E... rowValues) {
            Arrays.stream(rowValues)
                    .forEach(this::set);
            return this;
        }

        public <E> PlotterDataColumn set(Function<Integer,E> rowToValue) {
            return set(rowToValue.apply(rowIndex));
        }

        public <E> PlotterDataColumn set(BiFunction<Integer,Integer,E> rowColumnToValue) {
            return set(rowColumnToValue.apply(rowIndex, columnIndex));
        }


        public <E> PlotterDataColumn set(int n, Function<Integer,E> rowToValue) {
            return set(n, (r,c) -> rowToValue.apply(r));
        }

        public <E> PlotterDataColumn set(int n, BiFunction<Integer,Integer,E> rowColumnToValue) {
            for (int i = 0; i < n; ++i) {
                set(rowColumnToValue.apply(rowIndex, columnIndex));
            }
            return this;
        }

        public <E> PlotterDataColumn fill(int n, E e) {
            return set(n, (r,c) -> e);
        }

        public <E> PlotterDataColumn setRest(Function<Integer,E> rowToValue) {
            return setRest((r,c) -> rowToValue.apply(r));
        }

        public <E> PlotterDataColumn setRest(BiFunction<Integer,Integer,E> rowColumnToValue) {
            for (int r = rowIndex, rs = owner.getRowSize(); r < rs; ++r) {
                set(rowColumnToValue.apply(r, columnIndex));
            }
            return this;
        }

        public <E> PlotterDataColumn fillRest(E e)  {
            return setRest((r,c) -> e);
        }

        public PlotterDataColumn setAll(float[] rows) {
            for (float f : rows) {
                set(f);
            }
            return this;
        }

        public PlotterDataColumn setAll(double[] rows) {
            Arrays.stream(rows)
                    .forEach(this::set);
            return this;
        }

        public PlotterDataColumn setAll(int[] rows) {
            Arrays.stream(rows)
                    .forEach(this::set);
            return this;
        }

        public PlotterDataColumn setAll(long[] rows) {
            Arrays.stream(rows)
                    .forEach(this::set);
            return this;
        }

        public <E> PlotterDataColumn update(int n, Function<E, E> valueToValue) {
            return update(n, (int r, int c, E v) -> valueToValue.apply(v));
        }

        public <E> PlotterDataColumn update(int n, BiFunction<Integer, E, E> rowValueToValue) {
            return update(n, (int r,int c, E v) -> rowValueToValue.apply(r, v));
        }

        @SuppressWarnings("unchecked")
        public <E> PlotterDataColumn update(int n, IndexAndValue<E> rowColValueToValue) {
            for (int i = 0; i < n; ++i) {
                set(rowColValueToValue.apply(rowIndex, columnIndex, (E) owner.getCell(rowIndex, columnIndex)));
            }
            return this;
        }

        public <E> PlotterDataColumn updateRest(Function<E, E> valueToValue) {
            return updateRest((int r, int c, E v) -> valueToValue.apply(v));
        }

        public <E> PlotterDataColumn updateRest(BiFunction<Integer, E, E> rowValueToValue) {
            return updateRest((int r,int c, E v) -> rowValueToValue.apply(r, v));
        }

        @SuppressWarnings("unchecked")
        public <E> PlotterDataColumn updateRest(IndexAndValue<E> rowColValueToValue) {
            for (int r = rowIndex, rs = owner.getRowSize(); r < rs; ++r) {
                set(rowColValueToValue.apply(r, columnIndex, (E) owner.getCell(r, columnIndex)));
            }
            return this;
        }

        public Object get() {
            return owner.getCell(rowIndex, columnIndex);
        }

        @Override
        public String toString() {
            return "cell(row=" + rowIndex + ",col=" + columnIndex + ")";
        }
    }

    public interface IndexAndValue<E> {
        E apply(int rowIndex, int columnIndex, E e);
    }

    @Override
    public String toString() {
        return "[" + data.stream()
                .map(Object::toString)
                .collect(Collectors.joining(",\n")) + "]";
    }
}
