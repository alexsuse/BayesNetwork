package applet;

import java.awt.Label;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;


public class GibbsSampler {
	private Graph<String,DefaultEdge> myGraph;
	
	Hashtable<String, Integer> state = new Hashtable<String,Integer>();
	Hashtable<String, Integer> counts;
	Hashtable<String, Double> frequency= new Hashtable<String,Double>();
	Hashtable<String, Double> biases;
	HashSet<String> clamped;
	Hashtable<DefaultEdge, Double> interactions;
	
	Random rndsampler = new Random();

	
	Label summary;
	Label error_txt;
	Label status;
	Label iteration;
	
	public GibbsSampler(String vert_txt, String edge_txt, String clamped_txt, Label sum, Label err, Label stat, Label it){
		summary = sum;
		error_txt = err;
		status = stat;
		iteration = it;
		try{
			get_edges_and_vertices(vert_txt,edge_txt,clamped_txt);
		}
		catch(Exception e){
			error_txt.setText(e.getMessage()+"\nPlease review before running as results might not be reliable.");
		}
	}	
	
	public GibbsSampler(ArrayList<String> vert_txt, ArrayList<String> edge_txt, ArrayList<String> clamped_txt, Label sum, Label err, Label stat, Label it){
		summary = sum;
		error_txt = err;
		status = stat;
		iteration = it;
		try{
			get_edges_and_vertices(vert_txt,edge_txt,clamped_txt);
		}
		catch(Exception e){
			error_txt.setText(e.getMessage()+"\nPlease review before running as results might not be reliable.");
		}
	}
	
	public void run(int max_iterations){
		ArrayList<String> sampled_vars = new ArrayList<String>(myGraph.vertexSet());
		//Set all counters to zero
		for(Iterator<String> e = sampled_vars.iterator();e.hasNext();){
			String key = e.next();
			counts.put(key, 0);
			frequency.put(key,0.0);
		}
		//Remove clamped variables from sampling
		for(Iterator<String> i = clamped.iterator();i.hasNext();){
			sampled_vars.remove(i.next());
		}
		max_iterations*=sampled_vars.size();
		//Run iteration
	
		
		for(int iterations=0;iterations<max_iterations;iterations++){
			System.out.println("at iteration "+iterations);
			//Select a vertex in the graph randomly
			String v = (String) sampled_vars.toArray()[rndsampler.nextInt(sampled_vars.size())];
			//Sample the value of that variable
			samplestate(v);
			//Print out the state
			status.setText(printstate(state));
			//Update iteration counter
			iteration.setText(iterations/sampled_vars.size()+" MC steps");
		}
		//Print summary
		String summ = "";
		String cond = "";
		for(Iterator<String> i = clamped.iterator();i.hasNext();){
			String key = i.next();
			int value = state.get(key);
			cond=cond+key+'='+value;
			if(i.hasNext()) cond = cond+",";
		}
		for(Iterator<String> i = sampled_vars.iterator();i.hasNext();){
			String key = i.next();
			int value = counts.get(key);
			Double ratio = (double) Math.round(5000.0*(1.0+(1.0*value/max_iterations)))/10000.0;
			frequency.put(key, ratio);
			summ=summ+"P("+key+" = 1|"+cond+") = "+ratio+"\n";
		}
		summary.setText(summ);
	}
	
	public Hashtable<String,Double> getFrequency(){
		return frequency;
	}
	

	private String printstate(Hashtable<String, Integer> state){
		String statestring="";
		
		for(Enumeration<String> e = state.keys();e.hasMoreElements();){
			String key = e.nextElement();
			int value = state.get(key);
			statestring = statestring+"Variable "+key+" has value "+value+"\n";
		}
		
		return statestring;
	}
	
	private double get_cond_probability(String vertex){
		double energy_1=0.0;
		double energy_2=0.0;
		if(myGraph.containsVertex(vertex)){
			Collection<String> neighbors = (Collection<String>) Graphs.neighborListOf(myGraph,vertex);
			for(Iterator<String> i= neighbors.iterator();i.hasNext();){
				String n = i.next();
				double interaction = interactions.get(myGraph.getEdge(vertex,n));
				int neighbor_state = state.get(n);
				energy_1 -= interaction*1.0*neighbor_state;
				energy_2 += interaction*1.0*neighbor_state;
			}
			energy_1 -= biases.get(vertex)*1.0;
			energy_2 += biases.get(vertex)*1.0;
			return Math.exp(-energy_1)/(Math.exp(-energy_2)+Math.exp(-energy_1));
		}
		else{
			System.out.print("Vertex "+vertex+" is not in graph!\n");
			return 0.0;
		}
	}
	
	private void samplestate(String vertex){
		double cond_probability;
		cond_probability = get_cond_probability(vertex);
		
	
		if(drawRndNumber(cond_probability)==1){
			state.put(vertex, 1);
		}
		else{
			state.put(vertex, -1);
		}
		countstate();
	}
	
	private void countstate(){
		for(Enumeration<String> e = counts.keys();e.hasMoreElements();){
			String key= e.nextElement();
			int count = counts.get(key);
			int current = state.get(key);
			counts.put(key,count+current);
		}
		
	}
	
	private void get_edges_and_vertices(String vert_text,String edge_text,String clamped_text) throws Exception{
		//This function reads the applet input and creates the graph structures
		//It takes input from the textfields  edges_text, vertices_text and clamped_text
		//It also creates and sets the values of all interactions and biases
		//Some auxiliary variables
		String[] edge_txt, vert_txt,clamped_txt;
		HashSet<String> vertices;
		HashSet<String> edges;
		HashSet<String> clamped_parse;	

		error_txt.setText("");
		
		//creates the bayes network
		myGraph = new SimpleGraph<String, DefaultEdge>(DefaultEdge.class);		
		
		//creates all the hashtables storing the states, counts and parameters of the system
		state = new Hashtable<String, Integer>();
		counts = new Hashtable<String,Integer>();
		biases = new Hashtable<String, Double>();
		clamped = new HashSet<String>();
		interactions = new Hashtable<DefaultEdge, Double>();
		
		//Reads the input in the textfields, splitting at , as required
		vert_txt = vert_text.split(",");
		edge_txt = edge_text.split(",");
		clamped_txt = clamped_text.split(",");
		
		//check if something was input
		if(vert_txt.length<1) throw new Exception("No input in vertex list");
		if(edge_txt.length<1) throw new Exception("No input in edge list");
		
		//temporary vertices and edges hashsets for iterating
		vertices = new HashSet<String>(Arrays.asList(vert_txt));
		edges = new HashSet<String>(Arrays.asList(edge_txt));
		
		//loops over vertex entries, checks the parsing and adds them to myGraph
		//also adds an entry to state, count and biases with the proper values
		for(Iterator<String> i = vertices.iterator(); i.hasNext();){
			String v = i.next();
			String[] vparse = v.split(":");
			if(vparse.length!=2){
				throw new Exception("Vertex list not properly formatted!");
			}
			myGraph.addVertex(vparse[0]);
			state.put(vparse[0],1-2*drawRndNumber(0.5));
			biases.put(vparse[0], Double.parseDouble(vparse[1]));
		}
		
		//Same as above for the edgelist. A little more elaborate, because edges are a bit more elaborate
		for(Iterator<String> i = edges.iterator();i.hasNext();){
			String new_edge = i.next();
			String[] edge_parse1 = new_edge.split(":");
			if(edge_parse1.length!=2){
				throw new Exception("Edge list not properly formatted! Use : to separate vertices and interactions.");
			}			
			String[] edge_parse2 = edge_parse1[0].split("-");
			if(edge_parse2.length!=2){
				throw new Exception("Edge list not properly formatted! Use - to separate vertices.");
			}
			if(!myGraph.containsVertex(edge_parse2[0])||!myGraph.containsVertex(edge_parse2[1])){
				throw new Exception("Specified vertex names weren't provided in the vertex list!");
			}
			myGraph.addEdge(edge_parse2[0], edge_parse2[1]);
			DefaultEdge e = myGraph.getEdge(edge_parse2[0], edge_parse2[1]);
			interactions.put(e,Double.parseDouble(edge_parse1[1]));
		}
		
		//Checks the clamped vertex list, and stores the clamped values for them.
		if(clamped_text.length()>=1){
			clamped_parse = new HashSet<String>(Arrays.asList(clamped_txt));
			for(Iterator<String> i = clamped_parse.iterator();i.hasNext();){
				String cl = i.next();
				String[] cls = cl.split(":");
				if(myGraph.containsVertex(cls[0])){
					state.put(cls[0], Integer.parseInt(cls[1]));
					clamped.add(cls[0]);
				}
				else{
					throw new Exception("Vertex "+cls[0]+" was not specified on vertex list!");
				}
			}
		}
		else{
			throw new Exception("No clamped vertices specified.");
		}
	}
	private void get_edges_and_vertices(ArrayList<String> vert,ArrayList<String> edge,ArrayList<String> clamp) throws Exception{
		//This function reads the applet input and creates the graph structures
		//It takes input from the textfields  edges_text, vertices_text and clamped_text
		//It also creates and sets the values of all interactions and biases
		//Some auxiliary variables
	
		error_txt.setText("");
		
		//creates the bayes network
		myGraph = new SimpleGraph<String, DefaultEdge>(DefaultEdge.class);		
		
		//creates all the hashtables storing the states, counts and parameters of the system
		state = new Hashtable<String, Integer>();
		counts = new Hashtable<String,Integer>();
		biases = new Hashtable<String, Double>();
		clamped = new HashSet<String>();
		interactions = new Hashtable<DefaultEdge, Double>();

		//check if something was input
		if(vert.size()<1) throw new Exception("No input in vertex list");
		if(edge.size()<1) throw new Exception("No input in edge list");
		
		//loops over vertex entries, checks the parsing and adds them to myGraph
		//also adds an entry to state, count and biases with the proper values
		for(Iterator<String> i = vert.iterator(); i.hasNext();){
			String v = i.next();
			String[] vparse = v.split(":");
			if(vparse.length!=2){
				throw new Exception("Vertex list not properly formatted!");
			}
			myGraph.addVertex(vparse[0]);
			state.put(vparse[0],1-2*drawRndNumber(0.5));
			biases.put(vparse[0], Double.parseDouble(vparse[1]));
		}
		
		//Same as above for the edgelist. A little more elaborate, because edges are a bit more elaborate
		for(Iterator<String> i = edge.iterator();i.hasNext();){
			String new_edge = i.next();
			String[] edge_parse1 = new_edge.split(":");
			if(edge_parse1.length!=2){
				throw new Exception("Edge list not properly formatted! Use : to separate vertices and interactions.");
			}			
			String[] edge_parse2 = edge_parse1[0].split("-");
			if(edge_parse2.length!=2){
				throw new Exception("Edge list not properly formatted! Use - to separate vertices.");
			}
			if(!myGraph.containsVertex(edge_parse2[0])||!myGraph.containsVertex(edge_parse2[1])){
				throw new Exception("Specified vertex names weren't provided in the vertex list!");
			}
			myGraph.addEdge(edge_parse2[0], edge_parse2[1]);
			DefaultEdge e = myGraph.getEdge(edge_parse2[0], edge_parse2[1]);
			interactions.put(e,Double.parseDouble(edge_parse1[1]));
		}
		
		//Checks the clamped vertex list, and stores the clamped values for them.
		if(clamp.size()>=1){
			for(Iterator<String> i = clamp.iterator();i.hasNext();){
				String cl = i.next();
				String[] cls = cl.split(":");
				if(cls.length!=2) throw new Exception("Clamped vertices not properly formatted");
				if(myGraph.containsVertex(cls[0])){
					state.put(cls[0], Integer.parseInt(cls[1]));
					clamped.add(cls[0]);
				}
				else{
					throw new Exception("Vertex "+cls[0]+" was not specified on vertex list!");
				}
			}
		}
		else{
			throw new Exception("No clamped vertices specified.");
		}
	}
	
	public int drawRndNumber(double trueprob){
		Double rtmp = rndsampler.nextDouble();
		if(rtmp < trueprob)
			return (1);
		else
			return (-1);
	}

	private void printGraph(Graph<String,DefaultEdge> graph){
		//prints out all vertices and edges in a given graph along with possible interactions among them
		System.out.print("Vertices:\n");
		for(Iterator<String> i= graph.vertexSet().iterator();i.hasNext();){
			String thisvertex = i.next();
			System.out.print(thisvertex);
			if(biases.containsKey(thisvertex)){
				System.out.print(" : "+biases.get(thisvertex).toString());
			}
			System.out.print("\n");
		}
		System.out.print("Edges:\n");
		for(Iterator<DefaultEdge> i = graph.edgeSet().iterator();i.hasNext();){
			DefaultEdge thisedge = i.next();
			System.out.print(thisedge.toString());
			if(interactions.containsKey(thisedge)){
				System.out.print(" : "+interactions.get(thisedge).toString());
			}
			System.out.print("\n");
		}
		
	}

}
