package org.autogui.plot.data;

import java.util.*;
import java.util.function.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

///declarative matrix data builder via fluent interfaces.
/// The class translates from a table-data to a plotting-data.
///  For distinguishing those 2 input and output data, use different terms;
///
///* Input table-data: *line*s of *field*s. Each line contains separated fields.
///* Output plotting-data: *row*s of *column*s. Column defines a series of plotting-points.
///    Each row contains an instant value of each column.
///
///* {@link #selectLine()} : starts a specification of lines as {@link BuilderSelect}
///   * line selectors: {@link BuilderSelect#line(int)}, {@link BuilderSelect#lineIfLine(Predicate)}, ...
///   * binders : {@link BuilderSelect#bindColumnIndexIf(String, BiFunction)} ; `name, (colVal, colIdx) -> bool`, ...
///   * takers
public class PlotterDataBuilder {
    protected List<BuilderSelect<?>> lines = new ArrayList<>();
    protected Function<Set<String>,String> rowHeadLabel = ss -> String.join(":", ss);
    public static PlotterDataBuilder create() {
        return new PlotterDataBuilder();
    }

    public BuilderSelectHeader selectHeader() {
        return selectHeader("");
    }

    public BuilderSelectHeader selectHeader(String definedHeaderName) {
        var l = new BuilderSelectHeader(this, definedHeaderName);
        lines.add(l);
        return l;
    }

    public BuilderSelectData selectData() {
        return selectData("");
    }

    public BuilderSelectData selectData(String usingHeaderName) {
        BuilderSelectData l = new BuilderSelectData(this, usingHeaderName);
        lines.add(l);
        return l;
    }



    public static abstract class BuilderSelect<SelfType extends BuilderSelect<SelfType>> {
        protected PlotterDataBuilder owner;
        protected Set<Integer> selectedLines = new HashSet<>();
        protected List<Predicate<LineInfo>> lineSelectors = new ArrayList<>();
        protected boolean once;

        public BuilderSelect(PlotterDataBuilder owner) {
            this.owner = owner;
            lineIf((ln) -> selectedLines.isEmpty() || selectedLines.contains(ln));
        }

        public PlotterDataBuilder endSelect() {
            return owner;
        }

        public String getUsingHeaderName() {
            return "";
        }

        public boolean isOnce() {
            return once;
        }

        //----------------------

        /**
         * calls of the method construct a selector of disjunction of specified lines
         * @param index a line number index
         * @return this
         */
        @SuppressWarnings("unchecked")
        public SelfType line(int index) {
            selectedLines.add(index);
            return (SelfType) this;
        }

        public SelfType lineTop() {
            return line(0);
        }

        public SelfType lineNotTop() {
            return lineGreaterThan(0);
        }

        public SelfType lineGreaterThan(int index) {
            return lineIf((ln) -> ln > index);
        }

        public SelfType lineLessThan(int index) {
            return lineIf((ln) -> ln < index);
        }

        public SelfType lineGreaterThanEqual(int index) {
            return lineIf((ln) -> ln >= index);
        }

        public SelfType lineLessThanEqual(int index) {
            return lineIf((ln) -> ln <= index);
        }

        public SelfType lineNot(int index) {
            return lineIf((ln) -> ln != index);
        }

        public SelfType lineIf(Predicate<Integer> lineNumTest) {
            return lineIfLine((line) -> lineNumTest.test(line.getLineNum()));
        }

        @SuppressWarnings("unchecked")
        public SelfType lineIfLine(Predicate<LineInfo> lineTest) {
            lineSelectors.add(lineTest);
            return (SelfType) this;
        }

        @SuppressWarnings("unchecked")
        public SelfType once() {
            once = true;
            return (SelfType) this;
        }

        /**
         * @param boundName the field name
         * @return sub-builder for selecting lines by a condition of the field value of the field bound as the name
         */
        @SuppressWarnings("unchecked")
        public BuilderLineField<SelfType> lineFieldName(String boundName) {
            var headerName = getUsingHeaderName();
            return new BuilderLineField<>((SelfType) this,
                    BuilderLineField.predicateExistsByFieldIndex(headerName, boundName),
                    fieldTest ->
                        BuilderLineField.predicateByFieldName(headerName, boundName, fieldTest));
        }

        /**
         * @param fieldIndex the field index
         * @return sub-builder for selecting lines by a condition of the field value of the field at the index
         */
        @SuppressWarnings("unchecked")
        public BuilderLineField<SelfType> lineFieldIndex(int fieldIndex) {
            return new BuilderLineField<>((SelfType) this,
                    BuilderLineField.predicateExistsByFieldIndex(fieldIndex),
                    fieldTest ->
                        BuilderLineField.predicateByFieldIndex(fieldIndex, fieldTest));
        }


        //----------------------

        public boolean run(LineRunningState state,
                        CellAggregation cellAggregation) {
            boolean lineSelected = lineSelectors.stream()
                    .allMatch(p -> p.test(state));
            if (lineSelected) {
                cellAggregation.add(this.runSelected(state));
            }
            return lineSelected;
        }

        public abstract List<Cell> runSelected(LineRunningState state);
    }

    public static class BuilderLineField<OwnerType extends BuilderSelect<OwnerType>> {
        protected OwnerType owner;
        protected Predicate<LineInfo> predicateExists;
        protected Function<Predicate<?>, Predicate<LineInfo>> predicateFactory;

        public BuilderLineField(OwnerType owner, Predicate<LineInfo> predicateExists, Function<Predicate<?>, Predicate<LineInfo>> predicateFactory) {
            this.owner = owner;
            this.predicateExists = predicateExists;
            this.predicateFactory = predicateFactory;
        }

        public <E> OwnerType valueEquals(E fieldValue) {
            return test(s -> s.equals(fieldValue));
        }

        public OwnerType contains(String fieldValue) {
            return test(s -> s.toString().contains(fieldValue));
        }

        public OwnerType startsWith(String fieldValue) {
            return test(s -> s.toString().startsWith(fieldValue));
        }

        public OwnerType endsWith(String fieldValue) {
            return test(s -> s.toString().endsWith(fieldValue));
        }

        public OwnerType matchesPattern(String regex) {
            return test(s -> s.toString().matches(regex));
        }

        public <E> OwnerType test(Predicate<E> fieldTest) {
            return lineIfLine(predicateFactory.apply(fieldTest));
        }

        public OwnerType exists() {
            return lineIfLine(predicateExists);
        }

        public OwnerType notExists() {
            return lineIfLine(line -> !predicateExists.test(line));
        }

        public OwnerType lineIfLine(Predicate<LineInfo> lineTest) {
            return owner.lineIfLine(lineTest);
        }

        public static Predicate<LineInfo> predicateExistsByFieldIndex(String headerName, String boundName) {
            return (line) -> line.getBindingIndices(headerName, boundName).stream()
                    .allMatch(i -> i < line.getFieldSize());
        }

        public static Predicate<LineInfo> predicateExistsByFieldIndex(int fieldIndex) {
            return (line) -> fieldIndex < line.getFieldSize();
        }

        @SuppressWarnings("unchecked")
        public static <E> Predicate<LineInfo> predicateByFieldName(String headerName, String boundName, Predicate<E> fieldTest) {
            return (line) -> line.getBindingIndices(headerName, boundName).stream()
                    .map(line::getFieldObject)
                    .anyMatch(s -> fieldTest.test((E) s));
        }
        @SuppressWarnings("unchecked")
        public static  <E> Predicate<LineInfo> predicateByFieldIndex(int fieldIndex, Predicate<E> fieldTest) {
            return (line) -> fieldTest.test((E) line.getFieldObject(fieldIndex));
        }
    }

    public static final int lineIndexNone = -1;

    public static class BuilderSelectHeader extends BuilderSelect<BuilderSelectHeader> {
        protected String headerName;
        protected List<BindFieldIndex> binders = new ArrayList<>();
        public BuilderSelectHeader(PlotterDataBuilder owner, String headerName) {
            super(owner);
            this.headerName = headerName;
            this.once = true;
        }

        //----------------------

        /**
         * @param nameAsValue bind the field-index to the name, if the field-value equals to the name
         * @return this
         */
        public BuilderSelectHeader bindFieldIndexNameAsValue(String nameAsValue) {
            return bindFieldIndex(nameAsValue, nameAsValue);
        }

        public BuilderSelectHeader bindFieldIndex(String name, String headValue) {
            return bindFieldIndexIf(name, maxAccessingFieldIndexNone, (fld, val) -> Objects.toString(val).equals(headValue));
        }

        public BuilderSelectHeader bindFieldIndexContains(String name, String headValue) {
            return bindFieldIndexIf(name,  maxAccessingFieldIndexNone,(fld, val) -> Objects.toString(val).contains(headValue));
        }

        public BuilderSelectHeader bindFieldIndexStartsWith(String name, String headValue) {
            return bindFieldIndexIf(name, maxAccessingFieldIndexNone, (fld, val) -> Objects.toString(val).startsWith(headValue));
        }

        public BuilderSelectHeader bindFieldIndexEndsWith(String name, String headValue) {
            return bindFieldIndexIf(name,  maxAccessingFieldIndexNone,(fld, val) -> Objects.toString(val).endsWith(headValue));
        }

        public BuilderSelectHeader bindFieldIndexPattern(String name, String regex) {
            return bindFieldIndexIf(name, maxAccessingFieldIndexNone, (fld, val) -> Objects.toString(val).matches(regex));
        }

        public BuilderSelectHeader bindFieldIndexPattern(String name, Pattern regex) {
            return bindFieldIndexIf(name, maxAccessingFieldIndexNone, (fld, val) -> regex.matcher(Objects.toString(val)).matches());
        }

        public BuilderSelectHeader bindFieldIndexIn(String name, Object... headValues) {
            return bindFieldIndexIn(name, List.of(headValues));
        }

        public BuilderSelectHeader bindFieldIndexIn(String name, Collection<Object> headValues) {
            return bindFieldIndexIf(name, maxAccessingFieldIndexNone, (fld, val) -> headValues.contains(val));
        }

        public BuilderSelectHeader bindFieldIndex(String name, int index) {
            return bindFieldIndexInIndices(name, List.of(index));
        }

        public BuilderSelectHeader bindFieldIndexInIndices(String name, int... fieldIndices) {
            return bindFieldIndexInIndices(name, IntStream.of(fieldIndices)
                    .boxed().toList());
        }

        public BuilderSelectHeader bindFieldIndexInIndices(String name, Collection<Integer> fieldIndices) {
            return bindFieldIndexIf(name, fieldIndices.stream()
                    .max(Comparator.naturalOrder())
                    .orElse(maxAccessingFieldIndexNone),
                    (fld, val) -> fieldIndices.contains(fld));
        }

        public BuilderSelectHeader bindFieldIndexRange(String name, int fromFieldIndexInclusive, int toFieldIndexExclusive) {
            return bindFieldIndexIf(name, toFieldIndexExclusive - 1, (fld, val) -> fromFieldIndexInclusive <= fld && fld < toFieldIndexExclusive);
        }

        public BuilderSelectHeader bindFieldIndexRangeToLast(String name, int fromFieldIndexInclusive) {
            return bindFieldIndexIf(name, fromFieldIndexInclusive, (fld, val) -> fromFieldIndexInclusive <= fld);
        }

        public BuilderSelectHeader bindFieldIndexRangeFromFirst(String name, int toFieldIndexExclusive) {
            return bindFieldIndexIf(name, toFieldIndexExclusive - 1, (fld, val) -> fld < toFieldIndexExclusive);
        }

        public BuilderSelectHeader bindFieldIndexIf(String name, Predicate<Object> fieldValueTest) {
            binders.add(new BindFieldIndex(name, maxAccessingFieldIndexNone, (fld, val) -> fieldValueTest.test(val)));
            return this;
        }

        public BuilderSelectHeader bindFieldIndexIf(String name, int maxAccessingFieldIndex, BiFunction<Integer, Object, Boolean> fieldIndexAndValueTest) {
            binders.add(new BindFieldIndex(name, maxAccessingFieldIndex, fieldIndexAndValueTest));
            return this;
        }

        public BuilderSelectHeader noLine() {
            return line(lineIndexNone);
        }

        @Override
        public BuilderSelectHeader line(int index) {
            super.line(index);
            if (isMultiline()) {
                once = false;
            }
            return this;
        }

        @Override
        public BuilderSelectHeader lineIf(Predicate<Integer> lineNumTest) {
            super.lineIf(lineNumTest);
            if (isMultiline()) {
                once = false;
            }
            return this;
        }

        protected boolean isMultiline() {
            return this.selectedLines.size() > 1 || this.lineSelectors.size() > 1;
        }

        public List<Cell> runSelected(LineRunningState state) {
            if (state.getLineNum() == lineIndexNone) {
                int maxAccessingFieldIndex = binders.stream()
                        .map(BindFieldIndex::getMaxAccessingFieldIndex)
                        .filter(i -> i != maxAccessingFieldIndexNone)
                        .max(Comparator.naturalOrder())
                        .orElse(maxAccessingFieldIndexNone);
                if (maxAccessingFieldIndex != maxAccessingFieldIndexNone) {
                    IntStream.range(0, maxAccessingFieldIndex + 1)
                            .forEach(fieldIndex ->
                                    binders.forEach(b ->
                                            b.run(state, headerName, fieldIndex)));
                }
            } else {
                IntStream.range(0, state.getFieldSize())
                        .forEach(fieldIndex ->
                                binders.forEach(b ->
                                        b.run(state, headerName, fieldIndex)));
            }
            return List.of();
        }
    }

    public static class BuilderSelectData extends BuilderSelect<BuilderSelectData> {
        protected List<TakeFieldToTarget> valueGenerators = new ArrayList<>();
        protected List<TakeFieldToTarget> propertySetters = new ArrayList<>();
        protected String usingHeaderName;

        public BuilderSelectData(PlotterDataBuilder owner, String headerName) {
            super(owner);
            this.usingHeaderName = headerName;
        }

        @Override
        public String getUsingHeaderName() {
            return usingHeaderName;
        }

        public BuilderTakeField<BuilderSelectData> takeFieldName(String boundName) {
            return new BuilderTakeField<>(this, new TakeFieldFromBinding(usingHeaderName, boundName));
        }

        public BuilderTakeField<BuilderSelectData> takeFieldNameAsList(String boundName) {
            return new BuilderTakeField<>(this, new TakeFieldFromBindingAsList(usingHeaderName, boundName));
        }

        public BuilderTakeField<BuilderSelectData> takeFieldIndex(int fieldIndex) {
            return new BuilderTakeField<>(this, new TakeFieldIndex(usingHeaderName, fieldIndex));
        }

        public BuilderSelectData addTakeFieldToTarget(TakeFieldToTarget t) {
            if (t.isValueGenerator()) {
                valueGenerators.add(t);
            } else {
                propertySetters.add(t);
            }
            return this;
        }

        @Override
        public boolean run(LineRunningState state, CellAggregation cellAggregation) {
            if (state.getLineNum() == lineIndexNone) {
                return false;
            } else {
                return super.run(state, cellAggregation);
            }
        }

        public List<Cell> runSelected(LineRunningState state) {
            state.clearTargets();
            valueGenerators.forEach(g -> g.run(state));
            List<Cell> cells = state.getTargets();
            if (!cells.isEmpty()) {
                propertySetters.forEach(p -> p.run(state));
            }
            return cells;
        }
    }

    public static class BuilderTakeField<OwnerType extends BuilderSelectData> {
        protected OwnerType owner;
        protected TakeField takeField;
        protected Function<?, Object> valueConverter;

        public BuilderTakeField(OwnerType owner, TakeField takeField) {
            this.owner = owner;
            this.takeField = takeField;
        }

        public BuilderTakeFieldValue<OwnerType> value() {
            return new BuilderTakeFieldValue<>(owner, takeField);
        }

        public OwnerType asColumnLabel() {
            return asColumnLabel((exLabel, v) -> Objects.toString(v));
        }
        @SuppressWarnings("unchecked")
        public <E> OwnerType asColumnLabel(BiFunction<String, E, String> existingColumnLabelAndValueToColumnLabel) {
            return addTakeFieldToTarget(new TakeFieldToTarget(false, takeField, (state, i, v) ->
                    state.forEachTarget(t -> t.columnLabel = existingColumnLabelAndValueToColumnLabel.apply(t.columnLabel, (E) v))));
        }

        public OwnerType asRowLabel() {
            return asRowLabel((exLabel, v) -> Objects.toString(v));
        }

        @SuppressWarnings("unchecked")
        public <E> OwnerType asRowLabel(BiFunction<String, E, String> existingRowLabelAndValueToRowLabel) {
            return addTakeFieldToTarget(new TakeFieldToTarget(false, takeField, (state, i, v) ->
                    state.forEachTarget(t -> {
                        t.rowLabelName = i.name;
                        t.rowLabel = existingRowLabelAndValueToRowLabel.apply(t.rowLabel, (E) v);
                    })));
        }

        public OwnerType rowSortKey() {
            return addTakeFieldToTarget(takeColumnToTargetRowSortKey(takeField));
        }

        public OwnerType columnSortKey() {
            return addTakeFieldToTarget(takeColumnToTargetColumnSortKey(takeField));
        }

        @SuppressWarnings("unchecked")
        public <E> BuilderTakeField<OwnerType> convert(Function<E,Object> converter) {
            if (valueConverter == null) {
                this.valueConverter = converter;
            } else {
                this.valueConverter = (v) -> converter.apply((E) ((Function<Object,Object>) valueConverter).apply(v));
            }
            return this;
        }

        public BuilderTakeField<OwnerType> valueOccurrences() {
            return valueOccurrences((v, i) -> i);
        }

        @SuppressWarnings("unchecked")
        public <E> BuilderTakeField<OwnerType> valueOccurrences(BiFunction<E, Integer, Object> valueOccurrencesCombiner) {
            if (!(takeField instanceof TakeColumnOccurrence)) {
                takeField = new TakeColumnOccurrence(takeField, (v, i) -> valueOccurrencesCombiner.apply((E) v, i));
            }
            return this;
        }

        @SuppressWarnings("unchecked")
        public OwnerType addTakeFieldToTarget(TakeFieldToTarget t) {
            if (valueConverter != null) {
                t = t.setValueConverter(valueConverter);
            }
            return (OwnerType) owner.addTakeFieldToTarget(t);
        }
        //----------------------
        public static TakeFieldToTarget takeColumnToTargetRowSortKey(TakeField takeColumn) {
            return new TakeFieldToTarget(false, takeColumn, (state, i, v) -> state.forEachTarget(t -> t.rowSortKey = v));
        }

        public static TakeFieldToTarget takeColumnToTargetColumnSortKey(TakeField takeColumn) {
            return new TakeFieldToTarget(false, takeColumn, (state, i, v) -> state.forEachTarget(t -> t.columnSortKey = v));
        }
    }

    public static class BuilderTakeFieldValue<OwnerType extends BuilderSelectData> {
        protected OwnerType owner;
        protected TakeField takeField;
        protected TakeColumnRunnerCell takeCell;

        public BuilderTakeFieldValue(OwnerType owner, TakeField takeField) {
            this.owner = owner;
            this.takeField = takeField;
            takeCell = new TakeColumnRunnerCell();
        }

        public BuilderTakeFieldValue<OwnerType> fieldIndexAsColumnLabel() {
            takeCell.setColumnLabelSetter(TakeLabel::takeLabelFieldIndex);
            return this;
        }
        public BuilderTakeFieldValue<OwnerType> fieldNameOrIndexAsColumnLabel() {
            takeCell.setColumnLabelSetter(TakeLabel::takeLabelFieldNameOrIndex);
            return this;
        }
        public BuilderTakeFieldValue<OwnerType> lineIndexAsColumnLabel() {
            takeCell.setColumnLabelSetter(TakeLabel::takeLabelFieldNameOrIndex);
            return this;
        }
        public BuilderTakeFieldValue<OwnerType> lineValueAsColumnLabel() {
            takeCell.setColumnLabelSetter(TakeLabel::takeLabelLineValue);
            return this;
        }
        public BuilderTakeFieldValue<OwnerType> headerValueAsColumnLabel() {
            takeCell.setColumnLabelSetter(TakeLabel::takeLabelHeaderValue);
            return this;
        }
        public BuilderTakeFieldValue<OwnerType> columnLabel(String label) {
            takeCell.setColumnLabelSetter(TakeLabel.takeLabelFixed(label));
            return this;
        }
        public <E> BuilderTakeFieldValue<OwnerType> columnLabelConvert(Function<E, String> converter) {
            takeCell.setColumnLabelSetter(TakeLabel.takeLabelConvert(takeCell.getColumnLabelSetter(), converter));
            return this;
        }
        public BuilderTakeFieldValue<OwnerType> lineAndFieldIndicesAsColumnLabel(TakeLabelLineAndFieldIndices lineAndFieldIndices) {
            takeCell.setColumnLabelSetter(TakeLabel.takeLabelLineAndFieldIndices(lineAndFieldIndices));
            return this;
        }
        public BuilderTakeFieldValue<OwnerType> lineIndexAndFieldNameAsColumnLabel(TakeLabelLineIndexAndFieldName lineIndexAndFieldName) {
            takeCell.setColumnLabelSetter(TakeLabel.takeLabelLineIndexAndFieldName(lineIndexAndFieldName));
            return this;
        }
        public BuilderTakeFieldValue<OwnerType> lineAndFieldIndicesKeyAsColumnLabel() {
            return lineAndFieldIndicesAsRowLabel(PlotterDataBuilder::key);
        }


        public BuilderTakeFieldValue<OwnerType> fieldIndexAsRowLabel() {
            takeCell.setRowLabelSetter(TakeLabel::takeLabelFieldIndex);
            return this;
        }
        public BuilderTakeFieldValue<OwnerType> fieldNameOrIndexAsRowLabel() {
            takeCell.setRowLabelSetter(TakeLabel::takeLabelFieldNameOrIndex);
            return this;
        }
        public BuilderTakeFieldValue<OwnerType> lineIndexAsRowLabel() {
            takeCell.setRowLabelSetter(TakeLabel::takeLabelLineIndex);
            return this;
        }
        public BuilderTakeFieldValue<OwnerType> lineValueAsRowLabel() {
            takeCell.setRowLabelSetter(TakeLabel::takeLabelLineValue);
            return this;
        }
        public BuilderTakeFieldValue<OwnerType> headerValueAsRowLabel() {
            takeCell.setRowLabelSetter(TakeLabel::takeLabelHeaderValue);
            return this;
        }
        public BuilderTakeFieldValue<OwnerType> rowLabel(String label) {
            takeCell.setRowLabelSetter(TakeLabel.takeLabelFixed(label));
            return this;
        }
        public <E> BuilderTakeFieldValue<OwnerType> rowLabelConvert(Function<E, String> converter) {
            takeCell.setRowLabelSetter(TakeLabel.takeLabelConvert(takeCell.getRowLabelSetter(), converter));
            return this;
        }
        public BuilderTakeFieldValue<OwnerType> lineAndFieldIndicesAsRowLabel(TakeLabelLineAndFieldIndices lineAndFieldIndices) {
            takeCell.setRowLabelSetter(TakeLabel.takeLabelLineAndFieldIndices(lineAndFieldIndices));
            return this;
        }
        public BuilderTakeFieldValue<OwnerType> lineIndexAndFieldNameAsRowLabel(TakeLabelLineIndexAndFieldName lineIndexAndFieldName) {
            takeCell.setRowLabelSetter(TakeLabel.takeLabelLineIndexAndFieldName(lineIndexAndFieldName));
            return this;
        }
        public BuilderTakeFieldValue<OwnerType> lineAndFieldIndicesKeyAsRowLabel() {
            takeCell.setRowLabelSetter(TakeLabel.takeLabelLineAndFieldIndices(PlotterDataBuilder::key));
            return this;
        }

        @SuppressWarnings("unchecked")
        public OwnerType endTake() {
            return (OwnerType) owner.addTakeFieldToTarget(new TakeFieldToTarget(true, takeField, takeCell));
        }
    }

    public static String key(int line, List<Integer> field) {
        return line + "." + field;
    }

    public PlotterDataBuilder rowHeadLabel(String label) {
        return rowHeadLabel((ss) -> label);
    }

    public PlotterDataBuilder rowHeadLabel(Function<Set<String>, String> rowLabelNamesToRowHeadLabel) {
        this.rowHeadLabel = rowLabelNamesToRowHeadLabel;
        return this;
    }

    public <E> PlotterData run(Iterable<List<E>> source) {
        return run(new LineRunningStateList<E>(), source, LineRunningStateList::setLine);
    }

    public <StateType extends LineRunningState, LineType> PlotterData run(StateType state, Iterable<LineType> source,
                                                                          BiConsumer<StateType,LineType> lineSetter) {
        CellAggregation cells = new CellAggregation(rowHeadLabel);
        state.setLineNum(lineIndexNone);
        var lines = new ArrayList<>(this.lines);
        for (Iterator<BuilderSelect<?>> iter = lines.iterator(); iter.hasNext(); ) {
            var l = iter.next();
            if (l.run(state, cells)) {
                if (l.isOnce()) {
                    iter.remove();
                }
            }
        }
        int lineNum = 0;
        for (LineType line : source) {
            state.setLineNum(lineNum);
            lineSetter.accept(state, line);
            for (Iterator<BuilderSelect<?>> iter = lines.iterator(); iter.hasNext(); ) {
                var l = iter.next();
                if (l.run(state, cells)) {
                    if (l.isOnce()) {
                        iter.remove();
                    }
                    break;
                }
            }
            ++lineNum;
        }
        return cells.build();
    }

    public static final int maxAccessingFieldIndexNone = -1;

    public static class BindFieldIndex {
        protected String name;
        protected int maxAccessingFieldIndex;
        protected BiFunction<Integer, Object, Boolean> fieldIndexAndValueTest;

        public BindFieldIndex(String name, int maxAccessingFieldIndex, BiFunction<Integer, Object, Boolean> fieldIndexAndValueTest) {
            this.name = name;
            this.maxAccessingFieldIndex = maxAccessingFieldIndex;
            this.fieldIndexAndValueTest = fieldIndexAndValueTest;
        }

        public int getMaxAccessingFieldIndex() {
            return maxAccessingFieldIndex;
        }

        public void run(LineRunningState state, String headerName, int fieldIndex) {
            var fieldValue = state.getField(List.of(fieldIndex));
            if (fieldIndexAndValueTest.apply(fieldIndex, fieldValue)) {
                String boundName = (name == null) ?
                        Objects.toString(fieldValue) : //value as name
                        name; //specified name
                state.putBinding(new FieldIndexBinding(headerName, boundName, List.of(fieldIndex), fieldValue));
            }
        }

        @Override
        public String toString() {
            return "bind(" + name + ", " + fieldIndexAndValueTest + ")";
        }
    }

    public static class TakeFieldToTarget {
        protected boolean valueGenerator;
        protected TakeField takeField;
        protected TakeColumnRunner runner;
        protected Function<Object,Object> valueConverter;

        public TakeFieldToTarget(boolean valueGenerator, TakeField takeField,
                                 TakeColumnRunner runner) {
            this.valueGenerator = valueGenerator;
            this.takeField = takeField;
            this.runner = runner;
        }

        public boolean isValueGenerator() {
            return valueGenerator;
        }

        @SuppressWarnings("unchecked")
        public <E> TakeFieldToTarget setValueConverter(Function<E,Object> valueConverter) {
            this.valueConverter = (Function<Object,Object>) valueConverter;
            return this;
        }

        public void run(LineRunningState state) {
            if (valueConverter != null) {
                TakeColumnRunner f2 = (s, c, v) -> runner.run(s, c, valueConverter.apply(v));
                takeField.run(state, f2);
            } else {
                takeField.run(state, runner);
            }
        }

        @Override
        public String toString() {
            return "take(" + (valueGenerator ? "value" : "property") + ", " + takeField + ", " + runner + ")";
        }
    }

    public static class TakeColumnRunnerCell implements TakeColumnRunner {
        protected TakeLabel columnLabelSetter;
        protected TakeLabel rowLabelSetter;

        public TakeColumnRunnerCell() {
            this(TakeLabel::takeLabelFieldIndex, TakeLabel::takeLabelLineIndex);
        }

        public TakeColumnRunnerCell(TakeLabel columnLabelSetter, TakeLabel rowLabelSetter) {
            this.columnLabelSetter = columnLabelSetter;
            this.rowLabelSetter = rowLabelSetter;
        }

        public void setColumnLabelSetter(TakeLabel columnLabelSetter) {
            this.columnLabelSetter = columnLabelSetter;
        }

        public void setRowLabelSetter(TakeLabel rowLabelSetter) {
            this.rowLabelSetter = rowLabelSetter;
        }

        public TakeLabel getColumnLabelSetter() {
            return columnLabelSetter;
        }

        public TakeLabel getRowLabelSetter() {
            return rowLabelSetter;
        }

        @Override
        public void run(LineRunningState state, FieldIndexBinding binding, Object value) {
            Cell cell = state.addNewTarget();
            cell.value = value;
            cell.columnLabel = columnLabelSetter.takeLabelAsString(state, binding, value, cell);
            cell.rowLabel = rowLabelSetter.takeLabelAsString(state, binding, value, cell);
        }
    }

    public interface TakeLabel {
        default String takeLabelAsString(LineRunningState state, FieldIndexBinding binding, Object value, Cell cell) {
            var obj = takeLabel(state, binding, value, cell);
            return obj == null ? "" : obj.toString();
        }
        Object takeLabel(LineRunningState state, FieldIndexBinding binding, Object value, Cell cell);

        static Object takeLabelFieldIndex(LineRunningState state, FieldIndexBinding binding, Object value, Cell cell) {
            return binding.indices;
        }

        static Object takeLabelLineIndex(LineRunningState state, FieldIndexBinding binding, Object value, Cell cell) {
            return cell.lineIndex;
        }

        static String takeLabelFieldNameOrIndex(LineRunningState state, FieldIndexBinding binding, Object value, Cell cell) {
            return binding.name == null ? binding.indicesString() : binding.name;
        }

        static Object takeLabelHeaderValue(LineRunningState state, FieldIndexBinding binding, Object value, Cell cell) {
            return binding.value;
        }

        static Object takeLabelLineValue(LineRunningState state, FieldIndexBinding binding, Object value, Cell cell) {
            return value;
        }

        static TakeLabel takeLabelFixed(String label) {
            return (state, binding, value, cell) -> label;
        }

        @SuppressWarnings("unchecked")
        static <E> TakeLabel takeLabelConvert(TakeLabel wrapped, Function<E, String> converter) {
            return (state, binding, value, cell) ->
                    converter.apply((E) wrapped.takeLabel(state, binding, value, cell));
        }

        static TakeLabel takeLabelLineAndFieldIndices(TakeLabelLineAndFieldIndices lineAndFieldToLabel) {
            return (state, binding, value, cell) -> lineAndFieldToLabel.apply(cell.lineIndex, binding.indices);
        }

        static TakeLabel takeLabelLineIndexAndFieldName(TakeLabelLineIndexAndFieldName lineAndFieldToLabel) {
            return (state, binding, value, cell) ->
                    lineAndFieldToLabel.apply(cell.lineIndex, takeLabelFieldNameOrIndex(state, binding, value, cell));
        }
    }

    public interface TakeLabelLineAndFieldIndices {
        String apply(int lineIndex, List<Integer> fieldIndex);
    }

    public interface TakeLabelLineIndexAndFieldName {
        String apply(int lineIndex, String fieldName);
    }

    //----------------------

    public interface TakeColumnRunner {
        void run(LineRunningState state, FieldIndexBinding binding, Object value);
    }

    public interface TakeField {
        void run(LineRunningState state, TakeColumnRunner target);
    }

    public static class TakeColumnOccurrence implements TakeField {
        protected TakeField takeField;
        protected BiFunction<Object, Integer, Object> valueOccurrenceCombiner;

        public TakeColumnOccurrence(TakeField takeField, BiFunction<Object, Integer, Object> valueOccurrenceCombiner) {
            this.takeField = takeField;
            this.valueOccurrenceCombiner = valueOccurrenceCombiner;
        }

        @Override
        public void run(LineRunningState state, TakeColumnRunner target) {
            takeField.run(state,
                    (s, c,v) -> target.run(s, c,
                            valueOccurrenceCombiner.apply(v, s.getAndIncrementOccurrences(v))));
        }

        @Override
        public String toString() {
            return "occurrences(" + takeField + ")";
        }
    }

    public static class TakeFieldIndex implements TakeField {
        protected FieldIndexBinding index;

        public TakeFieldIndex(String headerName, int fieldIndex) {
            this.index = new FieldIndexBinding(headerName, null, List.of(fieldIndex), fieldIndex);
        }

        @Override
        public void run(LineRunningState state,
                          TakeColumnRunner target) {
            target.run(state, index, state.getFieldObject(index));
        }

        @Override
        public String toString() {
            return "index(" + index.indices + ")";
        }
    }

    public static class TakeFieldFromBinding implements TakeField {
        protected String headerName;
        protected String fieldName;

        public TakeFieldFromBinding(String headerName, String fieldName) {
            this.headerName = headerName;
            this.fieldName = fieldName;
        }

        @Override
        public void run(LineRunningState state,
                        TakeColumnRunner target) {
            List<FieldIndexBinding> bindings = state.getBinding(headerName, fieldName);
            if (bindings != null) {
                run(state, target, bindings);
            }
        }

        protected void run(LineRunningState state, TakeColumnRunner target, List<FieldIndexBinding> bindings) {
            bindings.forEach(binding ->
                    target.run(state, binding, state.getFieldObject(binding)));
        }

        @Override
        public String toString() {
            return "binding(" + fieldName + ")";
        }
    }

    public static class TakeFieldFromBindingAsList extends TakeFieldFromBinding {
        public TakeFieldFromBindingAsList(String headerName, String fieldName) {
            super(headerName, fieldName);
        }

        @Override
        protected void run(LineRunningState state, TakeColumnRunner target, List<FieldIndexBinding> bindings) {
            target.run(state, FieldIndexBinding.combine(bindings), bindings.stream()
                    .map(state::getFieldObject)
                    .toList());
        }
    }

    public interface LineInfo {
        int getFieldSize();
        int getLineNum();
        Object getField(List<Integer> indices);
        Object getFieldObject(int columnIndex);

        List<Integer> getBindingIndices(String headerName, String name);
    }

    public static abstract class LineRunningState implements LineInfo {
        //inter-lines state
        protected Map<String, Map<String, List<FieldIndexBinding>>> fieldIndexBindings; //headerName -> fieldName -> binding
        protected Map<Object, Integer> valueToOccurrences;

        //line state
        protected int lineNum = -1;

        protected List<Cell> targets = new ArrayList<>();

        public LineRunningState() {
            fieldIndexBindings = new LinkedHashMap<>();
            valueToOccurrences = new HashMap<>();
        }

        @Override
        public abstract int getFieldSize();

        @Override
        public abstract Object getField(List<Integer> indices);

        @Override
        public abstract Object getFieldObject(int index);

        public int getLineNum() {
            return lineNum;
        }

        public void setLineNum(int lineNum) {
            this.lineNum = lineNum;
        }

        public Object getFieldObject(FieldIndexBinding binding) {
            return binding.isSingleIndex() ?
                getFieldObject(binding.indices.getFirst()) :
                binding.indices.stream()
                        .map(this::getFieldObject)
                        .toList();
        }

        public void putBinding(FieldIndexBinding binding) {
            fieldIndexBindings.computeIfAbsent(binding.headerName, h -> new LinkedHashMap<>())
                    .computeIfAbsent(binding.name, n -> new ArrayList<>())
                    .add(binding);
        }

        public List<FieldIndexBinding> getBinding(String headerName, String name) {
            return fieldIndexBindings.getOrDefault(headerName, Map.of()).get(name);
        }

        @Override
        public List<Integer> getBindingIndices(String headerName, String name) {
            List<FieldIndexBinding> is = getBinding(headerName, name);
            if (is == null) {
                return Collections.emptyList();
            } else {
                return is.stream()
                        .flatMap(i -> i.indices.stream())
                        .toList();
            }
        }

        public List<Cell> getTargets() {
            return targets;
        }

        public void forEachTarget(Consumer<Cell> setter) {
            targets.forEach(setter);
        }

        public void clearTargets() {
            targets.clear();
        }

        public Cell addNewTarget() {
            Cell cell = new Cell(lineNum);
            targets.add(cell);
            return cell;
        }

        public int getAndIncrementOccurrences(Object value) {
            return valueToOccurrences.compute(value, (k, v) -> (v == null ? 0 : v) + 1) - 1;
        }
    }

    public static class LineRunningStateList<E> extends LineRunningState {
        List<E> line;

        public void setLine(List<E> line) {
            this.line = line;
        }

        public List<E> getLine() {
            return line;
        }

        @Override
        public int getFieldSize() {
            return line == null ? 0 : line.size();
        }

        @Override
        public Object getField(List<Integer> indices) {
            return indices.size() == 1 ?
                    getFieldObject(indices.getFirst()) :
                    indices.stream()
                        .map(this::getFieldObject)
                        .toList();
        }

        @Override
        public Object getFieldObject(int fieldIndex) {
            if (line != null && fieldIndex < line.size()) {
                return line.get(fieldIndex);
            } else {
                return "";
            }
        }

        @Override
        public String toString() {
            return "state(" + lineNum + ":" + line + "," + fieldIndexBindings + "," + targets + ")";
        }

    }

    public static class FieldIndexBinding {
        public String headerName;
        public String name;
        public List<Integer> indices;
        public Object value;

        public static FieldIndexBinding combine(List<FieldIndexBinding> bindings) {
            List<Object> values = new ArrayList<>(bindings.size());
            List<Integer> indices = new ArrayList<>(bindings.size());
            String headerName = null;
            String name = null;
            for (FieldIndexBinding binding : bindings) {
                if (headerName == null) {
                    headerName = binding.headerName;
                    name = binding.name;
                }
                if (binding.isSingleIndex()) {
                    values.add(binding.value);
                } else if (binding.value instanceof List<?> list){
                    values.addAll(list);
                } else {
                    values.add(binding.value);
                }
                indices.addAll(binding.indices);
            }
            return new FieldIndexBinding(headerName, name, indices, values);
        }

        public FieldIndexBinding(String headerName, String name, List<Integer> indices, Object value) {
            this.headerName = headerName;
            this.name = name;
            this.indices = indices;
            this.value = value;
        }

        public boolean isSingleIndex() {
            return indices.size() == 1;
        }

        public String indicesString() {
            return isSingleIndex() ? Integer.toString(indices.getFirst()) :
                    indices.toString();
        }

        @Override
        public String toString() {
            return "field(" + headerName + ", " + name + "," + indices + "," + value + ")";
        }
    }

    public static class Cell {
        public int lineIndex;
        public Object rowSortKey;
        public Object columnSortKey;
        public String columnLabel;
        public String rowLabelName;
        public String rowLabel;
        public Object value;

        public Cell(int lineIndex) {
            this.lineIndex = lineIndex;
        }

        public Object getRowSortKey() {
            if (rowSortKey == null) {
                return nullKey;
            }
            return rowSortKey;
        }

        public Object getColumnSortKey() {
            if (columnSortKey == null) {
                return nullKey;
            }
            return columnSortKey;
        }

        @Override
        public String toString() {
            return "cell(line=" + lineIndex + ",col=" + columnLabel + ",row=" + rowLabel + ",val=" + value + ",rowSort=" + rowSortKey + ")";
        }
    }

    public static class CellAggregation {
        protected Map<String, Map<String, Object>> rowToColumnToCell = new LinkedHashMap<>();
        protected Set<String> rowLabelNames = new LinkedHashSet<>();
        protected Map<String, Object> columnLabelsToSortKey = new LinkedHashMap<>();
        protected Map<String, Object> rowLabelsToSortKey = new LinkedHashMap<>();
        protected Function<Set<String>,String> rowHeadLabel;

        public CellAggregation(Function<Set<String>, String> rowHeadLabel) {
            this.rowHeadLabel = rowHeadLabel;
        }

        public void add(List<Cell> cells) {
            for (Cell cell : cells) {
                rowToColumnToCell.computeIfAbsent(cell.rowLabel, r -> new LinkedHashMap<>())
                        .put(cell.columnLabel, cell.value);
                columnLabelsToSortKey.compute(cell.columnLabel, (n,exKey) -> selectColumnKey(n, exKey, cell.getColumnSortKey()));
                rowLabelsToSortKey.compute(cell.rowLabel, (n,exKey) -> selectColumnKey(n, exKey, cell.getRowSortKey()));
                if (cell.rowLabelName != null) {
                    rowLabelNames.add(cell.rowLabelName);
                }
            }
        }

        public Object selectColumnKey(String name, Object exKey, Object cKey) {
            if (exKey == null) {
                return cKey;
            } else {
                int c = compare(exKey, cKey);
                if (c > 0) {
                    return cKey;
                } else {
                    return exKey;
                }
            }
        }

        public PlotterData build() {
            PlotterData data = PlotterData.create(rowToColumnToCell.size() + 1);

            setDefaultKey(columnLabelsToSortKey);
            setDefaultKey(rowLabelsToSortKey);

            List<String> columnLabels = toLabels(columnLabelsToSortKey);
            List<String> rowLabels = toLabels(rowLabelsToSortKey);

            List<Object> head = new ArrayList<>(columnLabels.size() + 1);
            head.add(rowHeadLabel.apply(rowLabelNames));
            head.addAll(columnLabels);
            data.add(head);

            for (String row : rowLabels) {
                Map<String,Object> cToC = rowToColumnToCell.get(row);
                var dst = new ArrayList<Object>(columnLabels.size() + 1);
                dst.add(row);
                for (String col : columnLabels) {
                    dst.add(toData(cToC.get(col)));
                }
                data.add(dst);
            }
            return data;
        }

        private void setDefaultKey(Map<String,Object> labelToSortKey) {
            int num = 0;
            for (Map.Entry<String,Object> e : labelToSortKey.entrySet()) {
                if (e.getValue() == null || e.getValue().equals(nullKey)) {
                    e.setValue(num);
                }
                ++num;
            }
        }

        private List<String> toLabels(Map<String,Object> labelToSortKey) {
            return labelToSortKey.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(PlotterDataBuilder::compare))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }

        public Object toRowSortKey(Map<String,Cell> colToCell, Object defVal) {
            return colToCell.values().stream()
                    .map(c -> c.getRowSortKey().equals(nullKey) ? c.rowLabel : c.rowSortKey)
                    .filter(Objects::nonNull)
                    .min(PlotterDataBuilder::compare)
                    .orElse(defVal);
        }


        public List<Object> withHead(String h, List<Object> es) {
            es.addFirst(h);
            return es;
        }

        public Object toData(Object cellValue) {
            if (cellValue == null) {
                return 0.0;
            } else {
                return cellValue;
            }
        }
    }

    public static class NullKey {}
    public static NullKey nullKey = new NullKey();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static int compare(Object l, Object r) {
        if (l == null || l.equals(nullKey)) {
            if (r == null || r.equals(nullKey)) {
                return 0;
            } else {
                return -1;
            }
        }  else if (r == null || r.equals(nullKey)/* && l != null*/) {
            return 1;
        } else if (l.equals(r)) {
            return 0;
        } else {
            if (l.getClass().equals(r.getClass()) && l instanceof Comparable<?>) {
                return ((Comparable) l).compareTo(r);
            } else if (l instanceof Number && r instanceof Number) {
                return Double.compare(((Number) l).doubleValue(), ((Number) r).doubleValue());
            } else {
                int c = l.getClass().getName().compareTo(r.getClass().getName());
                if (c == 0) {
                    return l.toString().compareTo(r.toString());
                } else {
                    return c;
                }
            }
        }
    }
}
