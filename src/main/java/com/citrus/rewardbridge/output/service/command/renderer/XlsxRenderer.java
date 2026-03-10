package com.citrus.rewardbridge.output.service.command.renderer;

import com.citrus.rewardbridge.common.exception.BusinessException;
import com.citrus.rewardbridge.output.dto.OutputFormat;
import com.citrus.rewardbridge.output.dto.OutputRenderCommand;
import com.citrus.rewardbridge.output.dto.RenderedFile;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class XlsxRenderer implements OutputRenderer {

    private static final Pattern MARKDOWN_SEPARATOR_CELL = Pattern.compile(":?-{3,}:?");

    @Override
    public boolean supports(OutputFormat outputFormat) {
        return outputFormat == OutputFormat.XLSX;
    }

    @Override
    public RenderedFile render(OutputRenderCommand command) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            MarkdownTable markdownTable = parseMarkdownTable(command.businessResponse().getResponse());
            if (markdownTable != null) {
                renderMarkdownTable(workbook, command, markdownTable);
            } else {
                renderPlainLines(workbook, command);
            }

            workbook.write(outputStream);
            return new RenderedFile(
                    buildFileName(command, "xlsx"),
                    OutputFormat.XLSX.contentType(),
                    outputStream.toByteArray()
            );
        } catch (IOException ex) {
            throw new BusinessException(
                    "XLSX_RENDER_FAILED",
                    "Failed to render XLSX output: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private void renderPlainLines(XSSFWorkbook workbook, OutputRenderCommand command) {
        XSSFSheet sheet = workbook.createSheet("consult");

        Row titleRow = sheet.createRow(0);
        titleRow.createCell(0).setCellValue(buildTitle(command));

        Row headerRow = sheet.createRow(1);
        headerRow.createCell(0).setCellValue("Line");
        headerRow.createCell(1).setCellValue("Content");

        List<String> lines = splitLines(command);
        for (int index = 0; index < lines.size(); index++) {
            Row row = sheet.createRow(index + 2);
            row.createCell(0).setCellValue(index + 1);
            row.createCell(1).setCellValue(lines.get(index));
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void renderMarkdownTable(XSSFWorkbook workbook, OutputRenderCommand command, MarkdownTable markdownTable) {
        XSSFSheet casesSheet = workbook.createSheet("cases");

        Row titleRow = casesSheet.createRow(0);
        titleRow.createCell(0).setCellValue(buildTitle(command));

        Row headerRow = casesSheet.createRow(1);
        for (int column = 0; column < markdownTable.headers().size(); column++) {
            headerRow.createCell(column).setCellValue(markdownTable.headers().get(column));
        }

        for (int rowIndex = 0; rowIndex < markdownTable.rows().size(); rowIndex++) {
            Row row = casesSheet.createRow(rowIndex + 2);
            List<String> values = markdownTable.rows().get(rowIndex);
            for (int column = 0; column < values.size(); column++) {
                row.createCell(column).setCellValue(values.get(column));
            }
        }

        for (int column = 0; column < markdownTable.headers().size(); column++) {
            casesSheet.autoSizeColumn(column);
        }

        if (!markdownTable.summaryLines().isEmpty()) {
            XSSFSheet summarySheet = workbook.createSheet("summary");
            Row summaryTitleRow = summarySheet.createRow(0);
            summaryTitleRow.createCell(0).setCellValue(buildTitle(command));

            for (int index = 0; index < markdownTable.summaryLines().size(); index++) {
                Row row = summarySheet.createRow(index + 1);
                row.createCell(0).setCellValue(markdownTable.summaryLines().get(index));
            }
            summarySheet.autoSizeColumn(0);
        }
    }

    private MarkdownTable parseMarkdownTable(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }

        List<String> lines = response.lines().toList();
        for (int index = 0; index < lines.size() - 1; index++) {
            if (!looksLikeMarkdownRow(lines.get(index)) || !isSeparatorRow(lines.get(index + 1))) {
                continue;
            }

            List<String> headers = splitMarkdownRow(lines.get(index));
            if (headers.isEmpty()) {
                continue;
            }

            int endIndex = index + 2;
            List<List<String>> rows = new ArrayList<>();
            while (endIndex < lines.size() && looksLikeMarkdownRow(lines.get(endIndex))) {
                List<String> values = splitMarkdownRow(lines.get(endIndex));
                if (values.size() == headers.size()) {
                    rows.add(values);
                }
                endIndex++;
            }

            if (rows.isEmpty()) {
                return null;
            }

            List<String> summaryLines = new ArrayList<>();
            for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                if (lineIndex >= index && lineIndex < endIndex) {
                    continue;
                }
                String trimmed = lines.get(lineIndex).trim();
                if (!trimmed.isEmpty()) {
                    summaryLines.add(trimmed);
                }
            }
            return new MarkdownTable(headers, rows, summaryLines);
        }

        return null;
    }

    private boolean looksLikeMarkdownRow(String line) {
        String trimmed = line == null ? "" : line.trim();
        return trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.chars().filter(ch -> ch == '|').count() >= 2;
    }

    private boolean isSeparatorRow(String line) {
        List<String> cells = splitMarkdownRow(line);
        return !cells.isEmpty() && cells.stream().allMatch(cell -> MARKDOWN_SEPARATOR_CELL.matcher(cell).matches());
    }

    private List<String> splitMarkdownRow(String line) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.startsWith("|")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("|")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.isBlank()) {
            return List.of();
        }

        String[] tokens = trimmed.split("\\|", -1);
        List<String> cells = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            cells.add(token.trim());
        }
        return cells;
    }

    private record MarkdownTable(
            List<String> headers,
            List<List<String>> rows,
            List<String> summaryLines
    ) {
    }
}
