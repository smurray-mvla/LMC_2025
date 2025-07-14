package application;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class LMCTextInputDialog {
	private Stage stage;
	private Scene scene;
	private GridPane gp;
	private TextField tf;
	private Label prompt;
	private Button ok;
	
	
	public LMCTextInputDialog() {

		prompt = new Label("Enter a number between 0 - 999:");
		tf = new TextField();
		tf.setOnAction(e -> updateUserInput());
		ok = new Button("OK");
		ok.setOnAction(e -> updateUserInput());
		gp = new GridPane();
		gp.add(prompt, 0, 0);
		gp.add(tf, 1, 0);
		gp.add(ok, 1, 1);
		scene = new Scene(gp,200,150);
		stage = new Stage();
		stage.setTitle("Input");
		stage.setScene(scene);
		stage.setAlwaysOnTop(true);
		stage.show();
	}
	
	private void updateUserInput() {
//		MainLMC.setUserResponse(tf.getText());
		
	}
	
	
	

}
