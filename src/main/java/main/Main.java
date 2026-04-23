package main;

public class Main {

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            ClockApp app = new ClockApp();
            app.setVisible(true);
        });
    }

}
