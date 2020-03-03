package multiplicator;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import petrinet.PetriNet;
import petrinet.Transition;

public class Main {
	
private static final int THREADS = 4;
	
	enum place {
		A, B, RESULT, CALC, TRANS, STORE
	}
	
	static Transition<place> t1, t2, t3, t4, last;
	
	static Set<Transition<place>> transitions;
	static PetriNet<place> net;
	
	private static void makeNet(int a, int b) {
		net = new PetriNet<place>(Map.of(place.STORE, a, place.B, b, place.TRANS, 1), false);
	}
	
	private static void makeTransitions() {
		t1 = new Transition<place>(Map.of(place.A, 1),
								   Collections.emptySet(),
								   Arrays.asList(place.CALC),
								   Map.of(place.RESULT, 1, place.STORE, 1));
		
		t2 = new Transition<place>(Map.of(place.STORE, 1),
								   Collections.emptySet(),
								   Arrays.asList(place.TRANS),
								   Map.of(place.A, 1));
		
		t3 = new Transition<place>(Map.of(place.B, 1, place.TRANS, 1),
								   Collections.emptySet(),
								   Arrays.asList(place.A),
								   Map.of(place.CALC, 1));
		
		t4 = new Transition<place>(Map.of(place.CALC, 1),
								   Collections.emptySet(),
								   Arrays.asList(place.STORE),
								   Map.of(place.TRANS, 1));
		
		last = new Transition<place>(Collections.emptyMap(),
									 Collections.emptySet(),
									 Arrays.asList(place.B, place.A, place.CALC),
									 Collections.emptyMap());
		
		transitions = new HashSet<Transition<place>>();
		transitions.addAll(Arrays.asList(t1, t2, t3, t4));
	}
	
	static int[] count = new int[THREADS];
	
	private static class Compute implements Runnable {
		int id;
		
		public Compute(int id) {
			this.id = id;
		}
		
		@Override
		public void run() {
			try {
				while (true) {
					count[id]++;
					net.fire(transitions);	
				}

			} catch (InterruptedException e) {
				System.out.println("Watek o id " + id + " wykonal sie " + count[id] + " razy");
			}
			
		}
		
	}
	
	public Main() {}
	
	public static void main(String[] args) {
		Scanner s = new Scanner(System.in);
		
		int a = s.nextInt(),
			b = s.nextInt();
		
		if (a > b) {
			int temp = a;
			a = b;
			b = temp;
		}
		s.close();
		makeNet(a, b);
		makeTransitions();

		Thread[] threads = new Thread[THREADS];
		
		for (Integer i = 0; i < THREADS; i++) {
			threads[i] = new Thread(new Compute(i), i.toString());
			threads[i].start();
		}
			
		try {
			net.fire(Collections.singleton(last));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}


		for (Integer i = 0; i < THREADS; i++) {
			threads[i].interrupt();
		}
		
		System.out.println(net.getPlaceValue(place.RESULT));
	}


}
