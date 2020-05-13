package it.polito.tdp.metroparis.model;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.traverse.GraphIterator;

import it.polito.tdp.metroparis.db.MetroDAO;

public class Model {

	// grafo non pesato
	private Graph<Fermata, DefaultEdge> graph;
	private List<Fermata> fermate;
	private Map<Integer, Fermata> fermateIdMap;

	public Model() {
		this.graph = new SimpleDirectedGraph<>(DefaultEdge.class);

		MetroDAO dao = new MetroDAO();

		// 1) CREAZIONE DEI VERTICI
		this.fermate = dao.getAllFermate(); // mi dà una lista che sontiene tutte le fermate

		// creo l'IdMap
		this.fermateIdMap = new HashMap<>();
		for (Fermata f : this.fermate) {
			fermateIdMap.put(f.getIdFermata(), f);
		}

		Graphs.addAllVertices(this.graph, this.fermate);

		// System.out.println(this.graph) ;

		// 2) CREAZIONE DEGLI ARCHI -- metodo 1 (coppie di vertici)
		// Efficienza N^2 = 619*619
		/*
		 * La più facile ma la meno efficente Per ogni coppia di vertici può esserci o
		 * non esserci una connessione Interroghiamo il DAO per saperlo
		 * 
		 * 
		 * for(Fermata fp : this.fermate) { for(Fermata fa : this.fermate) { if(
		 * dao.fermateConnesse(fp, fa) ) { //se esiste una connessione tra fa e fp
		 * this.graph.addEdge(fp, fa) ; } } }
		 */

		// CREAZIONE DEGLI ARCHI -- metodo 2 (da un vertice, trova tutti i connessi)
		/*
		 * COMPLESSITA' 619*2/3/4 dipende dal numero di nodi che trova 619*densità del
		 * grafo(num di archi del grafo/num di archi del grafo completo)
		 * 
		 * //DA PREFERIRE SE LA DENSITA' è BASSA for(Fermata fp: this.fermate) {
		 * List<Fermata> connesse = dao.fermateSuccessive(fp, fermateIdMap) ; //tutte le
		 * fermate adiacenti a fp
		 * 
		 * for(Fermata fa: connesse) { this.graph.addEdge(fp, fa) ; } }
		 */

		// CREAZIONE DEGLI ARCHI -- metodo 3 (chiedo al DB l'elenco degli archi)
		// Implemento questo rispetto al metodo 2 se è quasi immediato ottenere
		// la soluzione dal DB
		// Bisogna valutare l'efficenza del DB nel darmi tutte le risposte
		// Deve essere rapida la ricerca dee informazioni dei database
		List<CoppiaFermate> coppie = dao.coppieFermate(fermateIdMap);
		// Il DAO deve restituire un insieme di coppie di fermate

		for (CoppiaFermate c : coppie) {
			this.graph.addEdge(c.getFp(), c.getFa());
		}

//		System.out.println(this.graph) ;
		System.out.format("Grafo caricato con %d vertici %d archi \n", this.graph.vertexSet().size(),
				this.graph.edgeSet().size());

	}

	/**
	 * Visita l'intero grafo con la strategia Breadth First e ritorna l'insime dei
	 * vertici incontrati secondo l'algritmo di visita
	 * 
	 * @param source Vertice di partenza della vista
	 * @return insieme di vertici incontrati
	 */
	public List<Fermata> VisitaAmpiezza(Fermata source) {
		BreadthFirstIterator<Fermata, DefaultEdge> bfv = new BreadthFirstIterator<>(graph, source);
		// GraphIterator<Fermata, DefaultEdge< btv.... possiamo estrarre l'oggetto
		// Creato l'iteratore si posiziona sul primo elemento
		List<Fermata> visita = new ArrayList<Fermata>();

		while (bfv.hasNext()) {
			visita.add(bfv.next());
		}

		return visita;
	}

	/**
	 * Visita l'intero grafo con la strategia Depth First e ritorna l'insime dei
	 * vertici incontrati secondo l'algritmo di visita
	 * 
	 * @param source
	 * @return
	 */
	public List<Fermata> VisitaProfondita(Fermata source) {
		DepthFirstIterator<Fermata, DefaultEdge> bfv = new DepthFirstIterator<>(graph, source);
		// Creato l'iteratore si posiziona sul primo elemento
		List<Fermata> visita = new ArrayList<Fermata>();

		while (bfv.hasNext()) {
			visita.add(bfv.next());
		}

		return visita;
	}

	public Map<Fermata, Fermata> alberoVisita(Fermata source) {
		Map<Fermata, Fermata> albero = new HashMap<>();
		//devo inserire la sorgente a mano.
		albero.put(source, null); // non ha padre
		
		GraphIterator<Fermata, DefaultEdge> bfv = new BreadthFirstIterator<>(graph, source);
		//definisco una classe anonima
		bfv.addTraversalListener(new TraversalListener<Fermata, DefaultEdge>() {
			
			@Override
			public void vertexTraversed(VertexTraversalEvent<Fermata> e) {	
			}
			
			@Override
			public void vertexFinished(VertexTraversalEvent<Fermata> e) {				
			}
			
			@Override
			public void edgeTraversed(EdgeTraversalEvent<DefaultEdge> e) {
				//la vista sta considerando un nuovo arco.
				//questo arco ha scoperto un nuovo vertice?
				//se sì, da dove?
				DefaultEdge edge = e.getEdge(); //(a,b) ho scoperto 'a' partendo da 'b' o viceversa?
				//Mi faccio dare gli estremi dell'arco
				Fermata a = graph.getEdgeSource(edge);
				Fermata b = graph.getEdgeTarget(edge);
				if(albero.containsKey(a) && !albero.containsKey(b)) {
					// a è già noto, quindi ho scoperto b provenendo da a
					albero.put(b, a);
				}else if(albero.containsKey(b) && !albero.containsKey(a)){
					// b è già noto, quindi ho scoperto a provenendo da b
					albero.put(a,b) ;
				}
			}
			
			@Override
			public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {
			}
			
			@Override
			public void connectedComponentFinished(ConnectedComponentTraversalEvent e) {
			}
		});
		
		while(bfv.hasNext()) {
			bfv.next(); //estrai l'elemento e ignoralo
		}
		
		return albero;
	}
	
	public List<Fermata> camminiMinimi(Fermata partenza, Fermata arrivo) {
		DijkstraShortestPath<Fermata, DefaultEdge> dij = new DijkstraShortestPath<Fermata, DefaultEdge>(graph);
		GraphPath<Fermata, DefaultEdge> cammino = dij.getPath(partenza, arrivo);
		
		return cammino.getVertexList();
	}

	public static void main(String args[]) {
		/*-> creare il modello fa si che venga eseguito il costruttore
		 * e dentro il costruttore stiamo costruendo il grafo
		 */
		Model m = new Model();

		// per testare il modello
		List<Fermata> visita = m.VisitaAmpiezza(m.fermate.get(0));
		//System.out.println(visita);

		List<Fermata> visita1 = m.VisitaProfondita(m.fermate.get(0));
		//System.out.println(visita1);
		
		Map<Fermata, Fermata> albero = m.alberoVisita(m.fermate.get(0));
		//for(Fermata f : albero.keySet())
		//System.out.format("%s -> %s\n",f, albero.get(f) );

		List<Fermata> cammino = m.camminiMinimi(m.fermate.get(0), m.fermate.get(1));
		//restituisce il numero minimo di fermate
		System.out.println(cammino);
		/*
		 * A seconda della necessità può essere meglio rimandare alla costruzione del
		 * grafo successivamente
		 */
	}

}
