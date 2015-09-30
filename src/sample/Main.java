package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import sample.db.DbModel;

public class Main extends Application {

    private DbModel db = new DbModel();

    //TABLE VIEW AND DATA

    private TableView tableview;

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("main.fxml"));

        Scene scene = new Scene(root, 1000, 500);

        stage.setTitle("ERC Server - Database Manager");
        stage.setScene(scene);
        stage.show();
    }

    //MAIN EXECUTOR
    public static void main(String[] args) {
        launch(args);
    }





}
