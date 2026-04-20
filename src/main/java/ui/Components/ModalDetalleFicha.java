package ui.Components;

import Model.Ficha;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class ModalDetalleFicha extends StackPane {

        private final Ficha ficha;

        public ModalDetalleFicha(Ficha ficha) {
                this.ficha = ficha;
                initModal();
        }

        private void initModal() {
                setStyle("-fx-background-color: rgba(0,0,0,0.75);");
                setAlignment(Pos.CENTER);

                Label badge = new Label("FICHA VERIFICACIÓN");
                badge.setStyle("-fx-text-fill: #39A900; -fx-font-weight: bold; -fx-font-size: 10px;");

                Label estadoBadge = new Label(ficha.getEstado().getLabel().toUpperCase());
                estadoBadge.setStyle(
                                "-fx-background-color: #1c3a12; -fx-text-fill: #5ed01a;" +
                                                "-fx-font-size: 10px; -fx-font-weight: bold;" +
                                                "-fx-background-radius: 20; -fx-padding: 3 10 3 10;");

                HBox badgeRow = new HBox(10, badge, estadoBadge);
                badgeRow.setAlignment(Pos.CENTER_LEFT);

                Label titulo = new Label("Detalle de Ficha: " + ficha.getNumero());
                titulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");

                Label programa = new Label(ficha.getPrograma() != null ? ficha.getPrograma() : "—");
                programa.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 14px;");

                Button btnX = new Button("✕");
                btnX.setStyle(
                                "-fx-background-color: #1e2230; -fx-text-fill: #8b92a5;" +
                                                "-fx-background-radius: 50; -fx-cursor: hand;" +
                                                "-fx-font-size: 13px; -fx-padding: 5 10 5 10;" +
                                                "-fx-border-color: #2a2d3a; -fx-border-radius: 50;");
                btnX.setOnMouseEntered(e -> btnX.setStyle(
                                "-fx-background-color: #2a2d3a; -fx-text-fill: #e8eaf0;" +
                                                "-fx-background-radius: 50; -fx-cursor: hand;" +
                                                "-fx-font-size: 13px; -fx-padding: 5 10 5 10;" +
                                                "-fx-border-color: #3a3d4a; -fx-border-radius: 50;"));
                btnX.setOnMouseExited(e -> btnX.setStyle(
                                "-fx-background-color: #1e2230; -fx-text-fill: #8b92a5;" +
                                                "-fx-background-radius: 50; -fx-cursor: hand;" +
                                                "-fx-font-size: 13px; -fx-padding: 5 10 5 10;" +
                                                "-fx-border-color: #2a2d3a; -fx-border-radius: 50;"));

                VBox headerLeft = new VBox(6, badgeRow, titulo, programa);
                Region hSpacer = new Region();
                HBox.setHgrow(hSpacer, Priority.ALWAYS);
                HBox header = new HBox(headerLeft, hSpacer, btnX);
                header.setAlignment(Pos.TOP_CENTER);
                header.setPadding(new Insets(0, 0, 16, 0));
                header.setStyle("-fx-border-color: #2a2d3a; -fx-border-width: 0 0 1 0;");

                VBox cardVistas = buildCardVistas();
                VBox cardFalt = buildCardFaltantes();

                HBox body = new HBox(16, cardVistas, cardFalt);
                VBox.setVgrow(body, Priority.ALWAYS);

                HBox footer = buildFooter();

                VBox modal = new VBox(20, header, body, footer);
                modal.setPadding(new Insets(28, 28, 0, 28));
                modal.setMaxWidth(820);
                modal.setMaxHeight(560);
                modal.setStyle(
                                "-fx-background-color: #13161f;" +
                                                "-fx-background-radius: 20;" +
                                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 40, 0, 0, 10);");

                btnX.setOnAction(e -> cerrar());
                this.setOnMouseClicked(e -> {
                        if (e.getTarget() == this)
                                cerrar();
                });

                this.getChildren().add(modal);
        }

        @SuppressWarnings("deprecation")
        private VBox buildCardVistas() {
                Label iconVistas = new Label("✔");
                iconVistas.setStyle(
                                "-fx-background-color: #1c3a12; -fx-text-fill: #5ed01a;" +
                                                "-fx-background-radius: 8; -fx-padding: 4 8; -fx-font-size: 13px;");
                Label tVistas = new Label("Competencias Vistas");
                tVistas.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #e8eaf0;");
                HBox hVistas = new HBox(8, iconVistas, tVistas);
                hVistas.setAlignment(Pos.CENTER_LEFT);

                TableView<String> tableVistas = new TableView<>();
                tableVistas.setStyle(
                                "-fx-background-color: #0f1117; -fx-background-radius: 12;" +
                                                "-fx-border-color: #2a2d3a; -fx-border-radius: 12;" +
                                                "-fx-control-inner-background: #0f1117;" +
                                                "-fx-table-cell-border-color: #2a2d3a;");
                tableVistas.setPrefHeight(180);
                tableVistas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

                TableColumn<String, String> c1 = new TableColumn<>("COMPETENCIA");
                c1.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()));
                c1.setCellFactory(col -> new TableCell<>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                                super.updateItem(item, empty);
                                if (empty || item == null) {
                                        setGraphic(null);
                                        return;
                                }
                                HBox row = new HBox(8);
                                row.setAlignment(Pos.CENTER_LEFT);
                                Label dot = new Label("●");
                                dot.setStyle("-fx-text-fill: #39A900; -fx-font-size: 8px;");
                                Label lbl = new Label(item);
                                lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #e8eaf0;");
                                row.getChildren().addAll(dot, lbl);
                                setGraphic(row);
                                setText(null);
                                setStyle("-fx-background-color: transparent;");
                        }
                });

                TableColumn<String, String> c2 = new TableColumn<>("INSTRUCTOR");
                c2.setCellValueFactory(d -> new SimpleStringProperty(
                                ficha.getTransversalesVistas().get(d.getValue())));
                c2.setCellFactory(col -> new TableCell<>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                                super.updateItem(item, empty);
                                setText(empty ? null : item);
                                setStyle("-fx-text-fill: #8b92a5; -fx-background-color: transparent;");
                        }
                });

                tableVistas.getColumns().addAll(List.of(c1, c2));
                tableVistas.setItems(
                                FXCollections.observableArrayList(
                                                ficha.getTransversalesVistas().keySet()));

                VBox cardVistas = new VBox(12, hVistas, tableVistas);
                cardVistas.setPadding(new Insets(16));
                cardVistas.setStyle("-fx-background-color: #1a2332; -fx-background-radius: 16;");
                HBox.setHgrow(cardVistas, Priority.ALWAYS);  // Crece para ocupar el mayor espacio

                return cardVistas;
        }

        @SuppressWarnings("deprecation")
        private VBox buildCardFaltantes() {
                Label iconFalt = new Label("✖");
                iconFalt.setStyle(
                                "-fx-background-color: #2d1515; -fx-text-fill: #e24b4a;" +
                                                "-fx-background-radius: 8; -fx-padding: 4 8; -fx-font-size: 13px;");
                Label tFalt = new Label("Competencias Faltantes");
                tFalt.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #e8eaf0;");
                HBox hFalt = new HBox(8, iconFalt, tFalt);
                hFalt.setAlignment(Pos.CENTER_LEFT);

                List<String> faltantesList = ficha.getTransversalesFaltantes();
                if (faltantesList == null) faltantesList = List.of();

                TableView<String> tableFalt = new TableView<>();
                tableFalt.setStyle(
                                "-fx-background-color: #0f1117; -fx-background-radius: 12;" +
                                                "-fx-border-color: #3a1a1a; -fx-border-radius: 12;" +
                                                "-fx-control-inner-background: #0f1117;" +
                                                "-fx-table-cell-border-color: #2a2d3a;");
                tableFalt.setPrefHeight(180);
                tableFalt.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
                tableFalt.setItems(FXCollections.observableArrayList(faltantesList));

                TableColumn<String, String> f1 = new TableColumn<>("COMPETENCIA");
                f1.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()));
                f1.setCellFactory(col -> new TableCell<>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                                super.updateItem(item, empty);
                                if (empty || item == null) {
                                        setGraphic(null);
                                        return;
                                }
                                HBox row = new HBox(8);
                                row.setAlignment(Pos.CENTER_LEFT);
                                Label dot = new Label("●");
                                dot.setStyle("-fx-text-fill: #e24b4a; -fx-font-size: 8px;");
                                Label lbl = new Label(item);
                                lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #e8eaf0;");
                                row.getChildren().addAll(dot, lbl);
                                setGraphic(row);
                                setText(null);
                                setStyle("-fx-background-color: transparent;");
                        }
                });

                TableColumn<String, String> f2 = new TableColumn<>("STATUS");
                f2.setCellValueFactory(d -> new SimpleStringProperty("Pendiente"));
                f2.setCellFactory(col -> new TableCell<>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                                super.updateItem(item, empty);
                                if (empty || item == null) {
                                        setGraphic(null);
                                        return;
                                }
                                Label b = new Label(item.toUpperCase());
                                b.setStyle(
                                                "-fx-background-color: #2d1515; -fx-text-fill: #e24b4a;" +
                                                                "-fx-padding: 3 10; -fx-background-radius: 20;" +
                                                                "-fx-font-size: 9px; -fx-font-weight: bold;");
                                setGraphic(b);
                                setText(null);
                                setStyle("-fx-background-color: transparent; -fx-alignment: center-left;");
                        }
                });

                tableFalt.getColumns().addAll(List.of(f1, f2));

                Label infoIcon = new Label("ℹ");
                infoIcon.setStyle("-fx-text-fill: #39A900; -fx-font-size: 14px;");
                Label infoText = new Label("Las competencias faltantes serán asignadas en el próximo trimestre.");
                infoText.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 11px;");
                infoText.setWrapText(true);
                HBox infoBanner = new HBox(8, infoIcon, infoText);
                infoBanner.setAlignment(Pos.CENTER_LEFT);
                infoBanner.setPadding(new Insets(10, 12, 10, 12));
                infoBanner.setStyle(
                                "-fx-background-color: #1c3a12;" +
                                                "-fx-background-radius: 10;" +
                                                "-fx-border-color: #2d5a1a; -fx-border-radius: 10;");

                VBox cardFalt = new VBox(12, hFalt, tableFalt, infoBanner);
                cardFalt.setPadding(new Insets(16));
                cardFalt.setStyle("-fx-background-color: #2a1520; -fx-background-radius: 16;");
                cardFalt.setPrefWidth(260);  // Ancho fijo: más pequeña que Vistas
                HBox.setHgrow(cardFalt, Priority.NEVER);

                return cardFalt;
        }

        private HBox buildFooter() {
                Button cerrar = new Button("Cerrar");
                cerrar.setStyle(
                                "-fx-background-color: transparent; -fx-text-fill: #8b92a5;" +
                                                "-fx-font-weight: bold; -fx-background-radius: 20;" +
                                                "-fx-border-color: #2a2d3a; -fx-border-radius: 20;" +
                                                "-fx-padding: 8 24; -fx-cursor: hand;");
                cerrar.setOnMouseEntered(e -> cerrar.setStyle(
                                "-fx-background-color: #1e2230; -fx-text-fill: #e8eaf0;" +
                                                "-fx-font-weight: bold; -fx-background-radius: 20;" +
                                                "-fx-border-color: #3a3d4a; -fx-border-radius: 20;" +
                                                "-fx-padding: 8 24; -fx-cursor: hand;"));
                cerrar.setOnMouseExited(e -> cerrar.setStyle(
                                "-fx-background-color: transparent; -fx-text-fill: #8b92a5;" +
                                                "-fx-font-weight: bold; -fx-background-radius: 20;" +
                                                "-fx-border-color: #2a2d3a; -fx-border-radius: 20;" +
                                                "-fx-padding: 8 24; -fx-cursor: hand;"));
                cerrar.setOnAction(e -> cerrar());

                Button exportar = new Button("⬆  Exportar Detalle");
                exportar.setStyle(
                                "-fx-background-color: #226d00; -fx-text-fill: white;" +
                                                "-fx-font-weight: bold; -fx-background-radius: 20;" +
                                                "-fx-padding: 8 24; -fx-cursor: hand;");
                exportar.setOnMouseEntered(e -> exportar.setStyle(
                                "-fx-background-color: #39A900; -fx-text-fill: white;" +
                                                "-fx-font-weight: bold; -fx-background-radius: 20;" +
                                                "-fx-padding: 8 24; -fx-cursor: hand;"));
                exportar.setOnMouseExited(e -> exportar.setStyle(
                                "-fx-background-color: #226d00; -fx-text-fill: white;" +
                                                "-fx-font-weight: bold; -fx-background-radius: 20;" +
                                                "-fx-padding: 8 24; -fx-cursor: hand;"));

                HBox footer = new HBox(12, cerrar, exportar);
                footer.setAlignment(Pos.CENTER_RIGHT);
                footer.setStyle(
                                "-fx-background-color: #0f1117;" +
                                                "-fx-border-color: #2a2d3a; -fx-border-width: 1 0 0 0;" +
                                                "-fx-padding: 14 24 14 24;");

                return footer;
        }

        public void cerrar() {
                StackPane root = (StackPane) this.getParent();
                if (root != null) {
                        root.getChildren().remove(this);
                }
        }
}
