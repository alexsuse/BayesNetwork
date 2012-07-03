
import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextAttribute;
import java.lang.Math.*;
import java.text.AttributedString;
import java.util.*;
import java.text.*;
import javax.swing.*;

// import Jama.*;


public class BayesianNetworkGibbs extends Applet{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;		
	
		
	int imagewidth = 540;
	int imageheight = 245;
	Dimension dim = new Dimension(imagewidth,imageheight);
	Dimension dim2 = new Dimension(320,485);
	Image lgraph;
	Image t2graph;
	Image bnmodel;
	
	Graphics offGraphics_l;
	Graphics offGraphics_t2;
	Graphics offGraphics_bn;
	Graphics myGraphics;
	
	AttributedString tmphist;
	
	
	final int itnumber = 10000;
	final int startx = 0;
	final int starty = 0;
	//state vector: A,L,T_1,T_2
	int[] state = new int[4];
	int[][] statemat = new int[itnumber][4]; 
	double[] lratio = new double[itnumber];
	double[] t2ratio = new double[itnumber];
	int lctr;
	int t2ctr;
	Random rndsampler = new Random();
	double rtmp;
	double pl_current;
	int ly;
	int pointctr;
	int current_ly;
	int last_ly;
	int current_t2y;
	int last_t2y;
	int bn_mid;
	int lastt2;
	int lastl;
	int sleepms;
	
	//network probabilities, prob for being true
	//in vector: 1st for condvar=F, 2nd condvar=T
	double p_a; 
	double[] p_l = new double[2];
	double[] p_t1 = new double[2];
	double[] p_t2 = new double[2];
	//vector for unnormailzed t2 prob given its blanket
	double[] p_lblanket = new double[2];
	
	//true desired probs
	double[] l_truevec = new double[2];
	double l_true;
	double[] t2_truevec = new double[2];
	double t2_true;
	
	Button reset = new Button("Reset");
	Button startgibbs = new Button("Start");
	Label iter = new Label();
	Label mbprobs = new Label();
	JSlider sleeptime = new JSlider();
	
		
	
	public int drawRndNumber(double trueprob){
		rtmp = rndsampler.nextDouble();
		if(rtmp < trueprob)
			return (1);
		else
			return (0);
	}
	
	
	public void init(){
	  
		setLayout(null);
		myGraphics = this.getGraphics();			
			
		
		state[0] = 1;
		state[1] = drawRndNumber(0.5);
		System.out.println(state[1]);
		state[2] = 1;		
		state[3] = drawRndNumber(0.5);
		
		for(int i=0;i<4;i++)
			statemat[0][i] = state[i];
		
		p_a = 0.75;
		p_l[0] =  0.0001;
		p_l[1] =  0.01;
		p_t1[0] =  0.01;
		p_t1[1] =  0.9;
		p_t2[0] =  0.05;
		p_t2[1] =  0.95;
		
		bn_mid = 150;
		lctr = 0;
		t2ctr = 0;
		
		startgibbs.setBounds(dim.width/2-25,520,50,30);
		reset.setBounds(dim.width/2+40,520,50,30);
		iter.setBounds(10,290,90,30);
		mbprobs.setBounds(10,320,280,30);		
		
		sleeptime.setBounds(10,505,150,60);
		//sleeptime.setVisible(false);
		sleeptime.setMinimum(0);    
		sleeptime.setMaximum(1000);  
		sleeptime.setValue(800);
		//sleeptime.setMinorTickSpacing(10);
		sleeptime.setInverted(true);
		sleeptime.setPaintTicks(true);    
		sleeptime.setPaintLabels(true);
		sleeptime.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent e) {
				sleepms = sleeptime.getValue();
			}
		});
				
		Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
		labelTable.put( new Integer( 0 ), new JLabel("Fast") );
		labelTable.put( new Integer( 1000 ), new JLabel("Slow") );
		sleeptime.setLabelTable(labelTable );
		
		add(startgibbs);
		add(reset);
		add(iter);
		add(mbprobs);
		add(sleeptime);
	}
	
	
	public boolean action(Event evt, Object arg){			
		
		sleepms = sleeptime.getValue();
		
		if(evt.target instanceof JSlider)
			System.out.println("State changed");			
		
		if(evt.target == reset){
			iter.setText("");			
			mbprobs.setText("");
			init();
			offGraphics_l.setColor(Color.white);
			offGraphics_l.fillRect(startx, starty, dim.width, dim.height);
			offGraphics_l.setColor(Color.black);
			offGraphics_t2.setColor(Color.white);
			offGraphics_t2.fillRect(startx, starty, dim.width, dim.height);
			offGraphics_t2.setColor(Color.black);
			offGraphics_bn.setColor(Color.white);
			offGraphics_bn.fillRect(startx, starty, dim2.width, dim2.height);
			offGraphics_bn.setColor(Color.black);
			paint_graph(myGraphics,0);
			paint_model(myGraphics,0);
			
		}
		
		if(evt.target == startgibbs){
		
			l_truevec[0] = (1-p_l[1])*p_t1[0];
			l_truevec[1] = p_l[1]*p_t1[1];
			l_true = l_truevec[1]/(l_truevec[0] + l_truevec[1]);

			t2_truevec[0] = (1-p_t2[0])*p_t1[0]*(1-p_l[1])+((1-p_t2[1])*p_t1[1]*p_l[1]);
			t2_truevec[1] = p_t2[0]*p_t1[0]*(1-p_l[1])+(p_t2[1]*p_t1[1]*p_l[1]);
			t2_true = t2_truevec[1]/(t2_truevec[0] + t2_truevec[1]);
		
			//sleeptime.setVisible(true);
			mypaint_model(0);
			paint_model(myGraphics,0);
			
			for (int it=0; it<itnumber; it++){
				
				iter.setText("Iteration: "+Integer.toString(it+1));							
				mbprobs.setText("Sample from P(T2|mb(T2))= ("+Double.toString(Math.round( (1-p_t2[state[1]]) * 100. ) / 100.)+" , "+Double.toString(Math.round(p_t2[state[1]] * 100. ) / 100.)+")");
				try {
					if(it<25)
						Thread.sleep(sleepms);
					} catch (InterruptedException e){					}
				
				lastt2 = state[3];
				//sample new t2 value
				state[3] = drawRndNumber(p_t2[state[1]]);
				//if state switches
				if(lastt2 != state[3]){
					mypaint_model(it);
					paint_model(myGraphics,it);
				}
				
				//sample new l value
				p_lblanket[0] = (1-p_l[state[0]])*p_t1[0]*(state[3]*p_t2[0]+(1-state[3])*(1-p_t2[0]));
				p_lblanket[1] = p_l[state[0]]*p_t1[1]*(state[3]*p_t2[1]+(1-state[3])*(1-p_t2[1]));
				
				pl_current = p_lblanket[1]/(p_lblanket[0]+p_lblanket[1]);
				mbprobs.setText("Sample from P(L|mb(L))= ("+Double.toString(Math.round( (1-pl_current) * 10000. ) / 10000.)+" , "+Double.toString(Math.round(pl_current * 10000. ) / 10000.)+")");
				try {
					if(it<25)
						Thread.sleep(sleepms);
					} catch (InterruptedException e){					}
									
				lastl = state[1];
				state[1] = drawRndNumber(pl_current);
				
				if(lastl != state[1]){
					mypaint_model(it);
					paint_model(myGraphics,it);
				}
							
			
				lctr = lctr + state[1];
				t2ctr = t2ctr + state[3];
 			
				lratio[it] = (double) lctr/(it+1);
				t2ratio[it] = (double) t2ctr/(it+1);
 			    
				for(int j=0;j<4;j++)
					statemat[it][j] = state[j];
				
				mypaint_states(it);
				paint_model(myGraphics,it);
				
				if(it<=25 || (it>25 && (it+1)%10==0)){
					mypaint_graph(it);
					paint_graph(myGraphics,it);
				}
					
				/*System.out.println("L-ratio at "+it+" Iteration: "+lratio[it]);
				System.out.println("T2-ratio: "+t2ratio[it]);
				System.out.println("");
				*/
			}
			
			return(true);
			
		}		
		return(false);       
		
	}	
	public void start(){
		 
		 lgraph = createImage(dim.width,dim.height);
		 offGraphics_l = lgraph.getGraphics();
		 t2graph = createImage(dim.width,dim.height);
		 offGraphics_t2 = t2graph.getGraphics();
		 bnmodel = createImage(dim2.width,dim2.height);
		 offGraphics_bn = bnmodel.getGraphics();

				 
	  	 paint_model(myGraphics,0);
	}
	
	public void mypaint_states(int iteration){
		
		offGraphics_bn.setColor(Color.white);
		offGraphics_bn.fillRect(10, 360, 200, 130);
		offGraphics_bn.setColor(Color.black);
		
		int maxit = Math.min(iteration, 4);
		
		offGraphics_bn.drawLine(10, 380, 200, 380);
		offGraphics_bn.drawString("Iteration", 20, 370);
		offGraphics_bn.drawString("A", 82, 370);
		offGraphics_bn.drawString("L", 112, 370);
		tmphist= new AttributedString("T1");
		tmphist.addAttribute(TextAttribute.SUPERSCRIPT, -1, 1, 2);
		offGraphics_bn.drawString(tmphist.getIterator(), 140, 370);
		tmphist= new AttributedString("T2");
		tmphist.addAttribute(TextAttribute.SUPERSCRIPT, -1, 1, 2);
		offGraphics_bn.drawString(tmphist.getIterator(), 170, 370);
				
		offGraphics_bn.drawLine(70, 360, 70, 480);
		offGraphics_bn.drawLine(100, 360, 100, 480);
		offGraphics_bn.drawLine(130, 360, 130, 480);
		offGraphics_bn.drawLine(160, 360, 160, 480);
		offGraphics_bn.drawLine(190, 360, 190, 480);
		
		int ctr = 0;
		for(int j=maxit;j>=0;j--){ 
			offGraphics_bn.drawString(Integer.toString(iteration-j+1),25,400+ctr*20);
			for(int k=0;k<4;k++)
				offGraphics_bn.drawString(Integer.toString(statemat[iteration-j][k]), 81+k*30, 400+ctr*20);		
			ctr++;
		}
		
		
	}
	
	public void mypaint_model(int iteration){	
		//Knoten A		
		if(state[0] == 1){
			offGraphics_bn.setColor(Color.red);
		}
		else{
			offGraphics_bn.setColor(Color.blue);
		}
		offGraphics_bn.fillOval(bn_mid-20, 10, 40, 40);
		offGraphics_bn.setColor(Color.black);
		offGraphics_bn.drawString("P(A=1)=0.25", bn_mid+40, 35);
		tmphist= new AttributedString("A");
		tmphist.addAttribute(TextAttribute.SIZE, new Float(15));
		offGraphics_bn.drawString(tmphist.getIterator(), bn_mid-4, 35);
		//Knoten L
		if(state[1] == 1){
			offGraphics_bn.setColor(Color.red);
		}
		else{
			offGraphics_bn.setColor(Color.blue);
		}
		offGraphics_bn.fillOval(bn_mid-20, 120, 40, 40);
		offGraphics_bn.setColor(Color.black);
		offGraphics_bn.drawString("A     P(L=1)", bn_mid+40, 118);
		offGraphics_bn.drawLine(bn_mid+38, 125, bn_mid+110, 125);
		offGraphics_bn.drawLine(bn_mid+55, 107, bn_mid+55, 158);
		offGraphics_bn.drawString("0     0.01", bn_mid+40, 140);
		offGraphics_bn.drawString("1     0.0001", bn_mid+40, 155);
		tmphist= new AttributedString("L");
		tmphist.addAttribute(TextAttribute.SIZE, new Float(15));
		offGraphics_bn.drawString(tmphist.getIterator(), bn_mid-4, 145);
		//Pfeil A->L
		offGraphics_bn.drawLine(bn_mid,50,bn_mid,120);
		offGraphics_bn.drawLine(bn_mid-5,110,bn_mid,120);
		offGraphics_bn.drawLine(bn_mid+5,110,bn_mid,120);
		//Knoten T1
		if(state[2] == 1){
			offGraphics_bn.setColor(Color.red);
		}
		else{
			offGraphics_bn.setColor(Color.blue);
		}
		offGraphics_bn.fillOval(100-20, 230, 40, 40);
		offGraphics_bn.setColor(Color.black);
		tmphist= new AttributedString("L     P(T1=1)");
		tmphist.addAttribute(TextAttribute.SUPERSCRIPT, -1, 9,10);
		offGraphics_bn.drawString(tmphist.getIterator(), 5, 200);
				
		offGraphics_bn.drawLine(3, 207, 78, 207);
		offGraphics_bn.drawLine(20, 189, 20, 242);
		offGraphics_bn.drawString("0     0.9", 5, 224);
		offGraphics_bn.drawString("1     0.01", 5, 239);
		tmphist= new AttributedString("T"+1);
		tmphist.addAttribute(TextAttribute.SUPERSCRIPT, -1, 1,2);	
		tmphist.addAttribute(TextAttribute.SIZE, new Float(15));
		offGraphics_bn.drawString(tmphist.getIterator(), 100-4, 255);
		//Pfeil L->T1
		offGraphics_bn.drawLine(bn_mid,160,100,230);
		offGraphics_bn.drawLine(100-3,220,100,230);
		offGraphics_bn.drawLine(100,230,100+10,227);
		//Knoten T2
		if(state[3] == 1){
			offGraphics_bn.setColor(Color.red);
		}
		else{
			offGraphics_bn.setColor(Color.blue);
		}
		offGraphics_bn.fillOval(200-20, 230, 40, 40);
		offGraphics_bn.setColor(Color.black);
		
		tmphist= new AttributedString("L     P(T2=1)");
		tmphist.addAttribute(TextAttribute.SUPERSCRIPT, -1, 9,10);
		offGraphics_bn.drawString(tmphist.getIterator(), 245, 200);
		
		offGraphics_bn.drawLine(243, 207, 318, 207);
		offGraphics_bn.drawLine(260, 189, 260, 242);
		offGraphics_bn.drawString("0     0.95", 245, 224);
		offGraphics_bn.drawString("1     0.05", 245, 239);
		tmphist= new AttributedString("T"+2);
		tmphist.addAttribute(TextAttribute.SUPERSCRIPT, -1, 1,2);
		tmphist.addAttribute(TextAttribute.SIZE, new Float(15));
		offGraphics_bn.drawString(tmphist.getIterator(), 200-4, 255);
		//Pfeil L->T2
		offGraphics_bn.drawLine(bn_mid,160,200,230);
		offGraphics_bn.drawLine(200+3,220,200,230);
		offGraphics_bn.drawLine(200-10,227,200,230);
		
		
	}
	
	public void mypaint_graph(int iteration){		
		
				
		offGraphics_l.setColor(Color.white);
		offGraphics_l.fillRect(startx, starty, dim.width, dim.height);
		offGraphics_l.setColor(Color.black);
		
		offGraphics_t2.setColor(Color.white);
		offGraphics_t2.fillRect(startx, starty, dim.width, dim.height);
		offGraphics_t2.setColor(Color.black);
		
		tmphist= new AttributedString("Estimator of P(L=1|A=1,T1=1)");
		tmphist.addAttribute(TextAttribute.SUPERSCRIPT, -1, 24,25);	
		offGraphics_l.drawString(tmphist.getIterator(), 205, 10);
		
		tmphist= new AttributedString("Estimator of P(T2=1|A=1,T1=1)");
		tmphist.addAttribute(TextAttribute.SUPERSCRIPT, -1, 16,17);	
		tmphist.addAttribute(TextAttribute.SUPERSCRIPT, -1, 25,26);	
		offGraphics_t2.drawString(tmphist.getIterator(), 205, 10);
		
		//Y-Achse
		offGraphics_l.drawLine(30,dim.height-30, 30, 5);
		//Pfeil Y-Achse
		offGraphics_l.drawLine(25, 10, 30, 0);
		offGraphics_l.drawLine(35, 10, 30,0);
		
		offGraphics_t2.drawLine(30,dim.height-30, 30, 5);
		offGraphics_t2.drawLine(25, 10, 30, 0);
		offGraphics_t2.drawLine(35, 10, 30,0);
		
		//Einteilung y-Achse
		for (int i=dim.height-30; i>=10; i-=20){
		 	offGraphics_l.drawLine(26,i,34,i);
		 	offGraphics_t2.drawLine(26,i,34,i);
		}
		//Beschriftung Y-Achse
		offGraphics_l.drawString("0.2",5,dim.height-65);
		offGraphics_l.drawString("0.4",5,dim.height-105);
		offGraphics_l.drawString("0.6",5,dim.height-145);
		offGraphics_l.drawString("0.8",5,dim.height-185);
		offGraphics_l.drawString("1.0",5,dim.height-225);
			 	
		offGraphics_t2.drawString("0.2",5,dim.height-65);
		offGraphics_t2.drawString("0.4",5,dim.height-105);
		offGraphics_t2.drawString("0.6",5,dim.height-145);
		offGraphics_t2.drawString("0.8",5,dim.height-185);
		offGraphics_t2.drawString("1.0",5,dim.height-225);
		
		//X-Achse
		offGraphics_l.drawLine(30,dim.height-30, dim.width-5, dim.height-30);
		//Pfeil X-Achse
		offGraphics_l.drawLine(dim.width-10,dim.height-35, dim.width, dim.height-30);
		offGraphics_l.drawLine(dim.width-10, dim.height-25, dim.width, dim.height-30);
		
		offGraphics_t2.drawLine(30,dim.height-30, dim.width-5, dim.height-30);
		offGraphics_t2.drawLine(dim.width-10,dim.height-35, dim.width, dim.height-30);
		offGraphics_t2.drawLine(dim.width-10, dim.height-25, dim.width, dim.height-30);
		
		
		//Einteilung X-Achse
		for (int i=30; i<=dim.width-10; i+=10){
		 	offGraphics_l.drawLine(i, dim.height-26,i,dim.height-34);
		 	offGraphics_t2.drawLine(i, dim.height-26,i,dim.height-34);
		}
		if(iteration<25){
			//Beschriftung X-Achse
			offGraphics_l.drawString(Integer.toString(5),78,dim.height-10);
			offGraphics_t2.drawString(Integer.toString(5),78,dim.height-10);
			for (int i=130; i<=dim.width-15; i+=50){
				offGraphics_l.drawString(Integer.toString((i-30)/10),i-5,dim.height-10);	
				offGraphics_t2.drawString(Integer.toString((i-30)/10),i-5,dim.height-10);
			}
			//draw points			
			offGraphics_l.fillOval(37, (int)(dim.height-34-lratio[0]*200), 8, 8);
			offGraphics_t2.fillOval(37, (int)(dim.height-34-t2ratio[0]*200), 8, 8);
							
			for(int i=1;i<=iteration;i++){				
				offGraphics_l.fillOval(37+i*10, (int)(dim.height-34-lratio[i]*200), 8, 8);
				offGraphics_t2.fillOval(37+i*10, (int)(dim.height-34-t2ratio[i]*200), 8, 8);
			}
			
			offGraphics_l.setColor(Color.blue);
			offGraphics_l.drawLine(30,(int)(dim.height-30-l_true*200),dim.width-5,(int)(dim.height-30-l_true*200));
			offGraphics_l.setColor(Color.black);
			offGraphics_t2.setColor(Color.red);
			offGraphics_t2.drawLine(30,(int)(dim.height-30-l_true*200),dim.width-5,(int)(dim.height-30-t2_true*200));
			offGraphics_t2.setColor(Color.black);
			
		}	
		else{
			//Beschriftung X-Achse
			offGraphics_l.drawString(Integer.toString(1000),68,dim.height-10);
			offGraphics_t2.drawString(Integer.toString(1000),68,dim.height-10);
			for (int i=130; i<=dim.width-15; i+=50){
				offGraphics_l.drawString(Integer.toString((i-30)*20),i-12,dim.height-10);	
				offGraphics_t2.drawString(Integer.toString((i-30)*20),i-12,dim.height-10);		
			}
			//draw points	
			last_ly = (int) Math.round(dim.height-30-lratio[0]*200);
			current_ly	= dim.height-30-(int)Math.round(lratio[9]*200000)/1000;		
			offGraphics_l.drawLine(30, last_ly, 31, current_ly);
			current_ly = last_ly;
			last_t2y = (int) Math.round(dim.height-30-t2ratio[0]*200);
			current_t2y	= dim.height-30-(int)Math.round(t2ratio[19]*200000)/1000;		
			offGraphics_t2.drawLine(30, last_t2y, 31, current_t2y);
			current_t2y = last_t2y;
			pointctr = 1;
			for(int i=39;i<=iteration;i+=20){
				current_ly = dim.height-30-(int)Math.round(lratio[i]*200000)/1000;
				offGraphics_l.drawLine(30+pointctr,last_ly,31+pointctr,current_ly);
				last_ly = current_ly;
				current_t2y = dim.height-30-(int)Math.round(t2ratio[i]*200000)/1000;
				offGraphics_t2.drawLine(30+pointctr,last_t2y,31+pointctr,current_t2y);
				last_t2y = current_t2y;
				
				pointctr++;
			}
			offGraphics_l.setColor(Color.blue);
			offGraphics_l.drawLine(30,(int)(dim.height-30-l_true*200),dim.width-5,(int)(dim.height-30-l_true*200));
			offGraphics_l.setColor(Color.black);
			offGraphics_t2.setColor(Color.red);
			offGraphics_t2.drawLine(30,(int)(dim.height-30-l_true*200),dim.width-5,(int)(dim.height-30-t2_true*200));
			offGraphics_t2.setColor(Color.black);
				
		}	
	}
	public void paint_graph(Graphics g, int iteration){
		g.drawImage(lgraph,dim2.width+50,0,this);	
		g.drawImage(t2graph,dim2.width+50,dim.height+20,this);
		mypaint_graph(iteration);
	}
	public void paint_model(Graphics g, int iteration){
		g.drawImage(bnmodel,0,0,this);
		mypaint_model(iteration);
	}
	
	
}