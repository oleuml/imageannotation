import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

import java.util.stream.Collectors;

import java.util.ArrayList;
import java.util.List;

import javafx.application.*;
import javafx.event.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.input.*;
import javafx.scene.shape.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * Image Annotation tool
 * Selection idea from: https://stackoverflow.com/questions/30993681
 * 
 * @author meipp, oluml
 */
public class GUI extends Application {
	private Stage primaryStage;
	private Group imageLayer;
	private ImageView imageView;

	private Path selectedImage;
	private Selection selection;

	private static List<Path> images;
	private static int imageIndex;

	private static Path outputFile;

	public static void main(String[] args) throws IOException {
		outputFile = Paths.get(String.format("%s/info.dat", args[0]));
		if(!Files.exists(outputFile))
			Files.write(outputFile, "".getBytes());

		images = Files.list(Paths.get(args[0]))
					  .filter(p -> p.toString().toLowerCase().endsWith(".jpg") || p.toString().toLowerCase().endsWith(".png"))
					  .collect(Collectors.toList());
		imageIndex = 0;
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		try {
			this.primaryStage = primaryStage;

			primaryStage.setTitle("ImageAnnotation");

			BorderPane root = new BorderPane();
		
			imageLayer = new Group();
			imageView = new ImageView();
			imageLayer.getChildren().add(imageView);

			loadImage(images.get(0));

			root.setCenter(imageLayer);

			Scene scene = new Scene(root, 1024, 768);
			scene.setOnKeyPressed(event -> {
				switch(event.getCode()) {
					case Q:
					try {
						saveImage(selectedImage);
					}
					catch(IOException e) {
						throw new RuntimeException(e);
					}
					System.exit(0);
					case LEFT:
					increment(-1);
					break;
					case RIGHT:
					increment(+1);
					break;
				}
			});

			primaryStage.setScene(scene);
			primaryStage.show();
		}
		catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void increment(int inc) {
		imageIndex = Math.floorMod(imageIndex + inc, images.size());
		try {
			loadImage(images.get(imageIndex));
		}
		catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	private EventHandler<ActionEvent> incrementImage(int inc) {
		return event -> {
			increment(inc);
		};
	}

	private void setSelection(Selection selection) {
		imageLayer.getChildren().clear();
		imageLayer.getChildren().add(imageView);
	}

	private void loadImage(Path imagePath) throws IOException {
		if(selectedImage != null) {
			saveImage(selectedImage);
		}

		selectedImage = imagePath;
		imagePath = imagePath.toAbsolutePath().normalize();
		Image image = new Image(String.format("file://%s", imagePath));
		imageView.setImage(image);

		boolean done = false;
		List<String> lines = Files.readAllLines(outputFile);
		for(String line : lines) {
			String[] info = line.split("\\s+");
			if(Files.isSameFile(imagePath, Paths.get(info[0]))) {
				setSelection(selection);
				selection = selection(info);
				done = true;
				break;
			}
		}

		if(!done) {
			setSelection(selection);
			selection = new Selection(imageLayer);
		}

		primaryStage.setTitle(imagePath.toString());
	}

	private Selection selection(String[] info) {
		return new Selection(imageLayer, info);
	}

	private void saveImage(Path imagePath) throws IOException {
		boolean done = false;
		List<String> lines = Files.readAllLines(outputFile);
		for(int i = 0; i<lines.size(); i++) {
			String[] info = lines.get(i).split("\\s+");
			if(Files.isSameFile(imagePath, Paths.get(info[0]))) {
				lines.set(i, imagePath + "\t" + selection);
				done = true;
				break;
			}
		}

		if(!done) {
			lines.add(imagePath + "\t" + selection);
		}

		Files.write(outputFile, lines);
	}

	public static class Selection {
		DragContext dragContext = new DragContext();
		List<Rectangle> rects;
		Rectangle current;
		Group group;

		public Selection(Group group) {
			this(group, null);
		}

		public Selection(Group group, String[] info) {
			this.group = group;
			rects = new ArrayList<>();

			if(info != null) {
				int n = Integer.parseInt(info[1]);
				for(int i = 2; i<4*n+2; i+=4) {
					Rectangle r = rectangle(
						Integer.parseInt(info[i+0]),
						Integer.parseInt(info[i+1]),
						Integer.parseInt(info[i+2]),
						Integer.parseInt(info[i+3])
					);
					rects.add(r);
					group.getChildren().add(r);
				}
			}

			group.setOnMousePressed(this::onMousePressed);
			group.setOnMouseDragged(this::onMouseDragged);
			group.setOnMouseReleased(this::onMouseReleased);
		}

		private Rectangle rectangle(int x, int y, int width, int height) {
			Rectangle r = new Rectangle(x, y, width, height);
			r.setStroke(Color.RED);
			r.setStrokeWidth(1);
			r.setStrokeLineCap(StrokeLineCap.ROUND);
			r.setFill(Color.LIGHTBLUE.deriveColor(0, 1.2, 1, 0.6));
			r.setOnMouseClicked(removeRectangle(r));

			return r;
		}

		//public Bounds getBounds() {
		//	return group.getBounds();
		//}

		private void onMousePressed(MouseEvent event) {
			if(event.isPrimaryButtonDown()) {
				dragContext.x = event.getX();
				dragContext.y = event.getY();

				current = rectangle((int)dragContext.x, (int)dragContext.y, 0, 0);

				group.getChildren().add(current);
			}
		}

		private void onMouseDragged(MouseEvent event) {
			if(event.isPrimaryButtonDown()) {
				double width  = event.getX() - dragContext.x;
				double height = event.getY() - dragContext.y;

				if(width > 0)
					current.setWidth(width);
				else {
					current.setX(event.getX());
					current.setWidth(dragContext.x - current.getX());
				}

				if(height > 0)
					current.setHeight(height);
				else {
					current.setY(event.getY());
					current.setHeight(dragContext.y - current.getY());
				}
			}
		}

		private void onMouseReleased(MouseEvent event) {
			if(event.getButton() == MouseButton.PRIMARY) {
				if(current.getWidth() == 0 || current.getHeight() == 0)
					group.getChildren().remove(current);
				else {
					rects.add(current);
				}
			}
		}

		private EventHandler<MouseEvent> removeRectangle(Rectangle rect) {
			return event -> {
				if(event.getButton() == MouseButton.SECONDARY) {
					group.getChildren().remove(rect);
					rects.remove(rect);
				}
			};
		}

		public String toString() {
			String s = rects.size() + "";
			for(Rectangle r : rects)
				s += String.format("\t%d %d %d %d", (int)r.getX(), (int)r.getY(), (int)r.getWidth(), (int)r.getHeight());

			return s;
		}

		private static class DragContext {
			private double x, y;
		}
	}
}
