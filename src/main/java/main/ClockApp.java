package main;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class ClockApp extends JFrame {

    private final JLabel timerLabel;
    private final JTextArea descriptionArea;
    private final JButton startButton;
    private final JButton pauseButton;
    private final JButton stopButton;
    private final JButton reportButton;
    private final JTable historyTable;
    private final DefaultTableModel tableModel;

    private final RecordRepository repository = new RecordRepository();

    private Timer swingTimer;
    private long elapsedSeconds = 0;
    private boolean running = false;

    private static final Color BG_DARK      = new Color(18, 18, 30);
    private static final Color BG_CARD      = new Color(28, 28, 45);
    private static final Color ACCENT_GREEN = new Color(52, 211, 153);
    private static final Color ACCENT_AMBER = new Color(251, 191, 36);
    private static final Color ACCENT_RED   = new Color(248, 113, 113);
    private static final Color TEXT_PRIMARY = new Color(236, 236, 245);
    private static final Color TEXT_MUTED   = new Color(120, 120, 160);
    private static final Color BORDER_COLOR = new Color(45, 45, 70);

    public ClockApp() {
        setTitle("Clock — Contabilização de Horas");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(680, 660));
        setSize(780, 720);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { onClose(); }
        });

        setLayout(new BorderLayout(0, 0));

        // ── Header ──────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_CARD);
        header.setBorder(new EmptyBorder(18, 28, 18, 28));

        JLabel title = new JLabel("Contabilização de Horas");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Registre suas atividades com precisão");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_MUTED);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(subtitle);
        header.add(titleBlock, BorderLayout.WEST);

        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setForeground(BORDER_COLOR);
        sep.setBackground(BORDER_COLOR);

        JPanel headerWrapper = new JPanel(new BorderLayout());
        headerWrapper.setOpaque(false);
        headerWrapper.add(header, BorderLayout.CENTER);
        headerWrapper.add(sep, BorderLayout.SOUTH);
        add(headerWrapper, BorderLayout.NORTH);

        // ── Timer section (centro da janela) ─────────────────────────
        JPanel timerSection = new JPanel(new GridBagLayout());
        timerSection.setBackground(BG_CARD);
        timerSection.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
            new EmptyBorder(20, 28, 20, 28)
        ));

        // Contador principal
        timerLabel = new JLabel("00:00:00", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Consolas", Font.BOLD, 96));
        timerLabel.setForeground(TEXT_MUTED);

        JLabel timerCaption = new JLabel("horas  minutos  segundos", SwingConstants.CENTER);
        timerCaption.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        timerCaption.setForeground(TEXT_MUTED);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 4, 0);
        timerSection.add(timerLabel, gbc);

        gbc.gridy = 1; gbc.insets = new Insets(0, 0, 0, 0);
        timerSection.add(timerCaption, gbc);

        // ── Controles + descrição ─────────────────────────────────────
        JPanel controlSection = new JPanel(new BorderLayout(0, 16));
        controlSection.setOpaque(false);
        controlSection.setBorder(new EmptyBorder(14, 28, 14, 28));

        // Description
        JPanel descCard = buildCard();
        descCard.setLayout(new BorderLayout(0, 8));

        JLabel descLabel = new JLabel("Descrição da atividade *");
        descLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        descLabel.setForeground(TEXT_PRIMARY);

        JLabel descHint = new JLabel("Obrigatório para encerrar a tarefa");
        descHint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        descHint.setForeground(TEXT_MUTED);

        JPanel descHeader = new JPanel(new BorderLayout(8, 0));
        descHeader.setOpaque(false);
        descHeader.add(descLabel, BorderLayout.WEST);
        descHeader.add(descHint, BorderLayout.EAST);

        descriptionArea = new JTextArea(3, 40);
        descriptionArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descriptionArea.setBackground(new Color(38, 38, 58));
        descriptionArea.setForeground(TEXT_PRIMARY);
        descriptionArea.setCaretColor(ACCENT_GREEN);
        descriptionArea.setBorder(new EmptyBorder(10, 12, 10, 12));
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        descScroll.getViewport().setBackground(new Color(38, 38, 58));

        descCard.add(descHeader, BorderLayout.NORTH);
        descCard.add(descScroll, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnPanel.setOpaque(false);

        startButton  = buildButton("▶  Iniciar",   ACCENT_GREEN,            BG_DARK);
        pauseButton  = buildButton("⏸  Pausar",    ACCENT_AMBER,            BG_DARK);
        stopButton   = buildButton("⏹  Encerrar",  ACCENT_RED,              BG_DARK);
        reportButton = buildButton("📄  Relatório", new Color(139, 92, 246), BG_DARK);

        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);

        btnPanel.add(startButton);
        btnPanel.add(pauseButton);
        btnPanel.add(stopButton);
        btnPanel.add(reportButton);

        controlSection.add(descCard,  BorderLayout.CENTER);
        controlSection.add(btnPanel,  BorderLayout.SOUTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(timerSection,   BorderLayout.NORTH);
        centerPanel.add(controlSection, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // ── History panel ─────────────────────────────────────────────
        String[] columns = {"Data/Hora", "Duração", "Descrição"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        historyTable = new JTable(tableModel);
        historyTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        historyTable.setForeground(TEXT_PRIMARY);
        historyTable.setBackground(BG_CARD);
        historyTable.setGridColor(BORDER_COLOR);
        historyTable.setRowHeight(26);
        historyTable.setSelectionBackground(new Color(52, 52, 80));
        historyTable.setSelectionForeground(TEXT_PRIMARY);
        historyTable.getTableHeader().setBackground(new Color(38, 38, 58));
        historyTable.getTableHeader().setForeground(TEXT_MUTED);
        historyTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        historyTable.getTableHeader().setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(400);

        JScrollPane tableScroll = new JScrollPane(historyTable);
        tableScroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));
        tableScroll.setBackground(BG_DARK);
        tableScroll.getViewport().setBackground(BG_CARD);
        tableScroll.setPreferredSize(new Dimension(0, 160));

        JPanel historyHeader = new JPanel(new BorderLayout());
        historyHeader.setBackground(new Color(22, 22, 38));
        historyHeader.setBorder(new EmptyBorder(8, 28, 8, 28));
        JLabel histLabel = new JLabel("Histórico completo (carregado do XML)");
        histLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        histLabel.setForeground(TEXT_MUTED);
        historyHeader.add(histLabel, BorderLayout.WEST);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.add(historyHeader, BorderLayout.NORTH);
        bottomPanel.add(tableScroll, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // ── Listeners ─────────────────────────────────────────────────
        startButton.addActionListener(e -> onStart());
        pauseButton.addActionListener(e -> onPause());
        stopButton.addActionListener(e -> onStop());
        reportButton.addActionListener(e -> onReport());

        // ── Carrega registros salvos ───────────────────────────────────
        loadHistory();

        // ── Swing Timer (1 s tick) ─────────────────────────────────────
        swingTimer = new Timer(1000, e -> {
            elapsedSeconds++;
            timerLabel.setText(formatTime(elapsedSeconds));
        });

    }

    // ── Actions ─────────────────────────────────────────────────────────

    private void onStart() {
        if (!running) {
            running = true;
            swingTimer.start();
            startButton.setEnabled(false);
            pauseButton.setEnabled(true);
            stopButton.setEnabled(true);
            timerLabel.setForeground(ACCENT_GREEN);
        }
    }

    private void onPause() {
        if (running) {
            running = false;
            swingTimer.stop();
            startButton.setText("▶  Retomar");
            startButton.setEnabled(true);
            pauseButton.setEnabled(false);
            timerLabel.setForeground(ACCENT_AMBER);
        }
    }

    private void onStop() {
        String desc = descriptionArea.getText().trim();
        if (desc.isEmpty()) {
            JOptionPane optionPane = new JOptionPane(
                "Preencha a descrição da atividade antes de encerrar.",
                JOptionPane.WARNING_MESSAGE
            );
            JDialog dialog = optionPane.createDialog(this, "Descrição obrigatória");
            styleDialog(dialog);
            dialog.setVisible(true);
            descriptionArea.requestFocusInWindow();
            return;
        }

        swingTimer.stop();
        running = false;

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String duration  = formatTime(elapsedSeconds);

        TimeRecord record = new TimeRecord(timestamp, duration, elapsedSeconds, desc);
        repository.append(record);
        tableModel.insertRow(0, new Object[]{timestamp, duration, desc});

        elapsedSeconds = 0;
        timerLabel.setText("00:00:00");
        timerLabel.setForeground(TEXT_MUTED);
        descriptionArea.setText("");
        startButton.setText("▶  Iniciar");
        startButton.setEnabled(true);
        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);
    }

    private void loadHistory() {
        List<TimeRecord> records = repository.loadAll();
        for (TimeRecord r : records) {
            tableModel.addRow(new Object[]{r.timestamp, r.duration, r.description});
        }
    }

    private void onReport() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String today = LocalDate.now().format(fmt);

        // ── Diálogo de período ────────────────────────────────────────
        JDialog dlg = new JDialog(this, "Gerar Relatório", true);
        dlg.setSize(400, 230);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.getContentPane().setBackground(BG_CARD);
        dlg.setLayout(new BorderLayout(0, 0));

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(24, 28, 8, 28));

        JLabel lbFrom = new JLabel("De (dd/MM/yyyy)");
        lbFrom.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbFrom.setForeground(TEXT_PRIMARY);

        JLabel lbTo = new JLabel("Até (dd/MM/yyyy)");
        lbTo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbTo.setForeground(TEXT_PRIMARY);

        JTextField tfFrom = buildField(today);
        JTextField tfTo   = buildField(today);

        JLabel lbError = new JLabel(" ");
        lbError.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbError.setForeground(ACCENT_RED);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(0, 0, 10, 0);
        g.anchor = GridBagConstraints.WEST; g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0; g.weightx = 0.4; body.add(lbFrom, g);
        g.gridx = 1; g.gridy = 0; g.weightx = 0.6; g.insets = new Insets(0, 12, 10, 0); body.add(tfFrom, g);
        g.gridx = 0; g.gridy = 1; g.weightx = 0.4; g.insets = new Insets(0, 0, 10, 0);  body.add(lbTo, g);
        g.gridx = 1; g.gridy = 1; g.weightx = 0.6; g.insets = new Insets(0, 12, 10, 0); body.add(tfTo, g);
        g.gridx = 0; g.gridy = 2; g.gridwidth = 2;  g.insets = new Insets(0, 0, 0, 0);  body.add(lbError, g);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setOpaque(false);

        JButton btnCancel = buildButton("Cancelar", TEXT_MUTED, BG_DARK);
        JButton btnGen    = buildButton("Gerar HTML", new Color(139, 92, 246), BG_DARK);

        footer.add(btnCancel);
        footer.add(btnGen);

        dlg.add(body,   BorderLayout.CENTER);
        dlg.add(footer, BorderLayout.SOUTH);

        btnCancel.addActionListener(e -> dlg.dispose());

        btnGen.addActionListener(e -> {
            try {
                LocalDate from = LocalDate.parse(tfFrom.getText().trim(), fmt);
                LocalDate to   = LocalDate.parse(tfTo.getText().trim(),   fmt);
                if (from.isAfter(to)) {
                    lbError.setText("A data inicial não pode ser posterior à data final.");
                    return;
                }
                dlg.dispose();
                List<TimeRecord> all = repository.loadAll();
                File html = new ReportGenerator().generate(all, from, to);
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(html.toURI());
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Relatório salvo em:\n" + html.getAbsolutePath(),
                        "Relatório gerado", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (DateTimeParseException ex) {
                lbError.setText("Formato inválido. Use dd/MM/yyyy.");
            } catch (Exception ex) {
                lbError.setText("Erro ao gerar relatório: " + ex.getMessage());
            }
        });

        dlg.setVisible(true);
    }

    private JTextField buildField(String text) {
        JTextField tf = new JTextField(text);
        tf.setFont(new Font("Consolas", Font.PLAIN, 13));
        tf.setBackground(new Color(38, 38, 58));
        tf.setForeground(TEXT_PRIMARY);
        tf.setCaretColor(ACCENT_GREEN);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            new EmptyBorder(6, 10, 6, 10)
        ));
        return tf;
    }

    private void onClose() {
        if (running) {
            int choice = JOptionPane.showConfirmDialog(
                this,
                "Há uma tarefa em andamento. Deseja encerrar sem salvar?",
                "Sair",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (choice != JOptionPane.YES_OPTION) return;
        }
        dispose();
        System.exit(0);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private String formatTime(long totalSeconds) {
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private JPanel buildCard() {
        JPanel card = new JPanel();
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            new EmptyBorder(20, 24, 20, 24)
        ));
        return card;
    }

    private JButton buildButton(String text, Color fg, Color bg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(fg.darker());
                } else if (getModel().isRollover() && isEnabled()) {
                    g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 30));
                } else {
                    g2.setColor(bg);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(fg, 1),
            new EmptyBorder(10, 26, 10, 26)
        ));
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(false);
        return btn;
    }

    private void styleDialog(JDialog dialog) {
        dialog.getContentPane().setBackground(BG_CARD);
    }
}