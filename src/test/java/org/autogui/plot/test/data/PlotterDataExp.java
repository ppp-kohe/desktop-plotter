package org.autogui.plot.test.data;

import org.autogui.plot.data.PlotterData;
import org.autogui.plot.data.PlotterDataBuilder;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PlotterDataExp {
    public static void main(String[] args) throws IOException {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
        var b = PlotterDataBuilder.create()
                .selectHeader()
                .lineTop()
                .bindFieldIndexContains("date", "年月日")
                .bindFieldIndexContains("price", "基準価額")
                .endSelect()
                .selectData()
                .lineNotTop()
                .takeFieldName("date").<String>convert(d -> LocalDate.parse(d, fmt)).asRowLabel()
                .takeFieldName("price").convert(Double::parseDouble).value().fieldNameOrIndexAsColumnLabel().endTake()
                .endSelect();

        try (var sel = Files.lines(Paths.get(args[0]), Charset.forName("SJIS"))) {
            var table = sel.map(l -> List.of(l.split(",")))
                    .toList();
            var res = b.run(table);
            System.out.println(res);
        }
    }
}
