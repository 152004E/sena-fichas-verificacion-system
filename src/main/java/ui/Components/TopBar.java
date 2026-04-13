package ui.Components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class TopBar {

    private final Stage stage;
    private final Consumer<String> onSearch;
    private final Map<String, Label> tabs = new LinkedHashMap<>();

    private static final String TAB_DEFAULT = "-fx-font-size: 12px; -fx-text-fill: #6b7280; -fx-cursor: hand;" +
            "-fx-padding: 0 12 0 12; -fx-border-width: 0 0 2 0; -fx-border-color: transparent;";
    private static final String TAB_ACTIVE = "-fx-font-size: 12px; -fx-text-fill: #39A900; -fx-cursor: hand;" +
            "-fx-padding: 0 12 0 12; -fx-border-width: 0 0 2 0; -fx-border-color: #39A900;";

    public TopBar(Stage stage, Consumer<String> onSearch) {
        this.stage = stage;
        this.onSearch = onSearch;
    }

    public void setActiveTab(String key) {
        tabs.forEach((k, tab) -> tab.setStyle(k.equals(key) ? TAB_ACTIVE : TAB_DEFAULT));
    }

    private Label buildTab(String text, String key) {
        Label tab = new Label(text);
        tab.setStyle(TAB_DEFAULT);
        tab.setMinHeight(46);
        tab.setAlignment(Pos.CENTER);
        return tab;
    }
}