package ogallagher.marketsense.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javafx.geometry.Point2D;

public abstract class PointFilter implements Callable<List<Point2D>> {
	protected List<Point2D> input = new ArrayList<>();
	protected double minX, maxX, graphWidth, minY, maxY, graphHeight;
	
	public PointFilter() {
		this(0,0,0,0,0,0);
	}
	
	/**
	 * @param x Min x.
	 * @param X Max x.
	 * @param y Min y.
	 * @param Y Max y.
	 * @param w Graph width.
	 * @param h Graph height.
	 */
	public PointFilter(double x, double X, double y, double Y, double w, double h) {
		minX = x;
		maxX = X;
		minY = y;
		maxY = Y;
		graphWidth = w;
		graphHeight = h;
	}
	
	public void setInput(List<Point2D> input) {
		this.input.clear();
		this.input.addAll(input);
	}
	
	public void setInput(Point2D input) {
		this.input.clear();
		this.input.add(input);
	}
	
	public double getMinX() {
		return minX;
	}
	
	public double getMaxX() {
		return maxX;
	}
	
	public double getMinY() {
		return minY;
	}
	
	public double getMaxY() {
		return maxY;
	}
	
	public double getGraphWidth() {
		return graphWidth;
	}
	
	public double getGraphHeight() {
		return graphHeight;
	}
	
	public List<Point2D> call() throws Exception {
		throw new UnsupportedOperationException("must be defined by implementing class");
	}
}
