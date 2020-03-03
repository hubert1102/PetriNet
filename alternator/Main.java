package alternator;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import petrinet.PetriNet;
import petrinet.Transition;


public class Main {
	private static final int PRINT_TIME = 30000;
	
	enum place {
		WAIT_A, WAIT_B, WAIT_C, PRINT
	}
	static Set<Transition<place>> t_A, t_B, t_C, release_PRINT;
	
	static PetriNet<place> net;
	
	public static void makeNet() {
		Map<place, Integer> places = new HashMap<place, Integer>();
		places.put(place.WAIT_A, 1);
		
		net = new PetriNet<place>(places, false);
	}
	
	public static void setTransitions() {
		t_A = new HashSet<Transition<place>>();
		t_B = new HashSet<Transition<place>>();
		t_C = new HashSet<Transition<place>>();
		release_PRINT = new HashSet<Transition<place>>();
		
		Map<place, Integer> input =  Collections.singletonMap(place.WAIT_A, 1);
        Collection<place> reset = null;
        Collection<place> inhibitor = Collections.singleton(place.PRINT);
        Map<place, Integer> output = new HashMap<place, Integer>();
        output.put(place.PRINT, 1);
        output.put(place.WAIT_B, 1);
        
        t_A.add(new Transition<place>(input, reset, inhibitor, output));
        
        input = Collections.singletonMap(place.WAIT_B, 1);
        reset = null;
        inhibitor = Collections.singleton(place.PRINT);
        output = new HashMap<place, Integer>();
        output.put(place.PRINT, 1);
        output.put(place.WAIT_C, 1);

        t_B.add(new Transition<place>(input, reset, inhibitor, output));
        
        input = Collections.singletonMap(place.WAIT_C, 1);
        reset = null;
        inhibitor = Collections.singleton(place.PRINT);
        output = new HashMap<place, Integer>();
        output.put(place.PRINT, 1);
        output.put(place.WAIT_A, 1);
        
        t_C.add(new Transition<place>(input, reset, inhibitor, output));
        
        input = Collections.singletonMap(place.PRINT, 1);
        reset = null;
        inhibitor = null;
        output = new HashMap<place, Integer>();
        release_PRINT.add(new Transition<place>(input, reset, inhibitor, output));
	}
	
	private static class Printer implements Runnable {
		Collection<Transition<place>> t;
		
		public Printer(Collection<Transition<place>> t) {
			this.t = t;
		}
		
		@Override
		public void run() {
			while (true) {
				try {
					net.fire(t);
					
					System.out.print(Thread.currentThread().getName());
					System.out.print(".");
					
					net.fire(release_PRINT);
					
				} catch (InterruptedException e) {}
			}
		}
		
	}
	
	private static boolean isSafe(Collection<Transition<place>> transitions) {
		Set<Map<place, Integer>> reachables = net.reachable(transitions);
		
		for (Map<place, Integer> m : reachables) {
			if (m.get(place.PRINT) != null && m.get(place.PRINT) > 1)
				return false;
		}
		
		return true;
	}
	
	
	public Main() {}
	
	public static void main(String[] args) {
		makeNet();
		setTransitions();
		
		Collection<Transition<place>> all_t = new HashSet<Transition<place>>();
		all_t.addAll(t_A);
		all_t.addAll(t_B);
		all_t.addAll(t_C);
		
		if (isSafe(all_t)) {
			System.out.println("Kazdy stan jest bezpieczny");
		}
		else {
			System.out.println("Dwa watki moga jednoczesnie byc w sekcji krytycznej");
			return;
		}

		Thread A = new Thread(new Printer(t_A), "A"),
			   B = new Thread(new Printer(t_B), "B"),
			   C = new Thread(new Printer(t_C), "C");
		
		A.start();
		B.start();
		C.start();
		
		try {
			Thread.sleep(PRINT_TIME);
		} catch (InterruptedException e) {
			Thread t = Thread.currentThread();
			t.interrupt();
			System.out.println("Thread " + Thread.currentThread().getName() + " interrupted");
		}
		
		A.interrupt();
		B.interrupt();
		C.interrupt();
		
		return;
	}


}
