package com.blzcat.extend.util;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.Data;

@Data
public class PrintTable {
    private Table table;
    // 最大列宽：sql查询结果某列内容可能过大，不想完全显示，因此限制最大列宽
    private Integer maxWidth;
    // 最大条数:sql查询结果可能有非常多，通常不必完全显示，因此限制最大条数
    private Integer maxLength;

    public PrintTable(ResultSet rs) throws SQLException {
        List<List<String>> content = initData(rs);
        this.table = buildTable(content);
        this.maxLength = 10;
        this.maxWidth = 40;
    }

    private List<List<String>> initData(ResultSet rs) throws SQLException {
        List<List<String>> content = new ArrayList<>();

        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        ArrayList<String> header = new ArrayList<>();
        for (int i = 0; i < columnCount; i++) {
            header.add(metaData.getColumnName(i + 1));
        }
        content.add(header);

        ArrayList<String> data = new ArrayList<>();
        while (rs.next()) {
            for (int i = 0; i < columnCount; i++) {
                data.add(rs.getString(i + 1));
            }
        }
        content.add(data);
        return content;
    }

    /**
     * 创建Table实例
     *
     * @param content 内容
     * @return 表
     */

    private Table buildTable(List<List<String>> content) {
        return new Table(content);
    }

    /**
     * 打印表格
     */
    public void printTable(String... symbols) {
        String symbol = symbols.length == 0 ? "|" : symbols[0];
        // 按照最大列宽、最大数据量过滤后的表格
        Table limitTable = getLimitTable();
        // 设置表格的最大宽度：得到每列宽度，再求和
        List<Integer> originMaxWidthList = getMaxWidthLenList(limitTable);
        limitTable.setMaxWidthList(originMaxWidthList);

        // 得到格式化后的表格数据
        Table formatTable = getFormatTable(limitTable, symbol);
        Integer totalColSize = formatTable.getTotalColSize();
        // 打印首行分割符号
        System.out.println(StringUtils.getRepeatChar("-", totalColSize));
        formatTable.getContent().forEach(row -> {
            row.forEach(System.out::print);
            System.out.println();
            // 打印每行分割符号
            System.out.println(StringUtils.getRepeatChar("-", totalColSize));
        });
    }


    /**
     * 格式化表格
     *
     * @param symbol 定义每列间隔符号
     * @return 表
     */
    private Table getFormatTable(Table table, String symbol) {
        // 获取原表每列最大宽度
        List<Integer> originMaxWidthList = table.getMaxWidthList();
        // 除了间隔符号外，固定在每个单元格前后加两个空格
        int symbolLen = symbol.length() + 2;
        // 遍历原table，将每个单元格填充到该列最大长度
        List<List<String>> formatList = table.getContent().stream().map(row -> {
            // 用于流在遍历每行的过程中，获取列序号
            AtomicInteger atomicInteger = new AtomicInteger(0);
            return row.stream().map(cell -> {
                // 当前遍历的列序号
                int j = atomicInteger.getAndIncrement();
                // 原表该列的最大宽度+间隔符号宽度-双字节出现的次数
                int cellSize =
                    originMaxWidthList.get(j) + symbolLen - StringUtils.getZHCharCount(cell);
                // 如果是首行，还需要再前面加一个分割符号|，故长度加1
                cellSize = j == 0 ? cellSize + 1 : cellSize;
                // 返回原始字符串按照指定symbol填充到指定长度cellSize，并居中对齐的字符
                return StringUtils.getPadString(cell, cellSize, symbol, j);
            }).collect(Collectors.toList());
        }).collect(Collectors.toList());
        // 存储格式化后的表格数据
        Table formatTable = buildTable(formatList);
        // 设置格式化表格的总宽度：原始宽度+自定义分割符号的总宽度（列数*符号宽度）+首列前面的符号宽度
        int totalColSize = table.getTotalColSize() + table.getColCount() * symbolLen + 1;
        formatTable.setTotalColSize(totalColSize);
        return formatTable;
    }

    /**
     * @return 获取经过条件过滤的表格
     */
    private Table getLimitTable() {
        List<List<String>> limitContent =
            table.getContent().stream().limit(maxLength).map(row -> row.stream()
                // 去除内容中含制表符时对结果展示的影响
                .map(cell -> cell == null ? null : cell.replaceAll("\t", " ")).map(cell -> cell != null && cell.length() > maxWidth ? cell.substring(0, maxWidth) : cell).collect(Collectors.toList())).collect(Collectors.toList());
        return buildTable(limitContent);
    }

    /**
     * 计算table每行的最大宽度
     * 要使列宽相等，就需要将每个单元格宽度设置为该列最大宽度，二计算每行最大宽度相对容易些
     * 故将content转置后得到的每行最大宽度即为所求
     * 需要考虑单双字节的情况，比如有数组arr:{"aabb","sql表格","编程学习"},
     * 按照String.length计算，arr[1]最长，但是实际上arr[2]看起来才是最宽的
     * 因此计算宽度时，将双字节字符看做2个单位长度，即：每出现一个双字节字符，长度+1
     *
     * @return 每行最大宽度
     */
    private List<Integer> getMaxWidthLenList(Table table) {
        // 得到转置数组每个元素的长度,一个中文算两个长度
        return Arrays.stream(table.transpose()).map(rows -> Arrays.stream(rows).mapToInt(s -> {
            // sql查询结果如果为null，则认为长度为4
            if (s == null) {
                return 4;
            } else {
                // 加上双字节字符出现的次数，最短为null，四个字符
                return s.length() + StringUtils.getZHCharCount(s);
            }
        }).max().orElse(0)).collect(Collectors.toList());
    }

    @Data
    private static class Table {
        /**
         * 表格内容（含表头）
         */
        private List<List<String>> content;

        /**
         * 表格列总字符长度：便于打印行分割符号
         */
        private Integer totalColSize;
        /**
         * 每列最大宽度
         */
        private List<Integer> maxWidthList;

        Integer getTotalColSize() {
            if (totalColSize == null && maxWidthList != null && !maxWidthList.isEmpty()) {
                this.totalColSize = maxWidthList.stream().reduce(Integer::sum).get();
            }
            return totalColSize;
        }

        // private限制只能通过外部类构造
        private Table(List<List<String>> content) {
            this.content = content;
        }

        // 获取表格行数
        int getRowCount() {
            return content.size();
        }

        // 获取表格列数，0行代表表头，默认认为content中至少含有表头
        int getColCount() {
            return content.get(0).size();
        }

        /**
         * 转置二维数组
         *
         * @return 数组
         */
        private String[][] transpose() {
            int rowCount = getRowCount();
            int colCount = getColCount();
            String[][] result = new String[colCount][rowCount];

            for (int i = 0; i < rowCount; i++) {
                for (int j = 0; j < colCount; j++) {
                    result[j][i] = content.get(i).get(j);
                }
            }
            return result;
        }
    }

    public static class StringUtils {

        /**
         * 将str重复count次，返回结果
         *
         * @param str   字符
         * @param count 重复次数
         * @return 结果
         */
        public static String getRepeatChar(String str, int count) {
            StringBuilder res = new StringBuilder();
            IntStream.range(0, count).forEach(i -> res.append(str));
            return res.toString();
        }

        /**
         * 将字符串填充到指定长度并居中对齐
         *
         * @param str 字符
         * @param len 指定长度
         * @return 居中填充后的字符串
         */
        public static String getPadString(String str, Integer len) {
            StringBuilder res = new StringBuilder();
            str = str.trim();
            if (str.length() < len) {
                int diff = len - str.length();
                int fixLen = diff / 2;
                String fix = getRepeatChar(" ", fixLen);
                res.append(fix).append(str).append(fix);
                if (res.length() > len) {
                    return res.substring(0, len);
                } else {
                    res.append(getRepeatChar(" ", len - res.length()));
                    return res.toString();
                }
            }
            return str.substring(0, len);
        }

        /**
         * 此方法主要为表格的单元格数据按照指定长度填充并居中对齐并带上分割符号
         *
         * @param str    原始字符串
         * @param len    输出字符串的总长度
         * @param symbol 分割符号
         * @param index  传入的cell在list的索引，如果为第一个则需要在前面增加分割符号
         * @return 填充后的字符串
         */
        public static String getPadString(String str, Integer len, String symbol, int index) {
            String origin = str + "  ";
            if (index == 0) {
                String tmp = getPadString(origin, len - 2);
                return symbol + tmp + symbol;
            } else {
                String tmp = getPadString(origin, len - 1);
                return tmp + symbol;
            }
        }

        /**
         * 得到一个字符串中单字节出现的次数
         *
         * @param cell 字符
         * @return 单字节出现的次数
         */
        public static Integer getENCharCount(String cell) {
            if (cell == null) {
                return 0;
            }
            String reg = "[^\t\\x00-\\xff]";
            cell = cell.replaceAll(reg, "");
            // 把·当做中文字符两个宽度
            return cell.replaceAll("·", "").length();
        }

        /**
         * 得到一个字符串中双字节出现的次数
         *
         * @param cell 字符
         * @return 双字节出现的次数
         */
        public static Integer getZHCharCount(String cell) {
            if (cell == null) {
                return 0;
            }
            return cell.length() - getENCharCount(cell);
        }
    }

}
