package org.alliancegenome.core.translators;

import java.util.ArrayList;

public abstract class Translator<I, O> {
	
	public Iterable<O> translate(Iterable<I> inputObjects) {
		ArrayList<O> outputObjects = new ArrayList<O>();
		for (I inputObject: inputObjects) {
			outputObjects.add(translate(inputObject));
		}
		return outputObjects;
	}
	
	public O translate(I inputObject) {
		return translate(inputObject, 1);
	}
	
	public abstract O translate(I inputObject, int depth);

}
