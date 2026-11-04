package soptecit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * Clase principal del sistema SOPTEC-IT.
 * Abre el Login en pantalla completa. F11 entra/sale de pantalla completa.
 */
public class SoptecIT extends Application {

    private static final boolean PANTALLA_COMPLETA = true;

    @Override
    public void start(Stage stage) throws Exception {
        cargarFuentes();

        Parent root = FXMLLoader.load(getClass().getResource("/soptecit/vistas/Login.fxml"));
        Scene scene = new Scene(root, 1280, 760);

        stage.setTitle("SOPTEC-IT · Control de Información y Soporte Técnico");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/soptecit/img/icono.png")));
        stage.setMinWidth(1200);
        stage.setMinHeight(700);
        stage.setScene(scene);

        // Pantalla completa: F11 entra y sale (ESC no la cierra por accidente)
        stage.setMaximized(true);
        stage.setFullScreenExitHint("Presiona F11 para salir de pantalla completa");
        stage.setFullScreenExitKeyCombination(KeyCombination.keyCombination("F11"));
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.F11 && !stage.isFullScreen()) {
                stage.setFullScreen(true);
            }
        });
        stage.setFullScreen(PANTALLA_COMPLETA);
        stage.show();
    }

    /** Carga la tipografía Poppins incluida en el proyecto (carpeta fonts). */
    private void cargarFuentes() {
        String[] fuentes = {"Poppins-Regular", "Poppins-Medium", "Poppins-SemiBold", "Poppins-Bold"};
        for (String f : fuentes) {
            Font.loadFont(getClass().getResourceAsStream("/soptecit/fonts/" + f + ".ttf"), 13);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
