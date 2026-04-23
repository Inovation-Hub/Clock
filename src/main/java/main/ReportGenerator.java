package main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ReportGenerator {

    private static final DateTimeFormatter RECORD_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FMT    = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public File generate(List<TimeRecord> allRecords, LocalDate from, LocalDate to) throws IOException {
        List<TimeRecord> filtered = allRecords.stream()
            .filter(r -> {
                try {
                    LocalDate d = LocalDate.parse(r.timestamp.substring(0, 10), DATE_FMT);
                    return !d.isBefore(from) && !d.isAfter(to);
                } catch (Exception e) {
                    return false;
                }
            })
            .collect(Collectors.toList());

        long totalSeconds = filtered.stream().mapToLong(r -> r.totalSeconds).sum();

        File file = File.createTempFile("relatorio_horas_", ".html");
        try (FileWriter w = new FileWriter(file)) {
            w.write(buildHtml(filtered, from, to, totalSeconds));
        }
        return file;
    }

    private String buildHtml(List<TimeRecord> records, LocalDate from, LocalDate to, long totalSeconds) {
        String fromStr = from.format(DATE_FMT);
        String toStr   = to.format(DATE_FMT);
        String total   = formatTime(totalSeconds);

        StringBuilder rows = new StringBuilder();
        for (TimeRecord r : records) {
            rows.append("      <tr>\n")
                .append("        <td>").append(escape(r.timestamp)).append("</td>\n")
                .append("        <td class=\"duration\">").append(escape(r.duration)).append("</td>\n")
                .append("        <td>").append(escape(r.description)).append("</td>\n")
                .append("      </tr>\n");
        }

        return "<!DOCTYPE html>\n"
            + "<html lang=\"pt-BR\">\n"
            + "<head>\n"
            + "  <meta charset=\"UTF-8\">\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "  <title>Relatório de Horas — " + fromStr + " a " + toStr + "</title>\n"
            + "  <style>\n"
            + "    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }\n"
            + "    body { font-family: 'Segoe UI', system-ui, sans-serif; background: #0f0f1a; color: #e4e4f0; min-height: 100vh; padding: 40px 24px; }\n"
            + "    .container { max-width: 900px; margin: 0 auto; }\n"
            + "    header { margin-bottom: 36px; }\n"
            + "    header h1 { font-size: 24px; font-weight: 700; color: #fff; }\n"
            + "    header p  { font-size: 14px; color: #7878a0; margin-top: 6px; }\n"
            + "    .summary { display: flex; gap: 16px; margin-bottom: 32px; flex-wrap: wrap; }\n"
            + "    .card { background: #1c1c2d; border: 1px solid #2d2d46; border-radius: 10px; padding: 20px 24px; flex: 1; min-width: 160px; }\n"
            + "    .card .label { font-size: 11px; font-weight: 700; color: #7878a0; text-transform: uppercase; letter-spacing: .08em; margin-bottom: 8px; }\n"
            + "    .card .value { font-size: 28px; font-weight: 700; font-family: 'Consolas', monospace; color: #34d399; }\n"
            + "    .card .value.blue  { color: #60a5fa; }\n"
            + "    .card .value.amber { color: #fbbf24; }\n"
            + "    table { width: 100%; border-collapse: collapse; background: #1c1c2d; border: 1px solid #2d2d46; border-radius: 10px; overflow: hidden; }\n"
            + "    thead tr { background: #26263f; }\n"
            + "    th { padding: 12px 16px; text-align: left; font-size: 11px; font-weight: 700; color: #7878a0; text-transform: uppercase; letter-spacing: .08em; border-bottom: 1px solid #2d2d46; }\n"
            + "    td { padding: 12px 16px; font-size: 13px; border-bottom: 1px solid #1e1e32; vertical-align: top; }\n"
            + "    tbody tr:last-child td { border-bottom: none; }\n"
            + "    tbody tr:hover td { background: #22223a; }\n"
            + "    td.duration { font-family: 'Consolas', monospace; color: #34d399; white-space: nowrap; }\n"
            + "    .empty { text-align: center; color: #7878a0; padding: 48px 0; font-size: 14px; }\n"
            + "    footer { margin-top: 32px; font-size: 11px; color: #50506a; text-align: right; }\n"
            + "  </style>\n"
            + "</head>\n"
            + "<body>\n"
            + "  <div class=\"container\">\n"
            + "    <header>\n"
            + "      <h1>Relatório de Horas</h1>\n"
            + "      <p>Período: " + fromStr + " &nbsp;→&nbsp; " + toStr + "</p>\n"
            + "    </header>\n"
            + "    <div class=\"summary\">\n"
            + "      <div class=\"card\"><div class=\"label\">Total de horas</div><div class=\"value\">" + total + "</div></div>\n"
            + "      <div class=\"card\"><div class=\"label\">Registros</div><div class=\"value blue\">" + records.size() + "</div></div>\n"
            + "      <div class=\"card\"><div class=\"label\">Média por registro</div><div class=\"value amber\">"
                + (records.isEmpty() ? "00:00:00" : formatTime(totalSeconds / records.size()))
                + "</div></div>\n"
            + "    </div>\n"
            + "    <table>\n"
            + "      <thead><tr><th>Data/Hora</th><th>Duração</th><th>Descrição</th></tr></thead>\n"
            + "      <tbody>\n"
            + (records.isEmpty()
                ? "        <tr><td colspan=\"3\" class=\"empty\">Nenhum registro encontrado para o período selecionado.</td></tr>\n"
                : rows.toString())
            + "      </tbody>\n"
            + "    </table>\n"
            + "    <footer>Gerado em " + LocalDate.now().format(DATE_FMT) + " · Clock — Contabilização de Horas</footer>\n"
            + "  </div>\n"
            + "</body>\n"
            + "</html>\n";
    }

    private static String formatTime(long totalSeconds) {
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
