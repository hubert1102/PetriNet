package petrinet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;


public class Transition<T> {
	private Map<T, Integer> outputArcs;
	private Map<T, Integer> inputArcs;
	private ArrayList<T> resetArcs;
	private ArrayList<T> inhibitorArcs;
	
    public Transition(Map<T, Integer> input, Collection<T> reset, Collection<T> inhibitor, Map<T, Integer> output) {
        	inputArcs = input;        	
        	outputArcs = output;
        	resetArcs = new ArrayList<T>();
        	inhibitorArcs = new ArrayList<T>();
        	
        	if (reset != null)
        		resetArcs.addAll(reset);
        	
        	if (inhibitor != null)
        		inhibitorArcs.addAll(inhibitor);
    }

	public Map<T, Integer> getOutputArcs() {
		return outputArcs;
	}

	public Map<T, Integer> getInputArcs() {
		return inputArcs;
	}

	public ArrayList<T> getResetArcs() {
		return resetArcs;
	}

	public ArrayList<T> getInhibitorArcs() {
		return inhibitorArcs;
	}
}