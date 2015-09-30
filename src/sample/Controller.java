package sample;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import sample.db.DbModel;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;

import java.net.URL;
import java.sql.Connection;
import java.util.*;

public class Controller implements Initializable {
    @FXML
    private ChoiceBox tableNamesBox;

    @FXML
    private TableView tableView;

    @FXML
    private HBox hb;

    @FXML
    private Button refreshButton;

    private ObservableList<ObservableList> data;

    private DbModel db  = new DbModel();

    private HashMap<String, String> newRow = new HashMap<>();

    public void populateTableNames(Event event) {

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ObservableList obList = FXCollections.observableList(db.getTablesNames());
        tableNamesBox.getItems().clear();
        tableNamesBox.setItems(obList);

        tableNamesBox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                String selected = tableNamesBox.getItems().get((Integer) number2).toString();

                System.out.println(selected);

                // Update the table
                if (data != null){
                    data.clear();
                }

                buildData(selected);
            }
        });


    }

    //CONNECTION DATABASE
    public void buildData(String tableName){
        hb.getChildren().clear();
        data = FXCollections.observableArrayList();
        tableView.getColumns().clear();
        try{
            HashMap<Integer, HashMap<String, String>> rs = db.getRowsFromTable(null, tableName);

            boolean empty = rs.isEmpty();
            if (empty){
                rs = db.getColumnsNames(tableName);
            }
            /**********************************
             * TABLE COLUMN ADDED DYNAMICALLY *
             **********************************/
            String[] cols = new String[rs.get(1).size()];
            rs.get(1).keySet().toArray(cols);
            for(int i=0 ; i < cols.length; i++){
                //We are using non property style for making dynamic table
                final int j = i;
                TableColumn col = new TableColumn(cols[i]);
                col.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ObservableList,String>,ObservableValue<String>>(){
                    public ObservableValue<String> call(TableColumn.CellDataFeatures<ObservableList, String> param) {
                        return new SimpleStringProperty(param.getValue().get(j).toString());
                    }
                });

                tableView.getColumns().addAll(col);
                System.out.println("Column ["+i+"] ");
            }

            /********************************
             * Data added to ObservableList *
             ********************************/

            for(int i = 1; i <= rs.size() && !empty; i++){
                //Iterate Row
                ObservableList<String> row = FXCollections.observableArrayList();
                for (String col : cols){
                    row.add(rs.get(i).get(col));
                }
                System.out.println("Row [" + i + "] added "+row );
                data.add(row);

            }

            //FINALLY ADDED TO TableView

            tableView.getItems().clear();
            tableView.setItems(data);

            addNewRowForm();

        }catch(Exception e){
            e.printStackTrace();
            System.out.println("Error on Building Data");
        }
    }

    private void addNewRowForm() {
        List<Object> objs = new ArrayList<>();
        for (Object c : tableView.getColumns()){
            TextField tf = new TextField();
            TableColumn col = (TableColumn) c;
            tf.setPromptText(col.getText());
            //tf.setMaxWidth(col.getPrefWidth());
            tf.setMaxWidth(col.getMaxWidth());
            objs.add(tf);
        }

        final Button addButton = new Button("Add");
        addButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                HashMap<String, String> values = new HashMap<String, String>();
                for (Object o : hb.getChildrenUnmodifiable()){
                    try{
                        TextField col = (TextField) o;
                        values.put(col.getPromptText(), col.getText());

                    }catch (Exception ex){
                        // pass
                    }
                } // For
                // Insert into db
                db.addRow(tableNamesBox.getValue().toString(), values);

                // Remove hb

                buildData(tableNamesBox.getValue().toString());

            }
        });

        objs.add(addButton);


        hb.getChildren().addAll((Collection) objs);
        hb.setSpacing(3);

    }

    public void addRowClicked(ActionEvent actionEvent) {
        // Add an editable row OR submit an edited row

        // Check if the data is already enterd. If not, create an empty editable row
        if (newRow.isEmpty()){
            tableView.setEditable(true);


        }
    }

    public void rightMouseClickOnTable(Event event) {
        try {
            TableView tv = (TableView) event.getSource();
            TablePosition tp = (TablePosition) tv.getSelectionModel().getSelectedCells().get(0);
            System.out.println(String.format("Click at (%d, %d)", tp.getRow(), tp.getColumn()));

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation Dialog");
            alert.setHeaderText("Confirm delete");
            alert.setContentText("Are you sure you want to delete this row?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get().equals(ButtonType.OK)){
                // ... user chose OK
                HashMap<String, String> conds = new HashMap<>();
                ObservableList<TableColumn> columns = tv.getColumns();
                ObservableList currentRow = (ObservableList) tv.getItems().get(tp.getRow());
                for (int i = 0; i < columns.size(); i++){
                    conds.put(columns.get(i).getText(), currentRow.get(i).toString());
                }
                db.deleteRow(tableNamesBox.getValue().toString(), conds);
                buildData(tableNamesBox.getValue().toString());
            } else {
                // ... user chose CANCEL or closed the dialog
            }
        }catch(Exception ex){
            // pass
        }
    }

    public void refreshTable(ActionEvent actionEvent) {
        buildData(tableNamesBox.getValue().toString());
    }
}
