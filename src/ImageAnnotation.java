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

public class ImageAnnotation {
	public static void main(String[] args) throws IOException {
		if(args.length == 1) {
			GUI.main(args);
		}
		else if(args.length == 2) {
			Path source = Paths.get(args[0]);
			if(Files.isDirectory(source))
				source = Paths.get(args[0] + "/" + "info.dat");
			assert Files.isRegularFile(source);

			Path destination = Paths.get(args[1]);
			assert Files.isDirectory(destination);

			cropAll(source, destination.toString());
		}
		else {
			System.err.println("One or two arguments expected");
			System.exit(1);
		}
	}

	private static void cropAll(Path source, String destination) throws IOException {
		UnsafeConsumer<String, IOException> crop = line -> crop(line, destination);

		List<String> lines = Files.readAllLines(source);
		for(int i = 0; i<lines.size(); i++) {
			System.out.printf("Cropping image %d/%d\n", i+1, lines.size());
			crop.accept(lines.get(i));
		}
		System.out.println("Done");
	}

	//line: path n x1 y1 w1 h1 ... xn yn wn hn
	private static void crop(String line, String destination) throws IOException {
		String[] info = line.split("\\s+");
		int n = Integer.parseInt(info[1]);
		if(n == 0)
			return;

		File imagePath = new File(info[0]);
		BufferedImage img = ImageIO.read(imagePath);
		for(int i = 0; i<n; i++) {
			int ind = 4*i + 2;
			int x = Integer.parseInt(info[ind+0]);
			int y = Integer.parseInt(info[ind+1]);
			int w = Integer.parseInt(info[ind+2]);
			int h = Integer.parseInt(info[ind+3]);
			
			BufferedImage selection = img.getSubimage(x, y, w, h);
			String extension = "jpg";
			String selectionPath = destination + "/" + String.format("%s_%d_%d_%dx%d", imagePath.getName(), x, y, w, h) + "." + extension;
			ImageIO.write(selection, extension, new File(selectionPath));
		}
	}
}
