
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.ejml.data.DenseMatrix64F;
import org.ejml.simple.SimpleMatrix;
import org.fastica.*;
import org.fastica.math.Matrix;

public class ImageTestCase {

	static double[][] toMatrix (SimpleMatrix A) {
		double[][] result = new double[A.numRows()][A.numCols()];
		for (int i = 0; i < A.numRows(); ++i) {
			for (int j = 0; j < A.numCols(); ++j) {
				result[i][j] = A.get(i,j);
			}
		}
		return result;
	}

	static void printMatrix (double[][] A) {
		for (int i = 0; i < A.length; ++i) {
			for (int j = 0; j < A[0].length; ++j) {
				System.out.print(A[i][j] + " ");
			}
			System.out.println();
		}
	}

	static void printMatrix(DenseMatrix64F gabor) {
		for (int i = 0; i < gabor.numRows; ++i) {
			for (int j = 0; j < gabor.numCols; ++j) {
				System.out.print(gabor.get(i, j) + " ");
			}
			System.out.println();
		}
	}

	public static Set<Pair<Integer,Integer>> pickRandom(int n, int k1, int k2, int l1, int l2) {
		Random random = new Random(); // if this method is used often, perhaps define random at class level
		Set<Pair<Integer,Integer>> picked = new HashSet<Pair<Integer,Integer>>();
		while(picked.size() < n) {
			Pair<Integer,Integer> p = 
					new Pair<Integer,Integer>(k1 + random.nextInt(k2-k1),
							l1 + random.nextInt(l2-l1));
			picked.add(p);
		}
		return picked;
	}

	public static double calculateStd(SimpleMatrix window) {
		double mean = window.elementSum() / window.getNumElements();
		double var = 0;
		for (int i = 0; i < window.numRows(); ++i) {
			for (int j = 0; j < window.numCols(); ++j) {
				var += Math.pow((window.get(i,j) - mean), 2);
			}
		}
		return Math.sqrt(var/(window.getNumElements()-1));
	}

	public static BufferedImage toImage(double[][] arr) {
		int xLenght = arr.length;
		int yLength = arr[0].length;
		BufferedImage b = new BufferedImage(yLength, xLenght, 3);

		double[] minmax = elementMinMax(arr);
		double min = minmax[0];
		double max = minmax[1];

		for(int x = 0; x < xLenght; x++) {
			for(int y = 0; y < yLength; y++) {
				double color = arr[x][y];
				color = (255 * (color - min) / (max - min));
				Color col = new Color((int)color, (int)color, (int)color);
				b.setRGB(y, x, col.getRGB());
				//		        System.out.println(color + " > " + col.getRGB() + " > " + b.getRGB(y, x));
			}
		}

		BufferedImage resizedImage = new BufferedImage(200, 200, 3);
		Graphics2D g = resizedImage.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(b, 0, 0, 100, 100, null);
		g.dispose();	

		return resizedImage;
	}

	private static double[] elementMinMax(double[][] image) {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		for (int i = 0; i < image[0].length; ++i) {
			for (int j = 0; j < image.length; ++j) {
				double curr = image[i][j];
				if (min > curr)
					min = curr;
				if (curr > max)
					max = curr;
			}
		}
		return (new double[] {min, max});
	}

	
	
	// MAIN
	public void run() throws Exception {

		// This image contains the rgb (0-255) values of grasshopper image generated in matlab
		File file = new File("img.txt");

		Scanner scanner = new Scanner(file);
		int dim1 = scanner.nextInt();
		int dim2 = scanner.nextInt();

		double[][] A = new double[dim1][dim2];
		while (scanner.hasNextInt()) {
			for (int i = 0; i < dim1; ++i) {
				for (int j = 0; j < dim2; ++j) {
					if (scanner.hasNextInt()) {
						A[i][j] = scanner.nextInt();
					}
				}
			}
		}

		scanner.close();

		SimpleMatrix image = new SimpleMatrix(A);

		// Generate patches
		int patch_size = 8;
		int numPatches = 5000;
		// int numPatches = 200;
		int numMaxPossiblePatches = (image.numCols()-patch_size)*(image.numRows()-patch_size);
		int numMaxPatches = (numPatches <= numMaxPossiblePatches) ? numPatches : numMaxPossiblePatches;
		int numTries = numPatches*2;
		int numMaxTries = (numTries <= numMaxPossiblePatches) ? numTries : numMaxPossiblePatches;

		SimpleMatrix image_patches = new SimpleMatrix(patch_size*patch_size, numMaxPatches);

		Set<Pair<Integer,Integer>> indices = pickRandom(numMaxTries, 1, image.numRows()-patch_size,
				1, image.numCols()-patch_size);

		Iterator<Pair<Integer, Integer>> iterator = indices.iterator();

		int cnt = 0;
		while (iterator.hasNext() && cnt < numMaxPatches) {
			Pair<Integer,Integer> p = iterator.next();
			int x = p.getLeft(), y = p.getRight();
			SimpleMatrix window = image.extractMatrix(x, x+patch_size, y, y+patch_size);
			double std = calculateStd(window);
			if (std > 0) {
				window.reshape(patch_size*patch_size, 1);
				image_patches.insertIntoThis(0, cnt++, window);
			}
		}		

		
		// This is ICA
		int ica_comp = 40; // components we want to use
		double[][] X = toMatrix(image_patches);

		System.out.println("processing ICA...");
		long startTime = System.currentTimeMillis();

		// ICA main method
		FastICA ica = new FastICA(X, ica_comp);

		long endTime = System.currentTimeMillis(); 
		System.out.println("Time ICA: " + (endTime - startTime)/1000 + "s");

		double[][] sep = ica.getSeparatingMatrix();
		double[][] icaMatrix = sep.clone();
		System.out.println("Separating matrix: " + sep.length + " x " + sep[0].length + "\n" +  Matrix.toString(sep));



		// VISUALIZATION
		int showing = 16;
		BufferedImage[] images = new BufferedImage[showing];
		double[][] column = new double[patch_size][patch_size];
		for (int c = 0; c < showing; c++) {
			for (int i = 0, cntr = 0; i < patch_size; i++) {
				for (int j = 0; j < patch_size; j++) {
					column[i][j] = icaMatrix[c][cntr];
					cntr++;
				}
			}
			images[c] = toImage(column);
		}

		JPanel  panel = new JPanel ();
		for (int i = 0; i < showing; i++) 
			panel.add(new JLabel (new ImageIcon (images[i])));

		JFrame frame = new JFrame ("ICA results.");
		frame.getContentPane().add(panel);
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 

	}

}
