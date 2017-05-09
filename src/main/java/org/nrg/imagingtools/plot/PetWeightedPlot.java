package org.nrg.imagingtools.plot;


import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.Properties;
import java.util.Scanner;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class PetWeightedPlot {
	File config = null;
	String sessionId =null;
	String inPath=null;
	String outDir =null;
	final String SEPARATOR = "/";
    public PetWeightedPlot(String args[]) {
        for(int i=0; i<args.length; i++){
            if (args[i].equalsIgnoreCase("-config") ) {
                if (i+1 < args.length) {
                    config = new File(args[i+1]);
                }
            }else  if (args[i].equalsIgnoreCase("-sessionLabel") ) {
                if (i+1 < args.length) {
                    sessionId=args[i+1];
                }
            }else  if (args[i].equalsIgnoreCase("-out") ) {
                if (i+1 < args.length) {
                    outDir=args[i+1];
                }
            }else  if (args[i].equalsIgnoreCase("-in") ) {
                if (i+1 < args.length) {
                    inPath=args[i+1];
                }
            }
        }
        if (sessionId == null || config == null || outDir == null || inPath==null ) {
            printUsage();
        	handleError("ERROR: Insufficient Arguments ");
        }
    }

    private void handleError(String msg) {
        System.out.println(msg);
        System.exit(1);
    }
    
    protected void printUsage() {
        System.out.println("PetWeightedPlot OPTIONS:"); 
        System.out.println("\t\t-config <Path to property file>");
        System.out.println("\t\t-sessionLabel <Session Label>");
        System.out.println("\t\t-out <Path to out file>");
        System.out.println("\t\t-in <Root path to tac file>");
    }

    private XYSeries createWeightedAverage(String roi, String[] roi_file) {
    	XYSeries series = new XYSeries(roi);
        int rowLength = 100;
    	int[] nvoxels = new int[roi_file.length];
    	double[][] rawMeanValues = new double[roi_file.length][rowLength];
       	double[] start_time = new double[rowLength];
       	double[] duration = new double[rowLength];
       	int lineCount=0;
    	for (int i=0; i< roi_file.length; i++) {
    		String filename =inPath+SEPARATOR+sessionId + "_" + roi_file[i]+".tac"; 
    		File f = new File(filename);
    		System.out.println("Parsing file for region : " + roi + " " + filename);
	    	Scanner scanner =null;
    		try {
    	    	//first use a Scanner to get each line
    	    scanner = new Scanner(new FileReader(f));
    			//Need to skip first  lines	
    	      lineCount=0; 	
    	      while ( scanner.hasNextLine() ){
	        	  String line = scanner.nextLine();
    	    	  if (lineCount==0) { 
    	        		String[] cols = line.split("NVoxels=");
    	        		Integer nVoxel = new Integer(cols[1].trim());
    	        		nvoxels[i] = nVoxel.intValue();
    	          }else { //Data lines
    	        		String[] cols = line.trim().split("\\s+");
   	        			start_time[lineCount-1] = new Double(cols[1].trim()).doubleValue();
   	        			duration[lineCount-1] = new Double(cols[2].trim()).doubleValue();
    	        		rawMeanValues[i][lineCount-1] = new Double(cols[3].trim()).doubleValue();
    	        		//System.out.println(cols.length + " " + start_time[lineCount-1] + " " + duration[lineCount-1] + " " + rawMeanValues[i][lineCount-1]);
    	          }
     	        lineCount++;
    	      }
    	    }catch(Exception e) {
    	    	e.printStackTrace();
    	    }finally {
    	      //ensure the underlying stream is always closed
    	      //this only has any effect if the item passed to the Scanner
    	      //constructor implements Closeable (which it does in this case).
    	      if (scanner != null)scanner.close();
    	    }
    	    rowLength = lineCount-1;
    	}
//    	System.out.println("Number of rows in each file " + rowLength);
    	//Compute weighted average    	
    	//The combined region TAC is a weighted average of the individual regions by the voxel number.
    	for (int j=0; j< rowLength;j++) {
     		double num_sum =0;
    		double den_sum =0;
    		double weighted_mean =0;
	    	for (int i=0; i< roi_file.length; i++) {
	    		num_sum += nvoxels[i]*rawMeanValues[i][j] ;
	    		den_sum += nvoxels[i];
	    	}
	    	weighted_mean=num_sum/den_sum;
//	    	System.out.println(j + " " + start_time[j] + " " + duration[j] + " " + (start_time[j]+duration[j]/2)+" " + weighted_mean);
    		series.add(start_time[j]+duration[j]/2,weighted_mean);
    	}
    	return series;
    }
    
    private void plotAveragedRegions() {
        Properties properties = new Properties();
        XYSeriesCollection dataSet = new XYSeriesCollection();
        try { 
                properties.load(new FileInputStream(config));
                String rois_csv_list = properties.getProperty("REGIONS");
                String[] rois = rois_csv_list.split(",");
           		String[] colors = new String[rois.length];
           	    for (int i=0; i<rois.length; i++) {
                	String roi_name = rois[i].trim();
                	String roi_files = properties.getProperty(roi_name+"_FILES_SUFFIX");
                	String colorStr=properties.getProperty(roi_name+"_COLOR");
                	String[] roi_file = roi_files.trim().split(",");
                	XYSeries series = createWeightedAverage(roi_name,roi_file);
                	dataSet.addSeries(series);
                    colors[i] = colorStr;
                }
                JFreeChart chart = ChartFactory.createXYLineChart
		        (sessionId,  // Title
		         "Time(sec)",           // X-Axis label
		         "Combined Region Activity",           // Y-Axis label
		         dataSet,          // Dataset
		         PlotOrientation.VERTICAL,
		         true,                // Show legend
		         true,
		         false
		        );
				chart.setBorderPaint(Color.WHITE);
				chart.getLegend().setBackgroundPaint(new Color(248,248,255));
				XYPlot plot = (XYPlot) chart.getPlot();
				plot.setBackgroundPaint(new Color(248,248,255));

				XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer)(plot).getRenderer();
				renderer.setItemLabelsVisible(true);
				for (int i=0; i<rois.length; i++) {
					Color seriesColor = getColor(colors[i]);
                	renderer.setSeriesPaint(i,seriesColor);
				}
				plot.setRenderer(renderer);
				String fileName = outDir + SEPARATOR + sessionId + "_"+ "roi_activity_plot.jpg";
                File plotFile = new File(fileName);
                ChartUtilities.saveChartAsJPEG(plotFile, chart, 500, 300);
                System.out.println("Created file " + fileName);
         }catch(Exception e) {
        	e.printStackTrace();
        	handleError(e.getLocalizedMessage());
        }
    }
    
    private Color getColor(String colonSeparatedRGB) {
        String[] rgb = colonSeparatedRGB.split(":");
        return new Color(Integer.parseInt(rgb[0]),Integer.parseInt(rgb[1]),Integer.parseInt(rgb[2]) );
    }
    
    public static void main(String args[]) {
    	PetWeightedPlot petPlot = new PetWeightedPlot(args);
    	petPlot.plotAveragedRegions();
    	System.out.println("Done");
    	System.exit(0);
    }

}
