//Alex Kunze Susemihl: 2012/01/31
//Program to evaluate marginalization on Bayes' networks
//Network structure is specified by user, conditioning variables are given also
//Gibbs sampling is then used to obtain the marginal over the non-conditioned variables

package applet;


import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.applet.*;

import javax.imageio.ImageIO;


public class ImageApplet extends Applet {

	private static final long serialVersionUID = 1L;		


	int width, height;

	static Graph<String, DefaultEdge> myGraph;
	
	Hashtable<String, Integer> state;
	Hashtable<String, Integer> counts;
	Hashtable<String, Double> biases;
	Hashtable<DefaultEdge, Double> interactions;
	
	Graphics mygraphics;
	Button startsample = new Button("Start Gibbs sampler");
	Button startbp = new Button("Start loopy Belief Propagation");
	Label iteration = new Label();
	Label status = new Label();
	Label summary = new Label();
	Label error_txt = new Label();
	Label image_text = new Label();
	Button loadimage = new Button("Load Image from URL");
	TextField image_url = new TextField();
	CheckboxGroup method = new CheckboxGroup();
	Checkbox gibbs;
	Checkbox loopybp;
	private BufferedImage original_img;
	private static BufferedImage img;
	private ArrayList<String> vertices = new ArrayList<String>();
	private ArrayList<String> edges = new ArrayList<String>();
	private ArrayList<String> clamped = new ArrayList<String>();

	private static BufferedImage toGray(BufferedImage original,Double noise) {
		 
	    int alpha, red, green, blue;
	    int newPixel;
        Color black = new Color(0,0,0);
        Color white = new Color(255,255,255);
	    Random rng = new Random();
	    
	    BufferedImage lum = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());

	    int threshold = 128;
	    
	    for(int i=0; i<original.getWidth(); i++) {
	        for(int j=0; j<original.getHeight(); j++) {
	 
	            // Get pixels by R, G, B
	            alpha = new Color(original.getRGB(i, j)).getAlpha();
	            red = new Color(original.getRGB(i, j)).getRed();
	            green = new Color(original.getRGB(i, j)).getGreen();
	            blue = new Color(original.getRGB(i, j)).getBlue();
	 
	            red = (int) (0.21 * red + 0.71 * green + 0.07 * blue);
	            //thresholding
	            double a = rng.nextDouble();
	            if(red>threshold){
		            newPixel = white.getRGB();
		            if(a<noise) newPixel = black.getRGB();
	            }
	            else{
	            	newPixel = black.getRGB();
	            	if(a<noise) newPixel = white.getRGB();
	            }
	            // Return back to original format
	 
	            // Write pixels into image
	            lum.setRGB(i, j, newPixel);
	 
	        }
	    }
	 //thresholding
	    
	    return lum;
	 
	}
	
	public void init() {
		setLayout(null);

		width = getSize().width;
		height = getSize().height;

		image_text.setBounds(10,10,600,40);
		image_url.setBounds(10,60,600,30);
		loadimage.setBounds(10,90,600,40);
		gibbs = new Checkbox("Gibbs Sampling",method,false);
		loopybp = new Checkbox("Loopy BP",method,true);
		gibbs.setBounds(620,10,200,20);
		loopybp.setBounds(620,40,200,20);
		add(image_text);
		add(loadimage);
		add(image_url);
		add(loopybp);
		add(gibbs);
		image_url.setText("http://profile.ak.fbcdn.net/hprofile-ak-snc4/372056_1545934353_830404459_q.jpg");
		image_text.setText("Enter the URL of your image here. Images will be rescaled to 4:3.\nChoose your method from the checkbox and have fun.");
		
		mygraphics = this.getGraphics();
		paint(mygraphics);
		
	}
	
	public void getGraphFromImage(BufferedImage img,double beta){
		int width = img.getWidth(this);
		int height = img.getHeight(this);
		for(int i=0;i<width;i++){
			for(int j=0;j<height;j++){
		        //Only red level is used, since images are grayscaled 
				int red = new Color(img.getRGB(i, j)).getRed();
		        double bias = -0.5+(double)red/255;
		        vertices.add("pixel_"+i+"_"+j+":"+beta*bias);
		        if(i+1<width){
		        	double interaction = 1.0;
		        	edges.add("pixel_"+i+"_"+j+"-pixel_"+(i+1)+"_"+j+":"+beta*interaction);
		        }		       
		        if(j+1<height){
		        	double interaction = 1.0;
		        	edges.add("pixel_"+i+"_"+j+"-pixel_"+i+"_"+(j+1)+":"+beta*interaction);
		        }		        
			}
		}
	}
	static BufferedImage getImageFromBeliefs(Hashtable<String,Double> beliefs) {
		 ColorModel cm = img.getColorModel();
		 Color white = new Color(255,255,255);
		 Color black = new Color(0,0,0);
		 int newPixel;
		 boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		 WritableRaster raster = img.copyData(null);
		 BufferedImage reconstructed = new BufferedImage(cm, raster, isAlphaPremultiplied, null);
		 for(Enumeration<String> e = beliefs.keys();e.hasMoreElements();){
			String s = e.nextElement();
			String[] pix = s.split("_");
			int x = Integer.parseInt(pix[1]);
			int y = Integer.parseInt(pix[2]);
			if(beliefs.get(s)>0.5){
				newPixel = white.getRGB();
				reconstructed.setRGB(x,y,newPixel);
			}
			else{
				newPixel = black.getRGB();
				reconstructed.setRGB(x,y,newPixel);
			}
		 }
		 return reconstructed;
	}
	
	public boolean action(Event event, Object arg){
		if(event.target==loadimage){
			try {
				   URL url = new URL(image_url.getText());
				   System.out.println(url.toString());
				   original_img = ImageIO.read(url);
				   img = toGray(original_img,0.2);
			}
			catch (IOException e) {
				
			}
			if(img.getHeight()>300||img.getWidth()>400){
				height = 150;
				width = 200;
			}
			else{
				height = img.getHeight();
				width = img.getWidth();
			}
			this.getGraphics().drawImage(original_img, 10,130,width,height,this);
			this.getGraphics().drawImage(img,10,130+height+10,width,height,this);
			getGraphFromImage(img,100.0);
			if(method.getSelectedCheckbox()==loopybp){
				LoopyBP loopyBP = new LoopyBP(vertices,edges,clamped,summary,error_txt,status,iteration);
				System.out.println("Created loopybp");
				loopyBP.run(100);
				Hashtable<String,Double> beliefs = loopyBP.getPlusBeliefs();
				BufferedImage reconstructed_img = getImageFromBeliefs(beliefs);
				this.getGraphics().drawImage(reconstructed_img,10+width+10,130,width,height,this);
			}
			else{
				GibbsSampler gibbs = new GibbsSampler(vertices,edges,clamped,summary,error_txt,status,iteration);
				System.out.println("Created gibbs sampler");
				gibbs.run(100);
				Hashtable<String,Double> frequency= gibbs.getFrequency();
				BufferedImage reconstructed_img = getImageFromBeliefs(frequency);
				this.getGraphics().drawImage(reconstructed_img,10+width+10,130,width,height,this);
			}
			System.out.println("Should be done");
		}
		
		return false;
	}	

}
