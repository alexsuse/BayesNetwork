package applet;

import java.awt.Label;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.alg.BronKerboschCliqueFinder;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;


public class LoopyBP {
	//int numberOfStates=2;
	
	private Graph<String,DefaultEdge> bayesGraph;
	private DirectedGraph<String,DefaultEdge> factorGraph;	
	private HashSet<String> factor_nodes = new HashSet<String>();
	private HashSet<String> variable_nodes = new HashSet<String>();
	private Hashtable<DefaultEdge,Double> plus_messages = new Hashtable<DefaultEdge,Double>();
	private Hashtable<DefaultEdge,Double> minus_messages = new Hashtable<DefaultEdge,Double>();
	private Hashtable<DefaultEdge,Double> plus_oldmessages = new Hashtable<DefaultEdge,Double>();
	private Hashtable<DefaultEdge,Double> minus_oldmessages = new Hashtable<DefaultEdge,Double>();
	private Hashtable<String,Integer> state;
	private Hashtable<String,Double> biases;
	private Hashtable<DefaultEdge,Double> interactions;
	private HashSet<String> clamped;
	private Hashtable<String,Double> plus_beliefs;
	private Hashtable<String,Double> minus_beliefs;
	
	Label summary;
	Label error;
	Label status;
	Label iteration;
	
	public LoopyBP(String vert_txt,String edge_txt, String clamp_txt,Label sum, Label err, Label stat,Label it){
		//Build bayes network and factor graph
		summary = sum;
		error = err;
		status = stat;
		iteration = it;
		try{
			getEdgesAndVertices(vert_txt,edge_txt,clamp_txt);
		}
		catch(Exception e){
			error.setText(e.getMessage()+"\nPlease review your results, as they might be unreliable.");
		}
		factorGraph = getFactorGraph(bayesGraph);
		//Initialize messages
		for(DefaultEdge thisedge  : factorGraph.edgeSet() ){
			plus_messages.put(thisedge, 1.0);
			minus_messages.put(thisedge, 1.0);
			plus_oldmessages.put(thisedge, 1.0);
			minus_oldmessages.put(thisedge, 1.0);
		}

	}

	public LoopyBP(ArrayList<String> vertex_list,ArrayList<String> edge_list, ArrayList<String> clamp_list,Label sum, 
			Label err, Label stat,Label it){
		//Build bayes network and factor graph
		summary = sum;
		error = err;
		status = stat;
		iteration = it;
		try{
			getEdgesAndVertices(vertex_list,edge_list,clamp_list);
		}
		catch(Exception e){
			error.setText(e.getMessage()+"\nPlease review your results, as they might be unreliable.");
		}
		factorGraph = getFactorGraph(bayesGraph);
		//Initialize messages
		for(DefaultEdge thisedge  : factorGraph.edgeSet() ){
			plus_messages.put(thisedge, 1.0);
			minus_messages.put(thisedge, 1.0);
			plus_oldmessages.put(thisedge, 1.0);
			minus_oldmessages.put(thisedge, 1.0);
		}

	}

	private DirectedGraph<String,DefaultEdge> getFactorGraph(Graph<String,DefaultEdge> graph){
		BronKerboschCliqueFinder<String,DefaultEdge> cliquefinder = new BronKerboschCliqueFinder<String,DefaultEdge>(graph);
		DirectedGraph<String,DefaultEdge> factor_graph = new DefaultDirectedGraph<String,DefaultEdge>(DefaultEdge.class);
	
		Collection<Set<String>> cliques = cliquefinder.getAllMaximalCliques();
		
		for(Iterator<Set<String>>it = cliques.iterator();it.hasNext();){
			Set<String> thisclique = it.next();
			String factor_node_name ="factor";
			for(Iterator<String> itintern = thisclique.iterator();itintern.hasNext();){
				factor_node_name = factor_node_name+'_'+itintern.next();
			}
			factor_nodes.add(factor_node_name);
			factor_graph.addVertex(factor_node_name);
			for(Iterator<String> itintern = thisclique.iterator();itintern.hasNext();){
				String thisvertex = itintern.next();
				variable_nodes.add(thisvertex);
				factor_graph.addVertex(thisvertex);
				factor_graph.addEdge(factor_node_name,thisvertex);
				factor_graph.addEdge(thisvertex,factor_node_name);
			}
			
		}
		
		for(String thisvertex : graph.vertexSet()){
			factor_graph.addVertex(thisvertex);
			variable_nodes.add(thisvertex);
			if(biases.get(thisvertex)!=0.0 && !clamped.contains(thisvertex)){
				String factor_node_name = "factor_"+thisvertex;
				factor_nodes.add(factor_node_name);
				factor_graph.addVertex(factor_node_name);
				factor_graph.addEdge(thisvertex,factor_node_name);
				factor_graph.addEdge(factor_node_name,thisvertex);
			}
		}
		return factor_graph;
	}
	
	private double updateVariableMessage(String from_vertex, String to_vertex, double d){
		Hashtable<DefaultEdge,Double> messages;
		double message = 1.0;
		//message from variable node to factor node
		//message from variable node to factor node, message is multiplication of all other incoming messages
		//get all neighbors of origin vertex
		//Because factorGraph is a directedgraph all neighbors are returned twice (once for each edge they participate
		//incoming and outcoming). So we convert the neighbor list to a hashset and back to a list to remove duplicates.
		Set<DefaultEdge> in_edges = factorGraph.incomingEdgesOf(from_vertex);//Graphs.neighborListOf(factorGraph,from_vertex);
		DefaultEdge ignored_edge = factorGraph.getEdge(to_vertex,from_vertex);
		
		if(clamped.contains(from_vertex)){
			return 1.0;
		}
		
		if(d==1.0){
			//if value of variable is 1, take from messages for positive variable, plus_messages
			messages = plus_oldmessages;
		}
		else{
			//otherwise from minus_messages
			messages = minus_oldmessages;
		}
		for(DefaultEdge thisedge : in_edges){
			if(thisedge==ignored_edge){
				continue;
			}
			message*=messages.get(thisedge);
		}
		return message;
	}
	
	private double updateFactorMessage(String from_vertex, String to_vertex, double d){
		double message = 1.0;
		Set<DefaultEdge> in_edges = factorGraph.incomingEdgesOf(from_vertex);//Graphs.neighborListOf(factorGraph,from_vertex);
		DefaultEdge ignored_edge = factorGraph.getEdge(to_vertex,from_vertex);
		ArrayList<DefaultEdge> clamped_edges = new ArrayList<DefaultEdge>();; 
		
		for(DefaultEdge edge : in_edges){
			if(edge==ignored_edge){
				continue;
			}
			if(clamped.contains(factorGraph.getEdgeSource(edge))){
				clamped_edges.add(edge);
			}
		}
		
		if(in_edges.size()==1){
			//message from leaf node, thisvertex is factor_nodename, message is exp(-bias(thisneighbor)*d)
			return Math.exp(biases.get(to_vertex)*d);
		}
		else{
			//message from factor node to variable node
			message=0.0;
			int size = in_edges.size()-1-clamped_edges.size();
			if(size==0){
				double exponent = 0.0;
				for(DefaultEdge thisedge : clamped_edges){
					String node = factorGraph.getEdgeSource(thisedge);
					exponent += interactions.get(bayesGraph.getEdge(to_vertex,node))*state.get(node)*d;
				}
				return Math.exp(exponent);
			}
			
			int limit = (int) Math.pow(2.0, (double) size);
			double product,exponent;
			int[] neighbor_values = new int[size];
			for(int i = 0;i<limit;i++){
				int k = i;
				for (int j = 0; j < size; j++) {
					neighbor_values[j] = -1+2*(k%2);
					k = k/2;
				}
				product = 1.0;
				int j=0;
				for (DefaultEdge thisedge : in_edges){
					if(thisedge==ignored_edge){
						continue;
					}
					if(clamped_edges.contains(thisedge)){
						continue;
					}
					if(neighbor_values[j++]==1){
						product *= plus_oldmessages.get(thisedge);
					}
					else{
						product *= minus_oldmessages.get(thisedge);
					}
				}
				exponent = 0.0;
				j=0;
				for(DefaultEdge thisedge : in_edges){
					if(thisedge ==ignored_edge){
						continue;
					}
					if(clamped_edges.contains(thisedge)){
						exponent+= interactions.get(bayesGraph.getEdge(to_vertex,factorGraph.getEdgeSource(thisedge)))*d*state.get(factorGraph.getEdgeSource(thisedge));
						continue;
					}
					exponent += interactions.get(bayesGraph.getEdge(to_vertex,factorGraph.getEdgeSource(thisedge)))*d*neighbor_values[j++];
				}
				message += Math.exp(exponent)*product;
			}
			return message;
		}
	}
	
	public void run(int max_iterations){
		
		double plus_mes,minus_mes,normalizer;
		
		plus_beliefs = new Hashtable<String,Double>();
		minus_beliefs = new Hashtable<String,Double>();
		
		for(int iterations = 0; iterations< max_iterations; iterations++){
			//update messages
			for(String fro : factor_nodes){
				for(DefaultEdge thisedge : factorGraph.outgoingEdgesOf(fro)){
					plus_mes = updateFactorMessage(fro, factorGraph.getEdgeTarget(thisedge), 1.0);
					minus_mes = updateFactorMessage(fro, factorGraph.getEdgeTarget(thisedge), -1.0);
					plus_messages.put(thisedge, plus_mes);
					minus_messages.put(thisedge, minus_mes);
				}
			}
			for(String fro : variable_nodes){
				for(DefaultEdge thisedge : factorGraph.outgoingEdgesOf(fro)){
					plus_mes = updateVariableMessage(fro, factorGraph.getEdgeTarget(thisedge), 1.0);
					minus_mes = updateVariableMessage(fro, factorGraph.getEdgeTarget(thisedge), -1.0);
					plus_messages.put(thisedge, plus_mes);
					minus_messages.put(thisedge, minus_mes);
				}
			}

			for(String i : variable_nodes){
				for(DefaultEdge s : factorGraph.outgoingEdgesOf(i)){
					plus_mes = plus_messages.get(s);
					minus_mes = minus_messages.get(s);
					normalizer = plus_mes+minus_mes;
					plus_mes = plus_mes/normalizer;
					minus_mes = minus_mes/normalizer;
					plus_messages.put(s, plus_mes);
					minus_messages.put(s, minus_mes);					
				}
			}
			double change = 0.0;
			for(Enumeration<DefaultEdge> edges = plus_messages.keys(); edges.hasMoreElements();){
				DefaultEdge thisedge= edges.nextElement();
				change += Math.abs(plus_messages.get(thisedge)-plus_oldmessages.get(thisedge));
				change += Math.abs(minus_messages.get(thisedge)-minus_oldmessages.get(thisedge));
			}
			System.out.println(iterations+" "+change);
			if(change == 0.0){
				iteration.setText("Converged after "+(iterations+1)+" iterations!");
				break;
			}
			plus_oldmessages = plus_messages;
			minus_oldmessages = minus_messages;

		}

		for(Object i : bayesGraph.vertexSet()){
			getBelief((String)i);
		}
		String summ = "";
		String cond = "";
		for(Iterator<String> i = clamped.iterator();i.hasNext();){
			String key = i.next();
			int value = state.get(key);
			cond=cond+key+'='+value;
			if(i.hasNext()) cond = cond+",";
		}
		for(Iterator<String> i = bayesGraph.vertexSet().iterator();i.hasNext();){
			String key = i.next();
			if(clamped.contains(key)) continue;
			Double value = plus_beliefs.get(key);
			Double ratio = (double) Math.round(5000.0*value)/5000.0;
			summ=summ+"P("+key+" = 1|"+cond+") = "+ratio+"\n";
		}
		summary.setText(summ);
		
	}
	
	public Hashtable<String,Double> getPlusBeliefs(){
		return plus_beliefs;
	}
	
	public void getBelief(String vertex){
		double plus_belief=1.0;
		double minus_belief=1.0;
		double normalizer;
		Set<DefaultEdge> neighbors = factorGraph.incomingEdgesOf(vertex);//Graphs.neighborListOf(factorGraph,(String) vertex);
		//HashSet<String> hs = new HashSet<String>(neighbors);
		//neighbors = new ArrayList<String>(hs);
		for(Object neigh : neighbors){
			plus_belief*=plus_messages.get((DefaultEdge)neigh);//(String)neigh, vertex));
			minus_belief*=minus_messages.get((DefaultEdge)neigh);//(String)neigh, vertex));
		}
		normalizer = plus_belief+minus_belief;
		plus_belief = plus_belief/normalizer;
		minus_belief = minus_belief/normalizer;
		plus_beliefs.put(vertex, plus_belief);
		minus_beliefs.put(vertex, minus_belief);
	}
	
	private void getEdgesAndVertices(String vert_text,String edge_text,String clamped_text) throws Exception{
		//This function reads the applet input and creates the graph structures
		//It takes input from the textfields  edges_text, vertices_text and clamped_text
		//It also creates and sets the values of all interactions and biases
		//Some auxiliary variables
		String[] edge_txt, vert_txt,clamped_txt;
		HashSet<String> vertices;
		HashSet<String> edges;
		HashSet<String> clamped_parse;	

		error.setText("");
		
		//creates the bayes network
		bayesGraph = new SimpleGraph<String, DefaultEdge>(DefaultEdge.class);		
		
		//creates all the hashtables storing the states, counts and parameters of the system
		state = new Hashtable<String, Integer>();
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
			bayesGraph.addVertex(vparse[0]);
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
			if(!bayesGraph.containsVertex(edge_parse2[0])||!bayesGraph.containsVertex(edge_parse2[1])){
				throw new Exception("Specified vertex names weren't provided in the vertex list!");
			}
			bayesGraph.addEdge(edge_parse2[0], edge_parse2[1]);
			DefaultEdge e = bayesGraph.getEdge(edge_parse2[0], edge_parse2[1]);
			interactions.put(e,Double.parseDouble(edge_parse1[1]));
		}
		
		//Checks the clamped vertex list, and stores the clamped values for them.
		if(clamped_text.length()>=1&&clamped_txt.length>=1){
			clamped_parse = new HashSet<String>(Arrays.asList(clamped_txt));
			for(Iterator<String> i = clamped_parse.iterator();i.hasNext();){
				String cl = i.next();
				String[] cls = cl.split(":");
				if(bayesGraph.containsVertex(cls[0])){
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

	private void getEdgesAndVertices(ArrayList<String> vert_list,ArrayList<String> edge_list,ArrayList<String> clamped_list) throws Exception{
		//This function reads the applet input and creates the graph structures
		//It takes input from the textfields  edges_text, vertices_text and clamped_text
		//It also creates and sets the values of all interactions and biases

		error.setText("");
		
		//creates the bayes network
		bayesGraph = new SimpleGraph<String, DefaultEdge>(DefaultEdge.class);		
		
		//creates all the hashtables storing the states, counts and parameters of the system
		state = new Hashtable<String, Integer>();
		biases = new Hashtable<String, Double>();
		clamped = new HashSet<String>();
		interactions = new Hashtable<DefaultEdge, Double>();
		
	
		//check if something was input
		if(vert_list.size()<1) throw new Exception("No input in vertex list");
		if(edge_list.size()<1) throw new Exception("No input in edge list");
		
		//temporary vertices and edges hashsets for iterating
			
		//loops over vertex entries, checks the parsing and adds them to myGraph
		//also adds an entry to state, count and biases with the proper values
		for(Iterator<String> i = vert_list.iterator(); i.hasNext();){
			String v = i.next();
			String[] vparse = v.split(":");
			if(vparse.length!=2){
				throw new Exception("Vertex list not properly formatted!");
			}
			bayesGraph.addVertex(vparse[0]);
			biases.put(vparse[0], Double.parseDouble(vparse[1]));
		}
		System.out.println("vertices done");
		//Same as above for the edgelist. A little more elaborate, because edges are a bit more elaborate
		for(Iterator<String> i = edge_list.iterator();i.hasNext();){
			String new_edge = i.next();
			String[] edge_parse1 = new_edge.split(":");
			if(edge_parse1.length!=2){
				throw new Exception("Edge list not properly formatted! Use : to separate vertices and interactions.");
			}			
			String[] edge_parse2 = edge_parse1[0].split("-");
			if(edge_parse2.length!=2){
				throw new Exception("Edge list not properly formatted! Use - to separate vertices.");
			}
			if(!bayesGraph.containsVertex(edge_parse2[0])||!bayesGraph.containsVertex(edge_parse2[1])){
				throw new Exception("Specified vertex names weren't provided in the vertex list!");
			}
			bayesGraph.addEdge(edge_parse2[0], edge_parse2[1]);
			DefaultEdge e = bayesGraph.getEdge(edge_parse2[0], edge_parse2[1]);
			interactions.put(e,Double.parseDouble(edge_parse1[1]));
		}
		System.out.println("Edges done");
		//Checks the clamped vertex list, and stores the clamped values for them.
		if(clamped_list.size()>=1){
			for(Iterator<String> i = clamped_list.iterator();i.hasNext();){
				String cl = i.next();
				String[] cls = cl.split(":");
				if(cls.length!=2)
					throw new Exception("Clamped vertex list not properly formated");
				if(bayesGraph.containsVertex(cls[0])){
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
