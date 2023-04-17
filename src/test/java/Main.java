import org.xiphis.swing.HtmlDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    public static void main(String[] args) {

        JFrame frame = new JFrame("Demo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JButton button = new JButton("Test");
        frame.getContentPane().add(button);

        button.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {

                JDialog dialog = new HtmlDialog(frame, "<html>" +
                        "<head><title>Hello World</title>" +
                        "<link rel=\"stylesheet\" src=\"/org/xiphis/swing/default-style-sheet.css\">" +
                        "</head><body width='320' height='240'>" +
                        "<p>Foo bar!<br>This <mark>is</mark> a <b>simple test</b>... <!--table>\n" +
                        "  <tr>\n" +
                        "    <th>Month</th>\n" +
                        "    <th>Savings</th>\n" +
                        "  </tr>\n" +
                        "  <tr>\n" +
                        "    <td>January</td>\n" +
                        "    <td>$100</td>\n" +
                        "  </tr>\n" +
                        "</table--><h1>Moaring <code>codeing</code></h1>Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. testing <button type='button'>foo!</button><hr><input type='submit'>" +
                        "</body></html>", true);


                dialog.pack();
                dialog.setVisible(true);
            }
        });

        frame.setSize(600, 400);

        frame.setVisible(true);

    }
}