package resort.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

final class DatePickerDialog {

    private DatePickerDialog() {
    }

    static LocalDate showDialog(Component parent, String title,
                                LocalDate initialDate, LocalDate minimumDate) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        LocalDate minDate = minimumDate == null ? LocalDate.MIN : minimumDate;
        LocalDate initial = initialDate == null || initialDate.isBefore(minDate)
                ? minDate : initialDate;
        CalendarPanel calendar = new CalendarPanel(dialog, initial, minDate);
        dialog.setContentPane(calendar);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return calendar.getSelectedDate();
    }

    private static final class CalendarPanel extends JPanel {
        private final JDialog dialog;
        private final LocalDate minimumDate;
        private final JPanel daysPanel = new JPanel(new GridLayout(0, 7, 4, 4));
        private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
        private final JButton previousButton = new JButton("\u2039");
        private final JButton nextButton = new JButton("\u203A");
        private YearMonth displayedMonth;
        private LocalDate selectedDate;

        CalendarPanel(JDialog dialog, LocalDate initial, LocalDate minimumDate) {
            this.dialog = dialog;
            this.minimumDate = minimumDate;
            this.displayedMonth = YearMonth.from(initial);
            setLayout(new BorderLayout(12, 12));
            setBackground(Theme.SAND_PANEL);
            setBorder(new EmptyBorder(18, 20, 16, 20));

            JPanel header = new JPanel(new BorderLayout(8, 0));
            header.setOpaque(false);
            styleNavigationButton(previousButton);
            styleNavigationButton(nextButton);
            previousButton.addActionListener(event -> changeMonth(-1));
            nextButton.addActionListener(event -> changeMonth(1));
            monthLabel.setFont(Theme.HEADER_FONT);
            monthLabel.setForeground(Theme.OCEAN_DEEP);
            header.add(previousButton, BorderLayout.WEST);
            header.add(monthLabel, BorderLayout.CENTER);
            header.add(nextButton, BorderLayout.EAST);
            add(header, BorderLayout.NORTH);

            daysPanel.setOpaque(false);
            daysPanel.setPreferredSize(new Dimension(350, 280));
            add(daysPanel, BorderLayout.CENTER);

            JButton cancelButton = new JButton("Cancel");
            Theme.styleSecondaryButton(cancelButton);
            cancelButton.addActionListener(event -> dialog.dispose());
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            footer.setOpaque(false);
            footer.add(cancelButton);
            add(footer, BorderLayout.SOUTH);

            updateCalendar();
        }

        LocalDate getSelectedDate() {
            return selectedDate;
        }

        private void styleNavigationButton(JButton button) {
            Theme.styleSecondaryButton(button);
            button.setFont(new Font("SansSerif", Font.BOLD, 24));
            button.setBorder(new EmptyBorder(2, 14, 2, 14));
        }

        private void changeMonth(int amount) {
            displayedMonth = displayedMonth.plusMonths(amount);
            updateCalendar();
        }

        private void updateCalendar() {
            monthLabel.setText(displayedMonth.getMonth().getDisplayName(
                    TextStyle.FULL, Locale.getDefault()) + " " + displayedMonth.getYear());
            previousButton.setEnabled(displayedMonth.isAfter(YearMonth.from(minimumDate)));
            daysPanel.removeAll();

            for (DayOfWeek day : DayOfWeek.values()) {
                JLabel dayLabel = new JLabel(
                        day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        SwingConstants.CENTER);
                dayLabel.setFont(Theme.LABEL_FONT.deriveFont(Font.BOLD, 13f));
                dayLabel.setForeground(Theme.OCEAN_DEEP);
                daysPanel.add(dayLabel);
            }

            LocalDate firstDay = displayedMonth.atDay(1);
            int leadingDays = firstDay.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
            for (int i = 0; i < leadingDays; i++) {
                daysPanel.add(new JLabel());
            }

            for (int day = 1; day <= displayedMonth.lengthOfMonth(); day++) {
                LocalDate date = displayedMonth.atDay(day);
                JButton dayButton = new JButton(Integer.toString(day));
                dayButton.setFont(Theme.LABEL_FONT);
                dayButton.setFocusPainted(false);
                dayButton.setOpaque(true);
                dayButton.setBorder(BorderFactory.createLineBorder(
                        date.equals(LocalDate.now()) ? Theme.PALM : Theme.SAND_DARK,
                        date.equals(LocalDate.now()) ? 2 : 1));

                boolean selectable = !date.isBefore(minimumDate);
                dayButton.setEnabled(selectable);
                if (date.equals(selectedDate)) {
                    dayButton.setBackground(Theme.OCEAN_DEEP);
                    dayButton.setForeground(Color.WHITE);
                } else {
                    dayButton.setBackground(Theme.INPUT_CREAM);
                    dayButton.setForeground(Theme.TEXT_DARK);
                }
                dayButton.addActionListener(event -> {
                    selectedDate = date;
                    dialog.dispose();
                });
                daysPanel.add(dayButton);
            }
            daysPanel.revalidate();
            daysPanel.repaint();
            nextButton.setEnabled(true);
        }
    }
}
