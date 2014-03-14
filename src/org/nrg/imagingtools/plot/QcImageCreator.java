package org.nrg.imagingtools.plot;

import ij.ImagePlus;

import java.io.File;
import java.util.Hashtable;

import org.nrg.plexiViewer.io.PlexiFileOpener;
import org.nrg.plexiViewer.lite.io.PlexiFileSaver;
import org.nrg.plexiViewer.lite.io.PlexiImageFile;
import org.nrg.plexiViewer.utils.ImageUtils;
import org.nrg.plexiViewer.utils.LUTApplier;
import org.nrg.plexiViewer.utils.PlexiConstants;
import org.nrg.plexiViewer.utils.Transform.PlexiMontageMaker;


public class QcImageCreator {
	String sessionId =null;
	String inFile=null;
	String outDir =null;
	String lut=null;
	String format;
	String suffix;
	String min;
	String max;
	boolean addColorBar = false;
	public QcImageCreator(String args[]) {
        for(int i=0; i<args.length; i++){
        	if (args[i].equalsIgnoreCase("-sessionLabel") ) {
                if (i+1 < args.length) {
                    sessionId=args[i+1];
                }
            }else  if (args[i].equalsIgnoreCase("-out") ) {
                if (i+1 < args.length) {
                    outDir=args[i+1];
                }
            }else  if (args[i].equalsIgnoreCase("-in") ) {
                if (i+1 < args.length) {
                    inFile=args[i+1];
                }
            }else  if (args[i].equalsIgnoreCase("-lut") ) {
                if (i+1 < args.length) {
                    lut=args[i+1];
                }
            }else  if (args[i].equalsIgnoreCase("-format") ) {
                if (i+1 < args.length) {
                    format=args[i+1];
                }
	        }else  if (args[i].equalsIgnoreCase("-suffix") ) {
	            if (i+1 < args.length) {
	                suffix=args[i+1];
	            }
        }else  if (args[i].equalsIgnoreCase("-min") ) {
            if (i+1 < args.length) {
                min=args[i+1];
            }
    }else  if (args[i].equalsIgnoreCase("-max") ) {
        if (i+1 < args.length) {
            max=args[i+1];
        }
    }else  if (args[i].equalsIgnoreCase("-addColorBar") ) {
    	addColorBar = true;
    }

        }
        if (sessionId == null || outDir == null || inFile==null || suffix == null) {
            printUsage();
        	handleError("ERROR: Insufficient Arguments ");
        }
    }

    private void handleError(String msg) {
        System.out.println(msg);
        System.exit(1);
    }
    
    protected void printUsage() {
        System.out.println("QCImageCreator OPTIONS:"); 
        System.out.println("\t\t-sessionLabel <Session Label>");
        System.out.println("\t\t-out <Path to out file>");
        System.out.println("\t\t-in <Path to 4dfp image>");
        System.out.println("\t\t-lut <Path to LUT file>");
        System.out.println("\t\t-format <Image Format>");
        System.out.println("\t\t-min <Min Value>");
        System.out.println("\t\t-max <Max Value>");
        System.out.println("\t\t-addColorBar");        
    }  

    private void setMinValueToZero(ImagePlus img) {
        double max = img.getProcessor().getMax();
        setValue(img,0,max);
    }
    
    private void setValue(ImagePlus img, double min, double max) {
        img.getProcessor().setMinAndMax(min,max);
    }
    
    public void createQCImage() throws Exception{
    	File imgFile = new File(inFile);
        PlexiImageFile pf = new PlexiImageFile();
        pf.setURIAsString(imgFile.toURI().toString());
        pf.setPath(imgFile.getParent());
        pf.setName(imgFile.getName());
        pf.setFormat(format);
        pf.setXsiType(PlexiConstants.PLEXI_IMAGERESOURCE);
        ImagePlus petimg = PlexiFileOpener.openBaseFile(pf, false);
        //setMinValueToZero(petimg);
        if (min != null && max != null) {
        	setValue(petimg, Double.parseDouble(min),Double.parseDouble(max));
        }
       
        if (lut != null) {
            LUTApplier lutApplier = new LUTApplier(lut);
            petimg = lutApplier.applyLUT(petimg);
        }
        createMontage(outDir+File.separator+sessionId, petimg);
        
    }
 
    private void createMontage(String rootfilename,  ImagePlus petimage) {
        Hashtable attribs = ImageUtils.getSliceIncrement(petimage, 24);
        int cols = 6; int rows = 4; 
        int increment = ((Integer)attribs.get("increment")).intValue();
        //int increment = 11;
        //int startslice = 1;
        int startslice = ((Integer)attribs.get("startslice")).intValue();
        int endslice = ((Integer)attribs.get("endslice")).intValue();
        for (int i = 0; i < rows; i++) {
            int end = startslice + (cols - 1)*increment ;
            createJpeg(rootfilename + "_" + suffix+ i + "_t.jpg", petimage,cols,startslice,end,increment);
            startslice = end + increment;
        }
       
    }
    
    private void createJpeg(String filename, ImagePlus img, int cols, int startslice, int endslice, int increment ) {
        PlexiMontageMaker mm = new PlexiMontageMaker();
        ImagePlus montage = mm.makeMontage(img,cols,1,1,startslice,endslice,increment,true,false);
        if (addColorBar) {
        	//add an extra width at the edge of the image 
        	//Fill with black
        	//Add the color bar
        	CalibrationBar calibration;
        	if (max != null && min != null)
        		calibration = new CalibrationBar(montage, Double.parseDouble(min), Double.parseDouble(max));
        	else
        		calibration = new CalibrationBar(montage);
        	montage = calibration.insertColorBar();
        }
        PlexiFileSaver fs =  new PlexiFileSaver();
        //fs.saveAsJpeg(img,filename+".jpg", 100);
        //StackProcessor tbproc = new StackProcessor(montage.getStack(), montage.getProcessor());
        //ImageStack tb = tbproc.resize((int)montage.getWidth()/2,(int)montage.getHeight()/2);
        //montage.setStack("",tb);    
        fs =  new PlexiFileSaver();
        fs.saveAsJpeg(montage,filename, 100);
    }

    public static void main(String[] args){
    	try {
	    	QcImageCreator imageCreator = new QcImageCreator(args);
	    	imageCreator.createQCImage();
	    	System.out.println("Created Images");
	    	//System.exit(0);
    	}catch(Exception e) {
    		e.printStackTrace();
    		System.exit(1);
    	}
    }
    
}
