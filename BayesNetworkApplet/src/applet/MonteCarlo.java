//Alex Kunze Susemihl: 2012/01/31
//Program to evaluate marginalization on Bayes' networks
//Network structure is specified by user, conditioning variables are given also
//Gibbs sampling is then used to obtain the marginal over the non-conditioned variables

package applet;


import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import java.awt.*;
import java.util.*;
import java.applet.*;


public class MonteCarlo extends Applet {

	private static final long serialVersionUID = 1L;		


	int width, height;

	static Graph<String, DefaultEdge> myGraph;
	
	Hashtable<String, Integer> state;
	Hashtable<String, Integer> counts;
	Hashtable<String, Double> biases;
	HashSet<String> clamped;
	Hashtable<DefaultEdge, Double> interactions;
	
	Graphics mygraphics;
	Button startsample = new Button("Start Gibbs sampler");
	Button startbp = new Button("Start loopy Belief Propagation");
	Button demo = new Button("Demo Graph");
	TextField maximum = new TextField();
	TextField edges_text = new TextField();
	TextField vertices_text = new TextField();
	TextField clamped_text = new TextField();
	Label clamped_label = new Label();
	Label edges_label = new Label();
	Label vertices_label = new Label();
	Label max_text = new Label();
	Label iteration = new Label();
	Label status = new Label();
	Label summary = new Label();
	Label error_txt = new Label();
	
	
	public void init() {
	
		setLayout(null);
		
		width = getSize().width;
		height = getSize().height;
		maximum.setBounds(300,10,450,20);
		max_text.setBounds(300,50,450,30);
		vertices_text.setBounds(300,100,450,30);
		vertices_label.setBounds(300,130,450,60);
		edges_text.setBounds(300, 200, 450, 30);
		edges_label.setBounds(300, 230, 450, 60);
		startsample.setBounds(10, 10, 220, 30);
		startbp.setBounds(10,50,220,30);
		demo.setBounds(10,90,220,30);
		iteration.setBounds(10,140,300,100);
		status.setBounds(10,200,300,100);
		summary.setBounds(10,340,300,100);
		clamped_text.setBounds(300,300,450,30);
		clamped_label.setBounds(300,330,450,60);
		error_txt.setBounds(300,400,450,60);
		add(startsample);
		add(startbp);
		add(demo);
		add(iteration);
		add(status);
		add(summary);
		add(maximum);
		add(max_text);
		add(edges_label);
		add(edges_text);
		add(vertices_label);
		add(vertices_text);
		add(clamped_text);
		add(clamped_label);
		add(error_txt);
		maximum.setText("10000");
		max_text.setText("Enter the number of iterations here.");
		vertices_text.setText("v1:0.5,v2:0.5,v3:0.5");
		vertices_label.setText("Enter the vertices and biases of the graph here in the following way:\nname:1.0,name2:3.0");
		edges_text.setText("v1-v2:0.1,v2-v3:0.1");
		edges_label.setText("Enter the edges and weights of the graph here in the following way:\nname1-name2:-2.0,name2-name3:2.0");
		clamped_text.setText("v1:1");
		clamped_label.setText("Enter the vertices to be conditioned on and the values as follows:\nname1:0,name2:1");
		mygraphics = this.getGraphics();
		paint(mygraphics);
		
	}
	
	public boolean action(Event event, Object arg){
		if(event.target==demo){
			vertices_text.setText("v1:0.5,v2:0.5,v3:0.5");
			edges_text.setText("v1-v2:0.1,v2-v3:0.1");
			clamped_text.setText("v1:1");
		}
		
		if(event.target==startsample){
			int max_iterations;
			GibbsSampler gibbsSampler = new GibbsSampler(vertices_text.getText(),edges_text.getText(),clamped_text.getText(),summary,error_txt,status,iteration);
			try{
				double d = Double.parseDouble(maximum.getText());
				max_iterations = (int)d;
			}
			catch(NumberFormatException	x){
				max_iterations = 10000;
			}
			max_text.setText("Running Gibbs Sampler for "+max_iterations+" iterations");
			gibbsSampler.run(max_iterations);
		}
		if(event.target==startbp){
			int max_iterations;
			LoopyBP loopyBP = new LoopyBP(vertices_text.getText(),edges_text.getText(),clamped_text.getText(),summary,error_txt,status,iteration);
			try{
				double d = Double.parseDouble(maximum.getText());
				max_iterations = (int)d;
			}
			catch(NumberFormatException	x){
				max_iterations = 10000;
			}
			max_text.setText("Running Loopy Belief Propagation for "+max_iterations+" iterations");
			loopyBP.run(max_iterations);
		}
		
		return false;
	}	

	


	public void paint( Graphics g ) {
//		g.setColor( Color.white );
//		Iterator<String> iter =
//				new DepthFirstIterator<String, DefaultEdge>(myGraph);
//		String vertex;
//		String out = "";
//		while (iter.hasNext()) {
//			vertex = iter.next();
//			out = out+"Vertex " + vertex.toString() + " is connected to: "
//					+ myGraph.edgesOf(vertex).toString()+"\n";
//		}
//		status.setText(out);
	}
}
