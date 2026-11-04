module soptecit {
    requires javafx.controls;
    requires javafx.fxml;

    opens soptecit.controladores to javafx.fxml;
    exports soptecit;
}
