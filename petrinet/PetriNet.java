package petrinet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

public class PetriNet<T> {	
	volatile int writes = 0, reads = 0, writesWait = 0, readsWait = 0;
	Semaphore readers = new Semaphore(0);
	Semaphore mutex = new Semaphore(1);
	Semaphore mutexQ = new Semaphore(1);
	Semaphore mutexTest = new Semaphore(1);
	volatile LinkedList<Pair> writersQ = new LinkedList<Pair>();	
	volatile boolean isFair;
	volatile HashMap<T, Integer> places;
	
	public PetriNet(Map<T, Integer> initial, boolean fair) {
       isFair = fair;
       places = new HashMap<T, Integer>(initial);
       
       places.entrySet().removeIf(entry -> entry.getValue().equals(0));
    }
	
	private class Pair {
		Semaphore s;
		Collection<Transition<T>> transitions;
		
		public Pair(Semaphore s, Collection<Transition<T>> transitions) {
			this.s = s;
			this.transitions = transitions;;
		}
		
	}
	
	private void readBegin() {
		try {
			mutex.acquire();

			if (writes + writesWait > 0) {
				readsWait++;
				mutex.release();
				readers.acquire();
				readsWait--;
			}
		reads++;
		
		if (readsWait > 0) {
			readers.release();
		}
		else
			mutex.release();		
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void readEnd() {
		try {
			mutex.acquire();
			reads--;
		
			if (reads == 0 && enabledWriter()) {
				releaseFirst();
			}
			else
				mutex.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	private boolean enabledWriter() {
		try {
			mutexQ.acquire();
			int size = writersQ.size();
		
			for (int i = 0; i < size; i++) {
				Pair x = writersQ.get(i);
				
				if (containsEnabled(x.transitions)) {
					mutexQ.release();
					return true;
				}
						
			}
			mutexQ.release();
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	private void releaseFirst() {			
		try {
			mutexQ.acquire();
			int size = writersQ.size();
			
			for (int i = 0; i < size; i++) {
				Pair x = writersQ.get(i);
				
				if (containsEnabled(x.transitions)) {
					x.s.release();
					writersQ.remove(i);
					mutexQ.release();

					return;
				}		
			}
			if (mutexQ.availablePermits() == 0)
				mutexQ.release();
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}	
	}
	
	
	private HashMap<T, Integer> fireTransition(HashMap<T, Integer> places, Transition<T> t) {
		HashMap<T, Integer> postFire = new HashMap<T, Integer>();
		postFire.putAll(places);
		
		Map<T, Integer> inputs = t.getInputArcs();
		Map<T, Integer> outputs = t.getOutputArcs();
		ArrayList<T> resets = t.getResetArcs();
		
		for (Map.Entry<T, Integer> entry : inputs.entrySet()) {
			postFire.put(entry.getKey(), postFire.get(entry.getKey()) - entry.getValue());
			if (postFire.get(entry.getKey()) == 0)
				postFire.remove(entry.getKey());
		}
		
		for (Map.Entry<T, Integer> entry : outputs.entrySet()) 
			postFire.put(entry.getKey(), 
						(postFire.get(entry.getKey()) != null ? postFire.get(entry.getKey()) : 0) + entry.getValue());
		
		for (T entry : resets)
			postFire.remove(entry);
		
		return postFire;
	}
	
	private void fireThis(Transition<T> t) {
		places = fireTransition(places, t);
	}
	
	private boolean isEnabled(Transition<T> t, Map<T, Integer> places) {
		Map<T, Integer> inputs = t.getInputArcs();
		ArrayList<T> inhibitors = t.getInhibitorArcs();
			
		for (Map.Entry<T, Integer> entry : inputs.entrySet()) {
			if (places.get(entry.getKey()) == null)
				return false;
			if (places.get(entry.getKey()) < entry.getValue())
				return false;
		}
		
		for (T entry : inhibitors)
			if (places.get(entry) != null)
				return false;
		
		return true;
	}
    
    private void findReachable(Set<Map<T, Integer>> found, HashMap<T, Integer> current,  Collection<Transition<T>> transitions) {
    	found.add(current);
    	HashMap<T, Integer> newReached;
				
		for (Transition<T> t : transitions) {
			
			if (isEnabled(t, current)) {
				newReached = fireTransition(current, t);
				
				if (!found.contains(newReached)) {
					found.add(newReached);
					findReachable(found, newReached, transitions);
				}
						
			}
		}	
    }
    
    public Set<Map<T, Integer>> reachable(Collection<Transition<T>> transitions) {   	
    	Set<Map<T, Integer>> reachable = new HashSet<Map<T, Integer>>();

		readBegin();
		
		HashMap<T, Integer> current = new HashMap<T, Integer>(places);

		readEnd();
		
		findReachable(reachable, current, transitions);
    		
    	return reachable;
    }

    public boolean containsEnabled(Collection<Transition<T>> transitions) { 	
    	for (Transition<T> tr : transitions)
    		if (isEnabled(tr, places))
    			return true;
    	
    	return false;
    }
    
    public Transition<T> fire(Collection<Transition<T>> transitions) throws InterruptedException {
       	Semaphore myS;
       	
       	myS = new Semaphore(0);	
    	
		mutex.acquire();
		
		mutexQ.acquire();
		Pair p = new Pair(myS, transitions);
    	writersQ.add(p);
    	mutexQ.release();
    	
    	if (writes + reads > 0 || !containsEnabled(transitions)) {
    		writesWait++;
    		mutex.release();
        	myS.acquire();
    		writesWait--;
    	}
   
    	writes++;
    	mutex.release();    	
    	
    	mutexQ.acquire();
    	writersQ.remove(p);
    	mutexQ.release();
    	
    	for (Transition<T> t : transitions) {    	

    		if (isEnabled(t, places)) {    		
    			try {
	    			fireThis(t);
	    			
	    			return t;
    			} finally {
			    	mutex.acquire();
			    	writes--;
			    		    
			    	if (readsWait > 0)
			        	readers.release();
			    	else if (writesWait > 0 && enabledWriter()) {
			    		releaseFirst();
			    	}	
			    	else
			        	mutex.release();
    			}
    		}
    	}
    		    	
    	return null;
    }

	public Integer getPlaceValue(T res) {
		return places.get(res) == null ? 0 : places.get(res);
	}

}