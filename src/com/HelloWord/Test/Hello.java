package com.HelloWord.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.net.http.*;
import java.net.URI;
import java.util.List;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;


public class Hello {

    private static final String BASE_URL = "http://localhost:5000";
    private static JPanel chartPanel;
    private static JLabel minLabel;
    private static JLabel maxLabel;
    private static JLabel totalLabel;

    public static void main(String[] args) {
        JFrame frame = new JFrame("Gestion des Enseignants");
        
        ImageIcon logo = new ImageIcon("LOGO.ico");

        // Définir l'icône de la fenêtre
        frame.setIconImage(logo.getImage());
        frame.setSize(1000, 600);
        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        DefaultTableModel model = new DefaultTableModel(new String[]{"Numens", "Nom", "Nb Heures", "Taux Horaire", "Salaire"}, 0);
        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        frame.add(scrollPane, BorderLayout.CENTER);

        JPanel panel = new JPanel(new GridLayout(5, 2));
        JTextField numensField = new JTextField();
        JTextField nomField = new JTextField();
        JTextField nbheuresField = new JTextField();
        JTextField tauxhoraireField = new JTextField();

        panel.add(new JLabel("Numéro Enseignant :"));
        panel.add(numensField);
        panel.add(new JLabel("Nom :"));
        panel.add(nomField);
        panel.add(new JLabel("Nombre d'heures :"));
        panel.add(nbheuresField);
        panel.add(new JLabel("Taux Horaire :"));
        panel.add(tauxhoraireField);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addButton = new JButton("Ajouter");
        JButton updateButton = new JButton("Modifier");
        JButton deleteButton = new JButton("Supprimer");
        JButton clearButton = new JButton("Effacer");

        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearButton);
        
        clearButton.addActionListener(e -> {
            clearFields(numensField, nomField, nbheuresField, tauxhoraireField);
            numensField.setEditable(true);
            table.clearSelection();  // <-- Cette ligne déselectionne la ligne dans la JTable
        });

        panel.add(buttonPanel);
        frame.add(panel, BorderLayout.NORTH);

        // Panneau pour les informations de salaire
        JPanel salaryInfoPanel = new JPanel(new GridLayout(1, 3));
        minLabel = new JLabel("Salaire Minimal: ");
        maxLabel = new JLabel("Salaire Maximal: ");
        totalLabel = new JLabel("Salaire Total: ");
        salaryInfoPanel.add(minLabel);
        salaryInfoPanel.add(maxLabel);
        salaryInfoPanel.add(totalLabel);
        frame.add(salaryInfoPanel, BorderLayout.SOUTH);

        // Panneau pour l'histogramme
        chartPanel = new JPanel(new BorderLayout());
        chartPanel.setPreferredSize(new Dimension(300, 400));
        frame.add(chartPanel, BorderLayout.EAST);

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    numensField.setText(model.getValueAt(selectedRow, 0).toString());
                    nomField.setText(model.getValueAt(selectedRow, 1).toString());
                    nbheuresField.setText(model.getValueAt(selectedRow, 2).toString());
                    tauxhoraireField.setText(model.getValueAt(selectedRow, 3).toString());
                    numensField.setEditable(false);
                }
            }
        });

        addButton.addActionListener(e -> {
            if (validateInputs(frame, numensField, nomField, nbheuresField, tauxhoraireField)) {
                addEnseignant(numensField.getText(), nomField.getText(), nbheuresField.getText(), 
                            tauxhoraireField.getText(), model);
                clearFields(numensField, nomField, nbheuresField, tauxhoraireField);
                numensField.setEditable(true);
            }
        });

        updateButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(frame, "Veuillez sélectionner un enseignant à modifier", "Erreur", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (validateInputs(frame, numensField, nomField, nbheuresField, tauxhoraireField)) {
                updateEnseignant(numensField.getText(), nomField.getText(), nbheuresField.getText(), 
                              tauxhoraireField.getText(), model);
                clearFields(numensField, nomField, nbheuresField, tauxhoraireField);
                numensField.setEditable(true);
            }
        });

        deleteButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(frame, "Veuillez sélectionner un enseignant à supprimer", "Erreur", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String numens = model.getValueAt(selectedRow, 0).toString();
            int confirm = JOptionPane.showConfirmDialog(frame, "Supprimer l'enseignant " + numens + " ?", "Confirmation", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                deleteEnseignant(numens, model);
                clearFields(numensField, nomField, nbheuresField, tauxhoraireField);
                numensField.setEditable(true);
            }
        });

        clearButton.addActionListener(e -> {
            clearFields(numensField, nomField, nbheuresField, tauxhoraireField);
            numensField.setEditable(true);
        });

        fetchEnseignants(model);
        frame.setVisible(true);
    }

    private static boolean validateInputs(JFrame frame, JTextField numensField, JTextField nomField, 
                                        JTextField nbheuresField, JTextField tauxhoraireField) {
        if (numensField.getText().trim().isEmpty() || nomField.getText().trim().isEmpty() ||
            nbheuresField.getText().trim().isEmpty() || tauxhoraireField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Tous les champs doivent être remplis", "Erreur", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            double nb = Double.parseDouble(nbheuresField.getText());
            double taux = Double.parseDouble(tauxhoraireField.getText());
            if (nb < 0 || taux < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Nombre Heures et Taux Horaire doivent être des nombres positifs", "Erreur", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private static void clearFields(JTextField... fields) {
        for (JTextField f : fields) f.setText("");
    }

    private static void addEnseignant(String numens, String nom, String nbheures, String tauxhoraire,
            DefaultTableModel model) {
try {
// Vérifier d'abord si le numéro existe déjà dans le modèle (table)
for (int i = 0; i < model.getRowCount(); i++) {
if (model.getValueAt(i, 0).toString().equals(numens)) {
JOptionPane.showMessageDialog(null, "Le numéro enseignant existe déjà", "Erreur", JOptionPane.ERROR_MESSAGE);
return;
}
}

String json = String.format("{\"numens\":\"%s\", \"nom\":\"%s\", \"nbheures\":\"%s\", \"tauxhoraire\":\"%s\"}",
numens, nom, nbheures, tauxhoraire);
HttpRequest request = HttpRequest.newBuilder()
.uri(URI.create(BASE_URL + "/add-enseignant"))
.header("Content-Type", "application/json")
.POST(HttpRequest.BodyPublishers.ofString(json))
.build();
HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
if (response.statusCode() == 200) {
JOptionPane.showMessageDialog(null, "Ajout réussi");
fetchEnseignants(model);
} else if (response.statusCode() == 409) { // 409 Conflict est souvent utilisé pour les doublons
JOptionPane.showMessageDialog(null, "Le numéro enseignant existe déjà", "Erreur", JOptionPane.ERROR_MESSAGE);
} else {
JOptionPane.showMessageDialog(null, "Erreur lors de l'ajout: " + response.body());
}
});
} catch (Exception ex) {
ex.printStackTrace();
}
}

    private static void updateEnseignant(String numens, String nom, String nbheures, String tauxhoraire,
                                       DefaultTableModel model) {
        try {
            String json = String.format("{\"numens\":\"%s\", \"nom\":\"%s\", \"nbheures\":\"%s\", \"tauxhoraire\":\"%s\"}",
                    numens, nom, nbheures, tauxhoraire);
            String encodedNumens = URLEncoder.encode(numens, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/update-enseignant/" + encodedNumens))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
                if (response.statusCode() == 200) {
                    JOptionPane.showMessageDialog(null, "Modification réussie");
                    fetchEnseignants(model);
                } else {
                    JOptionPane.showMessageDialog(null, "Erreur lors de la modification: " + response.body());
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void deleteEnseignant(String numens, DefaultTableModel model) {
        try {
            String encodedNumens = URLEncoder.encode(numens, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/delete-enseignant/" + encodedNumens))
                    .DELETE()
                    .build();
            HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
                if (response.statusCode() == 200) {
                    JOptionPane.showMessageDialog(null, "Suppression réussie");
                    fetchEnseignants(model);
                } else {
                    JOptionPane.showMessageDialog(null, "Erreur lors de la suppression: " + response.body());
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void fetchEnseignants(DefaultTableModel model) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/enseignants"))
                    .GET()
                    .build();
            HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
                if (response.statusCode() == 200) {
                    try {
                        model.setRowCount(0);
                        ObjectMapper mapper = new ObjectMapper();
                     // ...
                        List<Object> list = mapper.readValue(response.body(), List.class);

                        model.setRowCount(0);

                        if (list.isEmpty()) {
                            minLabel.setText("Salaire Min: 0.00");
                            maxLabel.setText("Salaire Max: 0.00");
                            totalLabel.setText("Salaire Total: 0.00");
                            updateChart(0, 0, 0); // Optionnel : afficher un graphique vide
                            return;
                        }

                        double min = Double.MAX_VALUE, max = Double.MIN_VALUE, total = 0;

                        for (Object item : list) {
                            var map = (java.util.Map<?, ?>) item;
                            String numens = map.get("numens").toString();
                            String nom = map.get("nom").toString();
                            double nb = Double.parseDouble(map.get("nbheures").toString());
                            double taux = Double.parseDouble(map.get("tauxhoraire").toString());
                            double salaire = nb * taux;

                            model.addRow(new Object[]{numens, nom, nb, taux, salaire});

                            min = Math.min(min, salaire);
                            max = Math.max(max, salaire);
                            total += salaire;
                        }

                        // Mise à jour des labels
                        minLabel.setText(String.format("Salaire Min: %.2f", min));
                        maxLabel.setText(String.format("Salaire Max: %.2f", max));
                        totalLabel.setText(String.format("Salaire Total: %.2f", total));

                        // Mise à jour de l'histogramme
                        updateChart(min, max, total);

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    System.err.println("Erreur de récupération: " + response.body());
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void updateChart(double minSalary, double maxSalary, double totalSalary) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        if (minSalary == 0 && maxSalary == 0 && totalSalary == 0) {
            dataset.addValue(0, "Salaires", "Aucune donnée");
        } else {
            dataset.addValue(minSalary, "Salaires", "Minimum");
            dataset.addValue(maxSalary, "Salaires", "Maximum");
            dataset.addValue(totalSalary, "Salaires", "Total");
        }

        JFreeChart barChart = ChartFactory.createBarChart(
                "Statistiques des Salaires",
                "Catégorie",
                "Montant",
                dataset
        );

        ChartPanel chart = new ChartPanel(barChart);
        chart.setPreferredSize(new Dimension(300, 400));

        chartPanel.removeAll();
        chartPanel.add(chart, BorderLayout.CENTER);
        chartPanel.revalidate();
    }

}