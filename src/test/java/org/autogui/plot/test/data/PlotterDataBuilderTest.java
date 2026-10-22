package org.autogui.plot.test.data;

import org.autogui.plot.data.PlotterDataBuilder;
import org.autogui.plot.data.PlotterDataReducer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PlotterDataBuilderTest {
    @Test
    public void testSingleColumn() {
        var data = List.of(
                List.of("hello"),
                List.of("100"),
                List.of("200"),
                List.of("300"));
        var d = PlotterDataBuilder.create()
                .selectHeader()
                    .lineTop()
                    .bindFieldIndexNameAsValue("hello") //field[0]="hello" named as "hello"
                .endSelect()
                .selectData()
                    .lineNotTop()
                    .takeFieldName("hello").value().fieldIndexAsColumnLabel().endTake() //field[0]
                .endSelect();
        var res = d.run(data);
        System.err.println(res);
        Assertions.assertEquals(
                List.of(List.of("", "[0]"), //header :
                        List.of("1", "100"),
                        List.of("2", "200"),
                        List.of("3", "300")), res,
                "header row and 3 data rows");
    }

    @Test
    public void testSingleFieldToDoubleColumns() {
        var data = List.of(
                List.of("hello"),
                List.of("100"),
                List.of("200"),
                List.of("300"));
        var d = PlotterDataBuilder.create()
                .selectHeader()
                    .lineTop()
                    .bindFieldIndexNameAsValue("hello") //field[0] named as "hello"
                    .bindFieldIndex("world", "hello") //also, field[0] named as "world"
                .endSelect()
                .selectData()
                    .lineNotTop()
                    .takeFieldName("hello").value().fieldNameOrIndexAsColumnLabel().endTake() //field[0] as column "hello"
                    .takeFieldName("world").value().fieldNameOrIndexAsColumnLabel().endTake() //field[1] as column "world"
                .endSelect();
        var res = d.run(data);
        System.err.println(res);
        Assertions.assertEquals(
                List.of(List.of("", "hello", "world"), //header :
                        List.of("1", "100", "100"),
                        List.of("2", "200", "200"),
                        List.of("3", "300", "300")), res,
                "header row and 3 data rows with 2 columns");
    }

    @Test
    public void testDoubleFieldsToDoubleColumns() {
        var data = List.of(
                List.of("hello", "world"),
                List.of("100", "1000"),
                List.of("200", "2000"),
                List.of("300", "3000"));
        var d = PlotterDataBuilder.create()
                .selectHeader()
                    .lineTop()
                    .bindFieldIndexNameAsValue("hello") //field[0]="hello" named as "hello"
                    .bindFieldIndexNameAsValue("world") //field[1]="world" named as "world"
                .endSelect()
                .selectData()
                    .lineNotTop()
                    .takeFieldName("hello").value().fieldNameOrIndexAsColumnLabel().endTake() //field[0] as column "hello"
                    .takeFieldName("world").value().fieldNameOrIndexAsColumnLabel().endTake() //field[1] as column "world"
                .endSelect();
        var res = d.run(data);
        System.err.println(res);
        Assertions.assertEquals(
                List.of(List.of("", "hello", "world"), //header :
                        List.of("1", "100", "1000"),
                        List.of("2", "200", "2000"),
                        List.of("3", "300", "3000")), res,
                "header row and 3 data rows with 2 columns");
    }

    @Test
    public void testHeaderValueColumn() {
        var data = List.of(
                List.of("hello"),
                List.of("100"),
                List.of("200"),
                List.of("300"));
        var d = PlotterDataBuilder.create()
                .selectHeader()
                    .lineTop()
                    .bindFieldIndexContains("x", "ll") //field[0]="hello" named as "x"
                .endSelect()
                .selectData()
                    .lineNotTop()
                    .takeFieldName("x").value().headerValueAsColumnLabel().endTake() //field[0] "hello"
                .endSelect();
        var res = d.run(data);
        System.err.println(res);
        Assertions.assertEquals(
                List.of(List.of("", "hello"), //header :
                        List.of("1", "100"),
                        List.of("2", "200"),
                        List.of("3", "300")), res,
                "header row and 3 data rows");
    }

    @Test
    public void testLineFieldValueEquals() {
        var data = List.of(
                List.of("flag", "hello"),
                List.of("a", "100"),
                List.of("b", "200"),
                List.of("a", "300"));
        var d = PlotterDataBuilder.create()
                .selectHeader()
                    .lineTop()
                    .bindFieldIndexNameAsValue("flag")  //field[0]="flag"
                    .bindFieldIndexNameAsValue("hello") //field[1]="hello"
                .endSelect()
                .selectData()
                    .lineNotTop() //limits the data part
                    .lineFieldName("flag").valueEquals("a") //select field[0]=="a"
                    .lineFieldIndex(1).test(v -> Integer.parseInt(v.toString()) > 100) //and field[1]>100
                    .takeFieldName("hello").value().headerValueAsColumnLabel().endTake() //field[0] "hello"
                .endSelect();
        var res = d.run(data);
        System.err.println(res);
        Assertions.assertEquals(
                List.of(List.of("", "hello"), //header :
                        List.of("3", "300")), res,
                "header row and 1 data row, filtered by flag=a and hello>100");
    }


    @Test
    public void testFieldsToRows() {
        var data = List.of(
                List.of("hello", "world", "aaa", "bbb"),
                List.of("100", "200", "300", "400"));
        var d = PlotterDataBuilder.create()
                .selectHeader()
                    .lineTop()
                    .bindFieldIndexRangeToLast("data", 0)
                .endSelect()
                .selectData()
                    .lineNotTop()
                    .takeFieldName("data").value().columnLabel("X").fieldIndexAsRowLabel().endTake()
                .endSelect();
        var res = d.run(data);
        System.err.println(res);
        Assertions.assertEquals(
                List.of(List.of("", "X"), //header :
                        List.of("[0]", "100"),
                        List.of("[1]", "200"),
                        List.of("[2]", "300"),
                        List.of("[3]", "400")), res,
                "translate columns and rows");
    }


    @Test
    public void testUnifyFields() {
        var data = List.of(
                List.of("x", "y"),
                List.of("100", "1000"),
                List.of("200", "2000"),
                List.of("300", "3000"),
                List.of("400", "4000"));
        var d = PlotterDataBuilder.create()
                .selectHeader()
                    .lineTop()
                    .bindFieldIndexNameAsValue("x")
                    .bindFieldIndexNameAsValue("y")
                .endSelect()
                .selectData()
                    .lineNotTop()
                    .takeFieldName("x").value()
                        .lineIndexAsRowLabel().<Long>rowLabelConvert(i -> Long.toString(i * 2L - 1L))
                        .columnLabel("X")
                    .endTake()
                    .takeFieldName("y").value()
                        .lineIndexAsRowLabel().<Long>rowLabelConvert(i -> Long.toString(i * 2L))
                        .columnLabel("X")
                    .endTake()
                .endSelect();
        var res = d.run(data);
        System.err.println(res);
        Assertions.assertEquals(
                List.of(List.of("", "X"), //header :
                        List.of("1", "100"),
                        List.of("2", "1000"),
                        List.of("3", "200"),
                        List.of("4", "2000"),
                        List.of("5", "300"),
                        List.of("6", "3000"),
                        List.of("7", "400"),
                        List.of("8", "4000")), res,
                "serialize 2 fields to 1 column");
    }


    @Test
    public void testUnifyFieldsWithAutoLabel() {
        var data = List.of(
                List.of("x", "y"),
                List.of("100", "1000"),
                List.of("200", "2000"),
                List.of("300", "3000"),
                List.of("400", "4000"));
        var d = PlotterDataBuilder.create()
                .selectHeader()
                    .lineTop()
                    .bindFieldIndexNameAsValue("x")
                    .bindFieldIndexNameAsValue("y")
                .endSelect()
                .selectData()
                    .lineNotTop()
                    .takeFieldName("x").value()
                        .lineAndFieldIndicesKeyAsRowLabel() // line+"."+field
                        .columnLabel("X")
                    .endTake()
                    .takeFieldName("y").value()
                        .lineAndFieldIndicesKeyAsRowLabel()
                        .columnLabel("X")
                    .endTake()
                .endSelect();
        var res = d.run(data);
        System.err.println(res);
        Assertions.assertEquals(
                List.of(List.of("", "X"), //header :
                        List.of("1.[0]", "100"),
                        List.of("1.[1]", "1000"),
                        List.of("2.[0]", "200"),
                        List.of("2.[1]", "2000"),
                        List.of("3.[0]", "300"),
                        List.of("3.[1]", "3000"),
                        List.of("4.[0]", "400"),
                        List.of("4.[1]", "4000")), res,
                "serialize 2 fields to 1 column");
    }


    @Test
    public void testMultilayoutNoHeader() {
        var data = List.of(
                List.of("100", "1000"),
                List.of("200", "2000"),
                List.of("300", "3000"),
                List.of("400", "4000", "40"),
                List.of("500", "5000", "50"),
                List.of("600", "6000"));
        var d = PlotterDataBuilder.create()
                .selectHeader("2fields") //named header
                    .noLine() //no header
                    .bindFieldIndex("x", 0) //no header -> specify concrete fieldIndex
                    .bindFieldIndex("y", 1)
                .endSelect()
                .selectHeader("3fields") //another named header
                    .noLine()
                    .bindFieldIndex("x", 0)
                    .bindFieldIndex("y", 1)
                    .bindFieldIndex("z", 2)
                .endSelect()
                .selectData("2fields")
                    .lineFieldIndex(2).notExists() //column "z" is defined in the "3fields" header; refer it as a concrete-index, 2
                    .takeFieldName("x").value()
                        .columnLabel("X")
                    .endTake()
                .endSelect()
                .selectData("3fields")
                    .lineFieldName("z").exists() //z exists == it has field 2
                    .takeFieldName("z").value()
                        .columnLabel("X")
                    .endTake()
                .endSelect();
        var res = d.run(data);
        System.err.println(res);
        Assertions.assertEquals(
                List.of(List.of("", "X"), //header :
                        List.of("0", "100"), //no-header: starts from 0
                        List.of("1", "200"),
                        List.of("2", "300"),
                        List.of("3", "40"),
                        List.of("4", "50"),
                        List.of("5", "600")), res,
                "serialize 2 or 3 fields to 1 column");
    }

    @Test
    public void testFieldAsColumn() {
        var data = List.of(
                List.of("flag", "hello"),
                List.of("a", "100"),
                List.of("b", "200"),
                List.of("a", "300"),
                List.of("b", "400"),
                List.of("b", "500"));
        var d = PlotterDataBuilder.create()
                .selectHeader()
                    .lineTop()
                    .bindFieldIndexNameAsValue("flag")  //field[0]="flag"
                    .bindFieldIndexNameAsValue("hello") //field[1]="hello"
                .endSelect()
                .selectData()
                    .lineNotTop() //limits the data part
                    .takeFieldName("hello").value()
                    .endTake() //field[0] "hello"
                    .takeFieldName("flag").valueOccurrences().asRowLabel().endTake()
                    .takeFieldName("flag").asColumnLabel().endTake()
                .endSelect();
        var res = d.run(data);
        System.err.println(res);
        Assertions.assertEquals(
                List.of(List.of("flag", "a", "b"), //header :
                        List.of("0", "100", "200"),
                        List.of("1", "300", "400"),
                        List.of("2", 0.0, "500")), res,
                "");
    }


    @Test
    public void testFieldGroupAsColumn() {
        var data = List.of(
                List.of("flag1", "flag2", "hello", "world"),
                List.of("a", "x", "100", "10"),
                List.of("b", "y", "200", "20"),
                List.of("a", "x", "300", "30"),
                List.of("b", "y", "400", "40"),
                List.of("b", "x", "500", "50"));
        var d = PlotterDataBuilder.create()
                .selectHeader()
                    .lineTop()
                    .bindFieldIndexRange("flag", 0, 2)
                    .bindFieldIndexRangeToLast("hello", 2)
                .endSelect()
                .selectData()
                    .lineNotTop()
                    .takeFieldNameAsList("hello").value()
                    .endTake()
                    .takeFieldNameAsList("flag").valueOccurrences().asRowLabel().endTake()
                    .takeFieldNameAsList("flag").asColumnLabel().endTake()
                .endSelect();
        var res = d.run(data);
        System.err.println(res);
        Assertions.assertEquals(
                List.of(List.of("flag", "[a, x]", "[b, y]", "[b, x]"), //header :
                        List.of("0", List.of("100", "10"), List.of("200", "20"), List.of("500", "50")),
                        List.of("1", List.of("300", "30"), List.of("400", "40"), 0.0)), res,
                "");
    }


    @Test
    public void takeFieldAsRowAndColumnLabels() {
        var data = List.of(
                List.of("flag1", "flag2", "hello", "world"),
                List.of("a", "x", "100", "10"),
                List.of("b", "y", "200", "20"),
                List.of("a", "x", "300", "30"),
                List.of("b", "y", "400", "40"),
                List.of("b", "x", "500", "50"));
        var d = PlotterDataBuilder.create()
                .selectHeader()
                    .lineTop()
                    .bindFieldIndex("flag1", 0)
                    .bindFieldIndex("flag2", 1)
                    .bindFieldIndex("hello", 2)
                    .bindFieldIndex("world", 3)
                .endSelect()
                .selectData()
                    .lineNotTop()
                    .takeFieldName("hello").value().fieldNameOrIndexAsColumnLabel().endTake()
                    .takeFieldName("world").value().fieldNameOrIndexAsColumnLabel().endTake()
                    .takeFieldName("flag1").asRowLabel().endTake()
                    .takeFieldName("flag2").asColumnLabel().labelCombine((l, v) -> l + "." + v).endTake()
                .endSelect();
        var res = d.run(data);
        System.err.println(res);
        Assertions.assertEquals(
                List.of(
                        List.of("flag1", "hello.x", "world.x", "hello.y", "world.y"),
                        List.of("a", "300", "30", 0.0, 0.0),   //later value will overwrite existing cell values
                        List.of("b", "500", "50", "400", "40")
                ), res,
                "");
    }

    @Test
    public void testReducer() {
        var data = List.<List<Object>>of(
                List.of("date", "price"),
                List.of("2000-01-01", "10"),
                List.of("2000-01-02", "20"),
                List.of("2000-01-03", "30"),
                List.of("2000-01-04", "40"),
                List.of("2000-01-05", "50"),
                List.of("2000-01-06", "60"),
                List.of("2000-01-07", "70"),
                List.of("2000-01-08", "80"), //reduce [10, 20], [30, 40], [50, 60], [70, 80] -> [15:2], [35:2], [55:2], [75:2]
                List.of("2000-01-09", "90"),
                List.of("2000-01-10", "100"),
                List.of("2000-01-11", "110"),
                List.of("2000-01-12", "120"),
                List.of("2000-01-13", "130")  //reduce [15:2, 35:2] , [55:2, 75:2], [90, 100], [110, 120] -> [7.5+17.5=25:4], [27.5+37.5=65:4], [95:2], [115:2]
        );

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        var d = PlotterDataBuilder.create()
                .selectHeader()
                    .lineTop()
                    .bindFieldIndexContains("date", "date")
                    .bindFieldIndexContains("price", "price")
                .endSelect()
                .selectData()
                    .lineNotTop()
                    .takeFieldName("date").<String>asRowLabel(s -> LocalDate.parse(s, fmt)).endTake()
                    .takeFieldName("price").value(Double::parseDouble).fieldNameOrIndexAsColumnLabel().endTake()
                .endSelect()
                .withReduce(4, 8);
        var res = d.run(data);
        System.err.println(res);

        Assertions.assertEquals(6, res.size(), "rowSize");
        Assertions.assertEquals(3, res.getColumnSize(), "columnSize");

        Assertions.assertEquals(List.of("date", "price", "#RowCount"), res.getFirst(), "header");

        var r = res.get(1);
        Assertions.assertEquals(LocalDate.of(2000,1,1).toString(), r.get(0), "date");
        Assertions.assertEquals( 25.0, r.get(1), "price");
        Assertions.assertEquals(4L, r.get(2), "count");
        r = res.get(2);
        Assertions.assertEquals(LocalDate.of(2000,1,5).toString(), r.get(0), "date");
        Assertions.assertEquals( 65.0, r.get(1), "price");
        Assertions.assertEquals(4L, r.get(2), "count");
        r = res.get(3);
        Assertions.assertEquals(LocalDate.of(2000,1,9).toString(), r.get(0), "date");
        Assertions.assertEquals( 95.0, r.get(1), "price");
        Assertions.assertEquals(2L, r.get(2), "count");
        r = res.get(4);
        Assertions.assertEquals(LocalDate.of(2000,1,11).toString(), r.get(0), "date");
        Assertions.assertEquals( 115.0, r.get(1), "price");
        Assertions.assertEquals(2L, r.get(2), "count");
        r = res.get(5);
        Assertions.assertEquals(LocalDate.of(2000,1,13).toString(), r.get(0), "date");
        Assertions.assertEquals( 130.0, r.get(1), "price");
        Assertions.assertEquals(1L, r.get(2), "count");

        var resNoCount = res.filteredColumns(PlotterDataReducer.excludeRowCountColumnByHeaderName(res));
        System.err.println(resNoCount);

        Assertions.assertEquals(6, resNoCount.size(), "rowSize");
        Assertions.assertEquals(2, resNoCount.getFirst().size(), "columnSize");

        Assertions.assertEquals(List.of("date", "price"), resNoCount.getFirst(), "header");

        r = resNoCount.get(1);
        Assertions.assertEquals(LocalDate.of(2000,1,1).toString(), r.get(0), "date");
        Assertions.assertEquals( 25.0, r.get(1), "price");
        r = resNoCount.get(2);
        Assertions.assertEquals(LocalDate.of(2000,1,5).toString(), r.get(0), "date");
        Assertions.assertEquals( 65.0, r.get(1), "price");
        r = resNoCount.get(3);
        Assertions.assertEquals(LocalDate.of(2000,1,9).toString(), r.get(0), "date");
        Assertions.assertEquals( 95.0, r.get(1), "price");
        r = resNoCount.get(4);
        Assertions.assertEquals(LocalDate.of(2000,1,11).toString(), r.get(0), "date");
        Assertions.assertEquals( 115.0, r.get(1), "price");
        r = resNoCount.get(5);
        Assertions.assertEquals(LocalDate.of(2000,1,13).toString(), r.get(0), "date");
        Assertions.assertEquals( 130.0, r.get(1), "price");
    }
}
