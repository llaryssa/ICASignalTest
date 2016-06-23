import java.util.Random;

import javax.swing.JFrame;

import org.JMathStudio.DataStructure.Vector.*;
import org.JMathStudio.DataStructure.Iterator.Iterator1D.VectorIterator;
import org.JMathStudio.SignalToolkit.Utilities.SignalGenerator;

import org.fastica.FastICA;
import org.fastica.math.Matrix;

import org.math.plot.*;

public class TestCase {
	
	double[][] source;  //matrix input signal
	double[][] mixing;  //matrix for mixing array
	double[][] X;  //matrix for mixed sources

	double[][] mix;  //matrix for output of ICA mixing array
	double[][] sep;  //matrix for output of ICA separating matrix

	public void setUpSource() throws Exception {
		int sampleNumber = 10000;

		Random rand = new Random();

		Vector s1 = SignalGenerator.sine(0.01f, sampleNumber, 0);
		Vector s2 = SignalGenerator.sawTooth(0.03f, sampleNumber);

		source = new double[2][s1.length()];

		VectorIterator i1 = new VectorIterator(s1);
		VectorIterator i2 = new VectorIterator(s2);

		for (int i =0; i < s1.length(); i ++) {
			source[0][i] = (double)i1.getAndNext() + rand.nextDouble()/10;
			source[1][i] = (double)i2.getAndNext() + rand.nextDouble()/10;
		}
	}


	public void setUpMixingMatrix() throws Exception {
		mixing = new double[2][2];
		mixing[0][0] = 1d;
		mixing[0][1] = 1d;
		mixing[1][0] = 1d;
		mixing[1][1] = 2d;
		mixing = Matrix.transpose(mixing); 

	}

	// Works only w/ 2x2 matrices
	public double[][] inverse(double[][] A) {
		double[][] result = new double[2][2];
		double det = A[0][0]*A[1][1] - A[0][1]*A[1][0];
		result[0][0] = (1/det)*A[1][1];
		result[0][1] = -(1/det)*A[0][1];
		result[1][0] = -(1/det)*A[1][0];
		result[1][1] = (1/det)*A[0][0];
		return result;
	}

	public void plotResults() {
		int n_samples = 200;

		double[][] X_sampled = new double[X.length][n_samples];
		double[][] source_sampled = new double[source.length][n_samples];
		for (int i = 0; i < source_sampled.length; i++) {
			for (int j = 0; j < source_sampled[0].length; j++) {
				source_sampled[i][j] = source[i][j];
				X_sampled[i][j] = X[i][j];
			}
		}

		double[] x = new double[source_sampled[0].length];
		for (int i = 0; i < source_sampled[0].length; ++i) x[i] = i;

		Plot2DPanel plot1 = new Plot2DPanel();
		plot1.addLinePlot("First signal", x, source_sampled[0]);
		plot1.addLinePlot("Second signal", x, source_sampled[1]);

		double[][] source_hat = Matrix.mult(inverse(mix), X);

		Plot2DPanel plot2 = new Plot2DPanel();
		plot2.addLinePlot("First recovered signal", x, source_hat[0]);
		plot2.addLinePlot("Second recovered signal", x, source_hat[1]);

		JFrame frame1 = new JFrame("Original signals");
		frame1.setContentPane(plot1);
		frame1.setVisible(true);

		JFrame frame2 = new JFrame("Recovered signals");
		frame2.setContentPane(plot2);
		frame2.setVisible(true);
	}

	public void testFastICA() throws Exception{	
		X = Matrix.mult(mixing, source);
		FastICA ica = new FastICA(X, 2);
		mix = ica.getMixingMatrix();
		sep = ica.getSeparatingMatrix();
	}

	public static void main(String[] args) throws Exception {
		
		// For signal testing, the output is 2 plots
		// One of them is the original sources, and the other is the recovered signal
		// The test contain a little noise, so if the algorithm is good, it should return
		// good recovered signals.
		// For the test with images, the returned images are the ICA components
		// To be a good result, they should look like Gabor Filters.
		
//		// Uncomment this part to test ICA with signals (sine wave and sawtooth)
//		TestCase tc = new TestCase();
//
//		tc.setUpSource();
//		tc.setUpMixingMatrix();
//		tc.testFastICA();
//		tc.plotResults();
	
		// Uncomment this part to test ICA with the grasshopper image
		ImageTestCase itc = new ImageTestCase();
		itc.run();
		
		System.out.println("Test done.");
	}
}
